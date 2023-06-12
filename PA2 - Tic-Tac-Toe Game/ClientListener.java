import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class ClientListener extends Thread {
    private final InputStream inputStream;
    private final OutputStream myOutputStream;
    private final OutputStream opponentOutputStream;
    private final TicTacToe ticTacToe;
    private final String myMarker;
    private final String prefix;
    private boolean isFirstMessage;
    private boolean gameEnded;

    public ClientListener(InputStream inputStream, OutputStream myOutputStream, OutputStream opponentOutputStream, TicTacToe ticTacToe, String myMarker, String prefix) {
        this.inputStream = inputStream;
        this.myOutputStream = myOutputStream;
        this.opponentOutputStream = opponentOutputStream;
        this.ticTacToe = ticTacToe;
        this.myMarker = myMarker;
        this.prefix = prefix;
        this.isFirstMessage = true;
        this.gameEnded = false;

        if (isFirstMessage) {
            try {
                myOutputStream.write(("Connected to the server.\n").getBytes());
                myOutputStream.write(("Retrieved symbol " + myMarker + " and ID=" + prefix.charAt(prefix.length() - 3) + ".\n\n").getBytes());
                myOutputStream.write((ticTacToe.getGameInformation()).getBytes());
                myOutputStream.write(("\nTurn information: " + (ticTacToe.getTurn().equals(myMarker) ? "Your turn!\n" : "Waiting for opponent...\n")).getBytes());
                myOutputStream.write((ticTacToe.getCurrentBoard() + "\n").getBytes());
                myOutputStream.flush();
                isFirstMessage = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytesRead;

        try {
            while (!gameEnded && (bytesRead = inputStream.read(buffer)) != -1) {
                String message = new String(buffer, 0, bytesRead);
                String[] messages = message.split("\\n");

                for (String msg : messages) {
                    msg = msg.trim();

                    if (msg.equals("/restart")) {
                        if (ticTacToe.checkWin() || ticTacToe.checkDraw()) {
                            gameEnded = false;
                            ticTacToe.resetBoard();
                            System.out.println("The game is restarted.");
                            System.out.println("Waiting for Player " + ticTacToe.getTurn() + "'s move");
                            myOutputStream.write(("Game restarted. You have symbol " + myMarker + ".\n\n").getBytes());
                            myOutputStream.write((ticTacToe.getGameInformation()).getBytes());
                            myOutputStream.write(("\nTurn information: " + (ticTacToe.getTurn().equals(myMarker) ? "Your turn!\n" : "Waiting for opponent...\n")).getBytes());
                            myOutputStream.write((ticTacToe.getCurrentBoard() + "\n").getBytes());
                            myOutputStream.flush();

                            opponentOutputStream.write(("Game restarted. You have symbol " + (myMarker.equals("X") ? "O" : "X" + ".\n\n")).getBytes());
                            opponentOutputStream.write((ticTacToe.getGameInformation()).getBytes());
                            opponentOutputStream.write(("\nTurn information: " + (ticTacToe.getTurn().equals(myMarker) ? "Your turn!\n" : "Waiting for opponent...\n")).getBytes());
                            opponentOutputStream.write((ticTacToe.getCurrentBoard() + "\n").getBytes());
                            opponentOutputStream.flush();
                        } else {
                            myOutputStream.write(("The game is not over yet. You cannot restart at this point.\n").getBytes());
                            myOutputStream.flush();
                        }
                    } else if (msg.startsWith("move:")) {
                        int position;
                        System.out.println("Received " + myMarker + " on " + msg.substring(5));

                        try {
                            position = Integer.parseInt(msg.substring(5));
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid move. (The move should be a number.)");
                            myOutputStream.write(("Server says: “This is an illegal move. Please change your move!”\n").getBytes());
                            myOutputStream.flush();
                            continue;
                        }

                        if (!ticTacToe.getTurn().equals(myMarker)) {
                            myOutputStream.write(("It's not your turn!\n").getBytes());
                            myOutputStream.flush();
                        } else if (position < 1 || position > 9) {
                            System.out.println("It is an illegal move. (The move is out of the board.)");
                            myOutputStream.write(("Server says: “This is an illegal move. Please change your move!”\n").getBytes());
                            myOutputStream.flush();
                        } else if (!ticTacToe.placeMove(position, myMarker)) {
                            System.out.println("It is an illegal move. (The position is already occupied.)");
                            myOutputStream.write(("Server says: “This is an illegal move. Please change your move!”\n").getBytes());
                            myOutputStream.flush();
                        } else {
                            System.out.println("It is a legal move.");
                            String board = ticTacToe.getCurrentBoard();
                            myOutputStream.write(("\nTurn information: Waiting for opponent...\n").getBytes());
                            opponentOutputStream.write(("\nTurn information: Your turn!\n").getBytes());
                            String winMessage = ticTacToe.checkWin() ? "You win!\n" : "";
                            String drawMessage = ticTacToe.checkDraw() && !ticTacToe.checkWin() ? "It's a draw!\n" : "";
                            myOutputStream.write((board + winMessage + drawMessage).getBytes());
                            myOutputStream.flush();

                            winMessage = ticTacToe.checkWin() ? "You lose!\n" : "";
                            opponentOutputStream.write((board + winMessage + drawMessage).getBytes());
                            opponentOutputStream.flush();

                            if (!winMessage.equals("") || !drawMessage.equals("")) {
                                if (!drawMessage.equals("")) {
                                    System.out.println("Game over: It's a draw!");
                                } else if (winMessage.equals("You win!\n")) {
                                    System.out.println("Game over: Player " + myMarker + " wins!");
                                } else {
                                    System.out.println("Game over: Player " + (myMarker.equals("X") ? "O" : "X") + " wins!");
                                }
                                break;
                            }

                            ticTacToe.switchTurn();
                            System.out.println("Waiting for Player " + ticTacToe.getTurn() + "’s move");
                        }
                    } else if (!msg.equals("")) {
                        opponentOutputStream.write((prefix + msg + "\n").getBytes());
                        opponentOutputStream.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}