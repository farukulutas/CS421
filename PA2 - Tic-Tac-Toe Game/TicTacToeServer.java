import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TicTacToeServer {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java TicTacToeServer <port number>");
            return;
        }
        int portNumber = Integer.parseInt(args[0]);

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);  

            System.out.println("Server started. Waiting for clients...");

            Socket clientSocket1 = serverSocket.accept(); 
            System.out.println("A client is connected, and it is assigned with the symbol X and ID=0.");

            Socket clientSocket2 = serverSocket.accept(); 
            System.out.println("A client is connected, and it is assigned with the symbol O and ID=1.");
            System.out.println("The game is started.");

            InputStream inputStream1 = clientSocket1.getInputStream();
            OutputStream outputStream1 = clientSocket1.getOutputStream();

            InputStream inputStream2 = clientSocket2.getInputStream();
            OutputStream outputStream2 = clientSocket2.getOutputStream();

            TicTacToe ticTacToe = new TicTacToe();
            System.out.println("Waiting for Player " + ticTacToe.getTurn() + "’s move");

            ClientListener clientListener1 = new ClientListener(inputStream1, outputStream1, outputStream2, ticTacToe, "X", "Client 0: ");
            ClientListener clientListener2 = new ClientListener(inputStream2, outputStream2, outputStream1, ticTacToe, "O", "Client 1: ");

            clientListener1.start();
            clientListener2.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
