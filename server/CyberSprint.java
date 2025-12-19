package server;

import java.util.Random;

public class CyberSprint implements Game {
    private ClientHandler p1, p2;
    private int distP1 = 0, distP2 = 0; // Distance (0 to 100)
    private final int WIN_DISTANCE = 100;
    
    // Commands: 0=RUN, 1=JUMP, 2=SLIDE
    private String currentObstacle = "RUN"; 
    private boolean isFinished = false;

    public CyberSprint(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        
        broadcast("GAME_START: Cyber Sprint! Watch the commands and react fast!");
        broadcast("SPRINT_UPDATE:0:0"); // Initial positions
        nextObstacle();
    }

    private void nextObstacle() {
        if(isFinished) return;
        
        // Randomly pick next move
        int r = new Random().nextInt(3);
        if (r == 0) currentObstacle = "RUN";
        else if (r == 1) currentObstacle = "JUMP";
        else currentObstacle = "SLIDE";

        broadcast("OBSTACLE:" + currentObstacle);
    }

    @Override
    public synchronized void makeMove(String playerUsername, String moveCommand) {
        if (isFinished) return;

        // Check if player hit the correct button
        boolean correct = moveCommand.equalsIgnoreCase(currentObstacle);
        
        ClientHandler player = (playerUsername.equals(p1.username)) ? p1 : p2;
        
        if (correct) {
            // Success! Move forward
            if (player == p1) distP1 += 5; else distP2 += 5;
            
            player.sendMessage("INFO: Correct! +5m");
            
            // Check Win
            if (distP1 >= WIN_DISTANCE || distP2 >= WIN_DISTANCE) {
                isFinished = true;
                broadcast("SPRINT_UPDATE:" + distP1 + ":" + distP2);
                broadcast("GAME_OVER: " + playerUsername + " Won the Race!");
            } else {
                // Continue Race
                broadcast("SPRINT_UPDATE:" + distP1 + ":" + distP2);
                
                // Only generate new obstacle if this was a RUN command (mashable)
                // or random chance to keep it dynamic
                if (!currentObstacle.equals("RUN") || new Random().nextInt(5) == 0) {
                     nextObstacle();
                }
            }
        } else {
            // Failed! Stumble
            player.sendMessage("HINT: Wrong move! You stumbled!");
            // Penalty: No movement, wait for next command
        }
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}