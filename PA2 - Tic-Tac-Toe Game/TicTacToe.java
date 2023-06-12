public class TicTacToe {
    private final String[] board = new String[9];
    private String turn;

    public TicTacToe() {
        for (int i = 0; i < 9; i++) {
            board[i] = " ";
        }
        turn = (Math.random() < 0.5) ? "X" : "O";
    }

    public String getTurn() {
        return turn;
    }

    public void switchTurn() {
        turn = turn.equals("X") ? "O" : "X";
    }

    public synchronized boolean placeMove(int position, String marker) {
        if (position < 1 || position > 9 || !board[position - 1].equals(" ")) {
            return false;
        } else {
            board[position - 1] = marker;
            return true;
        }
    }

    public synchronized boolean makeMove(int position, String marker) {
        if (position < 0 || position >= 9 || !board[position].equals(" ")) {
            return false;
        } else {
            board[position] = marker;
            return true;
        }
    }

    public String getGameInformation() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nWelcome to the Tic Tac Toe game!\n");
        sb.append("To make a move, send a message in the following format:\n");
        sb.append("move:<number>\n");
        sb.append("where <number> is a number between 1 and 9 representing the position on the board.\n");
        sb.append("For example, to place your symbol at position 5, send 'move:5'.\n");
        sb.append("_________\n1 |2 |3 |\n4 |5 |6 |\n7 |8 |9 |\n");
        sb.append("Remember, messages that don't follow the move format will be considered as chat messages to your opponent.");
        sb.append("When the game is over, you can restart the game by typing '/restart'.");
        sb.append("You can leave the game at any time by typing '/quit'.");
        return sb.toString();
    }

    public String getCurrentBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("State of the board:\n_________\n");
        for (int i = 0; i < 9; i++) {
            if (i % 3 == 0 && i != 0) {
                sb.append("\n");
            }

            if ( board[i] != " ") {
                sb.append(board[i] + " |");
            }
            else {
                sb.append("__|");
            }

        }
        sb.append("\n");
        return sb.toString();
    }

    public boolean checkWin() {
        // rows check
        for (int i = 0; i < 9; i += 3) {
            if (!board[i].equals(" ") && board[i].equals(board[i + 1]) && board[i].equals(board[i + 2])) {
                return true;
            }
        }

        // columns check
        for (int i = 0; i < 3; i++) {
            if (!board[i].equals(" ") && board[i].equals(board[i + 3]) && board[i].equals(board[i + 6])) {
                return true;
            }
        }

        // diagonals check
        if (!board[0].equals(" ") && board[0].equals(board[4]) && board[0].equals(board[8])) {
            return true;
        }
        if (!board[2].equals(" ") && board[2].equals(board[4]) && board[2].equals(board[6])) {
            return true;
        }

        return false;
    }

    public boolean checkDraw() {
        for (String s : board) {
            if (s.equals(" ")) {
                return false;
            }
        }
        return true;
    }

    public void resetBoard() {
        for (int i = 0; i < 9; i++) {
            board[i] = " ";
        }
        turn = (Math.random() < 0.5) ? "X" : "O";
    }
}
