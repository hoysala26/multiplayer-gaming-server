package server;

import java.util.Random;

public class GuessNumber implements Game {
    private ClientHandler p1;
    private ClientHandler p2;
    private ClientHandler currentTurn; // Whose turn is it?
    private int targetNumber;
    private boolean isFinished = false;

    public GuessNumber(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = p1; // Player 1 starts
        
        // Generate Random Number (1-100)
        this.targetNumber = new Random().nextInt(100) + 1;
        
        broadcast("GAME_START: Guess the Number! I have picked a number between 1 and 100.");
        p1.sendMessage("INFO: It is your turn. Type /move 50 (to guess 50).");
        p2.sendMessage("INFO: Waiting for opponent...");
        
        // DEBUG: Print answer to server console so you can test easily
        System.out.println("DEBUG: Target Number is " + targetNumber);
    }

    @Override
    public synchronized void makeMove(String playerUsername, String moveCommand) {
        if (isFinished) return;

        // 1. Check Turn
        if (!playerUsername.equals(currentTurn.username)) {
            return; // Ignore moves out of turn
        }

        // 2. Parse Guess
        int guess;
        try {
            guess = Integer.parseInt(moveCommand.trim());
        } catch (NumberFormatException e) {
            currentTurn.sendMessage("INVALID: Please enter a number. Example: /move 42");
            return;
        }

        // 3. Game Logic
        broadcast(playerUsername + " guessed: " + guess);

        if (guess == targetNumber) {
            broadcast("GAME_OVER: Correct! " + playerUsername + " WINS!");
            isFinished = true;
        } else if (guess < targetNumber) {
            broadcast("HINT: Too Low!");
            switchTurn();
        } else {
            broadcast("HINT: Too High!");
            switchTurn();
        }
    }

    private void switchTurn() {
        currentTurn = (currentTurn == p1) ? p2 : p1;
        currentTurn.sendMessage("It is your turn.");
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}