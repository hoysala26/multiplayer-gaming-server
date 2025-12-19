package server;

import java.awt.Point;
import java.util.*;

public class SnakeBattle implements Game {
    private ClientHandler p1, p2;
    private ArrayList<Point> snake1 = new ArrayList<>();
    private ArrayList<Point> snake2 = new ArrayList<>();
    private Point food;
    private int dir1 = 1; // 0=Up, 1=Right, 2=Down, 3=Left
    private int dir2 = 3; 
    private boolean isFinished = false;
    private final int W = 40, H = 30; // Grid Size
    private boolean isSolo = false;

    public SnakeBattle(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        
        // If Player 2 is missing, it is SOLO MODE
        if (p2 == null) isSolo = true;

        // Init Player 1 (Green)
        snake1.add(new Point(5, 5)); 
        snake1.add(new Point(4, 5));

        // Init Player 2 (Orange) - ONLY IF NOT SOLO
        if (!isSolo) {
            snake2.add(new Point(35, 25)); 
            snake2.add(new Point(36, 25));
        }

        spawnFood();
        
        if (isSolo) {
            broadcast("GAME_START: CLASSIC SNAKE! Eat food to grow.");
        } else {
            broadcast("GAME_START: SNAKE BATTLE! Green vs Orange.");
        }

        startGameLoop();
    }

    private void startGameLoop() {
        new Thread(() -> {
            while (!isFinished) {
                try {
                    Thread.sleep(100); // Game Speed
                    update();
                    
                    // Protocol: SNAKE:foodX,foodY:s1Body...:s2Body...
                    StringBuilder msg = new StringBuilder("SNAKE:");
                    msg.append(food.x).append(",").append(food.y).append(":");
                    
                    // Snake 1 Data
                    for(Point p : snake1) msg.append(p.x).append(",").append(p.y).append(",");
                    msg.append(":");
                    
                    // Snake 2 Data (Empty if Solo)
                    if (!isSolo) {
                        for(Point p : snake2) msg.append(p.x).append(",").append(p.y).append(",");
                    }
                    
                    broadcast(msg.toString());
                } catch (Exception e) { isFinished = true; }
            }
        }).start();
    }

    private void update() {
        if (isFinished) return;

        // Move Player 1
        moveSnake(snake1, dir1, p1.username);

        // Move Player 2 (Only if PvP)
        if (!isSolo) {
            moveSnake(snake2, dir2, p2.username);
        }
        
        checkCollisions();
    }

    private void moveSnake(ArrayList<Point> snake, int dir, String name) {
        Point head = snake.get(0);
        Point newHead = new Point(head);
        
        if (dir == 0) newHead.y--;
        if (dir == 1) newHead.x++;
        if (dir == 2) newHead.y++;
        if (dir == 3) newHead.x--;

        // Wall Crash Check
        if (newHead.x < 0 || newHead.x >= W || newHead.y < 0 || newHead.y >= H) {
            endGame(name); // That player loses
            return;
        }

        snake.add(0, newHead); // Grow Head
        
        if (newHead.equals(food)) {
            spawnFood(); // Ate Food -> Grow
        } else {
            snake.remove(snake.size() - 1); // Normal Move -> Remove tail
        }
    }

    private void checkCollisions() {
        if(isFinished) return;
        Point h1 = snake1.get(0);

        // 1. Did P1 hit Self?
        if (snake1.subList(1, snake1.size()).contains(h1)) {
            endGame(p1.username);
            return;
        }

        // PvP Collisions (Only check if opponent exists)
        if (!isSolo) {
            Point h2 = snake2.get(0);
            
            // Did P2 hit Self?
            if (snake2.subList(1, snake2.size()).contains(h2)) endGame(p2.username);

            // Head-to-Body Collisions
            if (snake2.contains(h1)) endGame(p1.username); // P1 hit P2
            if (snake1.contains(h2)) endGame(p2.username); // P2 hit P1
        }
    }

    private void spawnFood() {
        food = new Point((int)(Math.random()*W), (int)(Math.random()*H));
    }

    private void endGame(String loser) {
        if(isFinished) return;
        isFinished = true;
        
        String msg;
        if (isSolo) {
            // Solo Game Over Message
            msg = "GAME_OVER: You Crashed! Score: " + (snake1.size() - 2);
            Leaderboard.addWin(p1.username); // Save high score attempt
        } else {
            // PvP Game Over Message
            String winner = loser.equals(p1.username) ? p2.username : p1.username;
            msg = "GAME_OVER: " + winner + " WINS THE BATTLE!";
            Leaderboard.addWin(winner);
        }
        
        broadcast(msg);
    }

    @Override
    public synchronized void makeMove(String player, String cmd) {
        int newDir = -1;
        if (cmd.equals("UP")) newDir = 0;
        else if (cmd.equals("RIGHT")) newDir = 1;
        else if (cmd.equals("DOWN")) newDir = 2;
        else if (cmd.equals("LEFT")) newDir = 3;

        if (newDir != -1) {
            if (player.equals(p1.username)) {
                if (!isOpposite(dir1, newDir)) dir1 = newDir;
            } else if (!isSolo && p2 != null && player.equals(p2.username)) {
                if (!isOpposite(dir2, newDir)) dir2 = newDir;
            }
        }
    }
    
    private boolean isOpposite(int d1, int d2) {
        return (d1 == 0 && d2 == 2) || (d1 == 2 && d2 == 0) || 
               (d1 == 1 && d2 == 3) || (d1 == 3 && d2 == 1);
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        if (p2 != null) p2.sendMessage(msg);
    }
}