package server;

public class TicTacToe implements Game {
    private char[] board;
    private ClientHandler playerX;
    private ClientHandler playerO;
    private String currentTurn; // Stores the Username of the current player
    private boolean isFinished = false;

    public TicTacToe(ClientHandler p1, ClientHandler p2) {
        this.playerX = p1;
        this.playerO = p2;
        this.board = new char[] {'-', '-', '-', '-', '-', '-', '-', '-', '-'};
        this.currentTurn = p1.username; // Player 1 starts
        
        // Notify players
        broadcast("GAME_START: You are playing TicTacToe!");
        playerX.sendMessage("INFO: You are Player X. Your turn.");
        playerO.sendMessage("INFO: You are Player O. Wait for turn.");
        broadcast("BOARD:" + getBoardString());
    }

    @Override
    public synchronized void makeMove(String playerUsername, String moveCommand) {
        if (isFinished) return;

        // 1. Validation: Is it this player's turn?
        if (!playerUsername.equals(currentTurn)) {
            return; // Ignore moves if it's not your turn
        }

        // 2. Parse the Move (String -> Int)
        int index;
        try {
            index = Integer.parseInt(moveCommand);
        } catch (NumberFormatException e) {
            return; // Invalid input
        }

        // 3. Validation: Is the move legal?
        if (index < 0 || index >= 9 || board[index] != '-') {
            return; // Invalid move
        }

        // 4. Apply the Move
        char symbol = (playerX.username.equals(playerUsername)) ? 'X' : 'O';
        board[index] = symbol;

        // 5. Check Win Condition
        if (checkWin(symbol)) {
            broadcast("BOARD:" + getBoardString());
            broadcast("GAME_OVER: Winner is " + playerUsername + "!");
            isFinished = true;
            return;
        }

        // 6. Check Draw Condition
        if (checkDraw()) {
            broadcast("BOARD:" + getBoardString());
            broadcast("GAME_OVER: It's a Draw!");
            isFinished = true;
            return;
        }

        // 7. Switch Turn
        currentTurn = (currentTurn.equals(playerX.username)) ? playerO.username : playerX.username;
        broadcast("BOARD:" + getBoardString());
    }

    @Override
    public boolean isGameOver() {
        return isFinished;
    }

    // --- Helper Methods ---

    private boolean checkWin(char symbol) {
        int[][] wins = {
            {0,1,2}, {3,4,5}, {6,7,8}, // Rows
            {0,3,6}, {1,4,7}, {2,5,8}, // Cols
            {0,4,8}, {2,4,6}           // Diagonals
        };
        for (int[] w : wins) {
            if (board[w[0]] == symbol && board[w[1]] == symbol && board[w[2]] == symbol) {
                return true;
            }
        }
        return false;
    }

    private boolean checkDraw() {
        for (char c : board) {
            if (c == '-') return false; // Found an empty spot, so not a draw
        }
        return true;
    }

    private String getBoardString() {
        return new String(board);
    }

    private void broadcast(String msg) {
        playerX.sendMessage(msg);
        playerO.sendMessage(msg);
    }
}