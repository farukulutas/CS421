import socket
import sys
import threading
from urllib.parse import urlsplit
import ssl
import gzip
import io
import time
import os

if len(sys.argv) > 2:
    TIMEOUT = int(sys.argv[2])
else:
    TIMEOUT = 10

BUFFER_SIZE = 8192
MAX_FILE_SIZE = 100 * 1024 * 1024  # 100 MB
CACHE_DIR = "cache"
LOG_FILE = "proxy.log"

if not os.path.exists(CACHE_DIR):
    os.makedirs(CACHE_DIR)

def log_message(message):
    with open(LOG_FILE, "a") as log_file:
        log_file.write(f"{time.strftime('%Y-%m-%d %H:%M:%S')} - {message}\n")

def send_error(client_socket, message):
    error_response = f"HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\nContent-Length: {len(message)}\r\nConnection: keep-alive\r\n\r\n{message}"
    client_socket.sendall(error_response.encode("utf-8"))

def validate_request(request_data):
    try:
        method, url, version = request_data.split("\n")[0].split()
        if method not in {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "CONNECT", "PATCH"}:
            return False, "Invalid method"
        if not version.startswith("HTTP/"):
            return False, "Invalid HTTP version"
        return True, ""
    except ValueError:
        return False, "Invalid request"

def check_cache(file_name):
    file_path = os.path.join(CACHE_DIR, file_name)
    if os.path.exists(file_path):
        return file_path
    return None

def save_to_cache(file_name, content):
    file_path = os.path.join(CACHE_DIR, file_name)
    with open(file_path, "wb") as cache_file:
        cache_file.write(content)

def handle_request(client_socket):
    remote_socket = None
    try:
        client_socket.settimeout(TIMEOUT)
        request_data = client_socket.recv(4096).decode("utf-8")
        
        # Validate request
        is_valid, error_message = validate_request(request_data)
    
        if not is_valid:
            print("Error:", error_message)
            send_error(client_socket, error_message)
            client_socket.close()
            return
        
        print("Request received:\n", request_data)
        log_message(f"Request received:\n{request_data}")

        lines = request_data.split("\n")
        url = urlsplit(lines[0].split()[1])

        server_name = url.hostname
        if server_name is None:
            print("Error: Invalid URL")
            send_error(client_socket, "Invalid URL")
            client_socket.close()
            return

        file_name = url.path.split("/")[-1]
        
        # Check cache
        cached_file = check_cache(file_name)
        if cached_file:
            # Serve cached file
            print(f"Serving file '{file_name}' from cache\n")
            with open(cached_file, "rb") as cache_file:
                client_socket.sendall(cache_file.read())
            return

        # Connect to remote server
        remote_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        remote_socket.settimeout(TIMEOUT)

        if url.scheme == "https":
            remote_socket = ssl.wrap_socket(remote_socket)

        try:
            remote_socket.connect((server_name, 443 if url.scheme == "https" else 80))
        except Exception as e:
            print(f"Error: {e}")
            send_error(client_socket, "Unable to connect to remote server")
            client_socket.close()
            return

        # Send request to remote server
        http_request = f"GET {url.path} HTTP/1.1\r\nHost: {server_name}\r\nConnection: keep-alive\r\nAccept-Encoding: gzip, deflate\r\n\r\n"
        remote_socket.sendall(http_request.encode("utf-8"))

        # Receive response and cache file
        response_data = remote_socket.recv(BUFFER_SIZE)
        # Process response headers and body
        response_headers, response_body = response_data.split(b"\r\n\r\n", 1)
        status_code = int(response_headers.split()[1])

        content_length = None
        content_encoding = None
        for header_line in response_headers.decode().split("\r\n"):
            if header_line.lower().startswith("content-length"):
                content_length = int(header_line.split(":")[1].strip())
            if header_line.lower().startswith("content-encoding"):
                content_encoding = header_line.split(":")[1].strip()

        # Download and cache file if within the size limit
        if content_length is not None and content_length > MAX_FILE_SIZE:
            # Error: File size exceeds maximum limit
            print(f"Error: File size exceeds maximum limit ({MAX_FILE_SIZE / (1024 * 1024)} MB)")
            send_error(client_socket, f"File size exceeds maximum limit ({MAX_FILE_SIZE / (1024 * 1024)} MB)")
            return

        if status_code == 200:
            # Download file and cache it
            print(f"Downloading file '{file_name}'...")
            memory_buffer = io.BytesIO()
            if content_encoding == "gzip":
                with gzip.GzipFile(fileobj=io.BytesIO(response_body)) as gzip_file:
                    decompressed_data = gzip_file.read()
                    memory_buffer.write(decompressed_data)
            else:
                memory_buffer.write(response_body)

            received_length = len(response_body)

            while content_length is None or received_length < content_length:
                response_data = remote_socket.recv(BUFFER_SIZE)
                if not response_data:
                    break
                if content_encoding == "gzip":
                    with gzip.GzipFile(fileobj=io.BytesIO(response_data)) as gzip_file:
                        decompressed_data = gzip_file.read()
                        memory_buffer.write(decompressed_data)
                        received_length += len(decompressed_data)
                else:
                    memory_buffer.write(response_data)
                    received_length += len(response_data)

            print(f"Retrieved: 200 OK")
            print(f"File '{file_name}' downloaded successfully\n")
            save_to_cache(file_name, memory_buffer.getvalue())
            memory_buffer.seek(0)
            client_socket.sendall(memory_buffer.read())

        else:
            # Error: Server returned a non-200 status code
            print(f"Error: Server returned status code {status_code}\n")
            send_error(client_socket, f"Server returned status code {status_code}")

    except Exception as e:
        # Error: Unexpected exception
        print(f"Error: {e}")
        send_error(client_socket, "An unexpected error occurred")
    finally:
        client_socket.close()
        if remote_socket:
            remote_socket.close()

def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} [port] [timeout]")
        sys.exit(1)

    port = int(sys.argv[1])

    try:
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.bind(("0.0.0.0", port))
        server_socket.listen(10)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

    print(f"Server started on port {port}\n")

    while True:
        client_socket, client_addr = server_socket.accept()
        print(f"Connection from {client_addr}")
        thread = threading.Thread(target=handle_request, args=(client_socket,))
        thread.start()

if __name__ == "__main__":
    main()
