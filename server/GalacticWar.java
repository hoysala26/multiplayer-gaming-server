package server;

public class GalacticWar implements Game {
    private ClientHandler p1, p2;
    private int hp1 = 100, hp2 = 100;
    private boolean isFinished = false;

    public GalacticWar(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        broadcast("GAME_START: Galactic War! Use W,A,S,D to Fly and SPACE to Shoot.");
        
        // Assign sides: P1 is Left (Red), P2 is Right (Blue)
        p1.sendMessage("SETUP:LEFT");
        p2.sendMessage("SETUP:RIGHT");
    }

    @Override
    public synchronized void makeMove(String player, String cmd) {
        if (isFinished) return;
        
        ClientHandler sender = (player.equals(p1.username)) ? p1 : p2;
        ClientHandler opponent = (sender == p1) ? p2 : p1;

        // HIGH SPEED RELAY PROTOCOL
        if (cmd.startsWith("POS:")) {
            // cmd format: POS:x:y
            opponent.sendMessage("ENEMY_POS:" + cmd.substring(4));
        }
        else if (cmd.equals("SHOOT")) {
            opponent.sendMessage("ENEMY_SHOOT");
        }
        else if (cmd.equals("HIT")) {
            // Player admits they got hit
            if (sender == p1) hp1 -= 5; else hp2 -= 5;
            
            broadcast("HP:" + hp1 + ":" + hp2);
            opponent.sendMessage("ENEMY_HIT_CONFIRM"); // Tell shooter they hit
            
            if (hp1 <= 0 || hp2 <= 0) {
                isFinished = true;
                String winner = (hp1 > 0) ? p1.username : p2.username;
                broadcast("GAME_OVER: " + winner + " Dominates the Galaxy!");
            }
        }
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        p2.sendMessage(msg);
    }
}