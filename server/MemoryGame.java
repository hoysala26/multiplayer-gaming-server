package server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MemoryGame implements Game {
    private ClientHandler p1, p2, currentTurn;
    private String[] board = new String[16]; // The secret emojis
    private boolean[] revealed = new boolean[16]; // Which cards are face up permanently
    private boolean[] tempRevealed = new boolean[16]; // Which cards are currently flipped for this turn
    private int scoreP1 = 0, scoreP2 = 0;
    
    // Turn State
    private int firstPickIndex = -1; // -1 means no card picked yet
    private boolean isFinished = false;

    // The Icons (8 pairs)
    private static final String[] ICONS = {"🚀", "🚀", "💎", "💎", "🔥", "🔥", "🍀", "🍀", 
                                           "👑", "👑", "🍕", "🍕", "🎸", "🎸", "💀", "💀"};

    public MemoryGame(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.currentTurn = p1;

        // Shuffle the board
        List<String> shuffleList = Arrays.asList(ICONS.clone());
        Collections.shuffle(shuffleList);
        shuffleList.toArray(board);

        broadcast("GAME_START: Memory Matrix! Find the matching pairs.");
        sendTurnInfo();
        broadcast("BOARD:" + getHiddenBoardState());
    }

    @Override
    public synchronized void makeMove(String playerUsername, String moveCommand) {
        if (isFinished || !playerUsername.equals(currentTurn.username)) return;

        int index;
        try {
            index = Integer.parseInt(moveCommand);
        } catch (NumberFormatException e) { return; }

        // Validation: Cannot pick same card twice, or already solved card
        if (index < 0 || index >= 16 || revealed[index] || index == firstPickIndex) return;

        // --- GAME LOGIC ---
        
        // Scenario 1: First Card Flip
        if (firstPickIndex == -1) {
            firstPickIndex = index;
            tempRevealed[index] = true;
            broadcast("BOARD:" + getHiddenBoardState()); // Show the card
            currentTurn.sendMessage("INFO: Pick the second card.");
        } 
        // Scenario 2: Second Card Flip
        else {
            tempRevealed[index] = true;
            broadcast("BOARD:" + getHiddenBoardState()); // Show both cards
            
            // Check Match
            if (board[firstPickIndex].equals(board[index])) {
                // MATCH FOUND!
                revealed[firstPickIndex] = true;
                revealed[index] = true;
                
                if (currentTurn == p1) scoreP1++; else scoreP2++;
                broadcast("INFO: MATCH FOUND! " + board[index]);
                
                // Reset pick but KEEP TURN
                firstPickIndex = -1;
                Arrays.fill(tempRevealed, false);
                
                if (checkWin()) endGame();
                else {
                    broadcast("BOARD:" + getHiddenBoardState());
                    currentTurn.sendMessage("INFO: Go again!");
                }
            } else {
                // NO MATCH
                try { Thread.sleep(1000); } catch (InterruptedException e) {} // Small delay to see cards
                
                // Hide them again
                tempRevealed[firstPickIndex] = false;
                tempRevealed[index] = false;
                firstPickIndex = -1;
                
                switchTurn();
                broadcast("BOARD:" + getHiddenBoardState());
            }
        }
    }

    private void switchTurn() {
        currentTurn = (currentTurn == p1) ? p2 : p1;
        sendTurnInfo();
    }

    private void sendTurnInfo() {
        p1.sendMessage("SCORE: You " + scoreP1 + " - " + scoreP2 + " Opponent");
        p2.sendMessage("SCORE: You " + scoreP2 + " - " + scoreP1 + " Opponent");
        broadcast("INFO: It is " + currentTurn.username + "'s turn.");
    }

    private String getHiddenBoardState() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (revealed[i] || tempRevealed[i]) {
                sb.append(board[i]).append(",");
            } else {
                sb.append("?,"); // ? means hidden
            }
        }
        return sb.toString();
    }

    private boolean checkWin() {
        for (boolean b : revealed) if (!b) return false;
        return true;
    }

    private void endGame() {
        isFinished = true;
        String winner = (scoreP1 > scoreP2) ? p1.username : (scoreP2 > scoreP1) ? p2.username : "Draw";
        broadcast("GAME_OVER: Game Finished! Winner: " + winner);
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}