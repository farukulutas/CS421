import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class TicTacToeClient {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TicTacToeClient <port number>");
            return;
        }
        int portNumber = Integer.parseInt(args[0]);

        try {
            Socket socket = new Socket("localhost", portNumber);

            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            Scanner scanner = new Scanner(System.in);

            ServerListener serverListener = new ServerListener(inputStream);
            serverListener.start();

            while (true) {
                String message = scanner.nextLine();

                outputStream.write(message.getBytes());
                outputStream.write("\n".getBytes());
                outputStream.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
