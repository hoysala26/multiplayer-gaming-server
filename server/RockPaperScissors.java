package server;

public class RockPaperScissors implements Game {
    private ClientHandler p1;
    private ClientHandler p2;
    private String moveP1 = null;
    private String moveP2 = null;
    private boolean isFinished = false;

    public RockPaperScissors(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        broadcast("GAME_START: Rock-Paper-Scissors! Type /move rock, /move paper, or /move scissors");
    }

    @Override
    public synchronized void makeMove(String playerUsername, String moveCommand) {
        if (isFinished) return;

        // Normalize input (make it lowercase)
        String move = moveCommand.toLowerCase();
        if (!move.equals("rock") && !move.equals("paper") && !move.equals("scissors")) {
            getPlayer(playerUsername).sendMessage("INVALID: Use rock, paper, or scissors.");
            return;
        }

        // Store the move
        if (playerUsername.equals(p1.username)) {
            moveP1 = move;
            p1.sendMessage("You chose: " + move);
            p2.sendMessage("Opponent has made a move...");
        } else {
            moveP2 = move;
            p2.sendMessage("You chose: " + move);
            p1.sendMessage("Opponent has made a move...");
        }

        // Check if both have moved
        if (moveP1 != null && moveP2 != null) {
            determineWinner();
        }
    }

    private void determineWinner() {
        String result;
        if (moveP1.equals(moveP2)) {
            result = "It's a DRAW!";
        } else if ((moveP1.equals("rock") && moveP2.equals("scissors")) ||
                   (moveP1.equals("paper") && moveP2.equals("rock")) ||
                   (moveP1.equals("scissors") && moveP2.equals("paper"))) {
            result = p1.username + " WINS!";
        } else {
            result = p2.username + " WINS!";
        }

        broadcast("RESULT: " + p1.username + " (" + moveP1 + ") vs " + 
                  p2.username + " (" + moveP2 + ")");
        broadcast("GAME_OVER: " + result);
        isFinished = true;
    }

    @Override
    public boolean isGameOver() {
        return isFinished;
    }

    private ClientHandler getPlayer(String username) {
        return username.equals(p1.username) ? p1 : p2;
    }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}