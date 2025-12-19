package server;

import java.awt.Point;
import java.util.*;

public class ShadowRogue implements Game {
    private ClientHandler p1, p2;
    private int[][] map; // 0=Wall, 1=Floor, 2=Exit (Chalice)
    private final int W = 20, H = 20;
    private Point pos1, pos2;
    private ArrayList<Point> enemies = new ArrayList<>();
    private boolean isFinished = false;
    private Random rand = new Random();
    private boolean isSolo = false;

    public ShadowRogue(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        
        if (p2 == null) isSolo = true;

        generateDungeon();
        
        if (isSolo) {
            broadcast("GAME_START: SHADOW ROGUE (SOLO)! Find the Chalice (Yellow) to escape.");
        } else {
            broadcast("GAME_START: ROGUE RACE! First to the Chalice wins!");
        }
        
        sendState();
    }

    private void generateDungeon() {
        map = new int[W][H];
        // 1. Fill with Walls
        for(int x=0; x<W; x++) Arrays.fill(map[x], 0);

        // 2. Dig Rooms (Random Walk Algorithm)
        int x = W/2, y = H/2;
        pos1 = new Point(x, y); // P1 Start
        if (!isSolo) pos2 = new Point(x, y); // P2 Start
        
        // Dig 200 tiles
        for (int i=0; i<200; i++) {
            map[x][y] = 1; // Floor
            int dir = rand.nextInt(4);
            if(dir==0 && y>1) y--;
            if(dir==1 && x<W-2) x++;
            if(dir==2 && y<H-2) y++;
            if(dir==3 && x>1) x--;
        }
        
        // 3. Place Goal (The final position of the digger)
        map[x][y] = 2; // Chalice

        // 4. Place Enemies (Skeletons)
        for(int i=0; i<8; i++) {
            int ex = rand.nextInt(W), ey = rand.nextInt(H);
            // Spawn enemy on floor, but not too close to start
            if (map[ex][ey] == 1 && (Math.abs(ex-pos1.x) > 4)) {
                enemies.add(new Point(ex, ey));
            }
        }
    }

    @Override
    public synchronized void makeMove(String player, String cmd) {
        if (isFinished) return;
        
        Point current = (player.equals(p1.username)) ? pos1 : pos2;
        if (current == null) return; 

        // Calculate Movement
        int dx = 0, dy = 0;
        if (cmd.equals("UP")) dy = -1;
        else if (cmd.equals("DOWN")) dy = 1;
        else if (cmd.equals("LEFT")) dx = -1;
        else if (cmd.equals("RIGHT")) dx = 1;

        int nx = current.x + dx;
        int ny = current.y + dy;

        // Check Wall Collision
        if (nx >= 0 && nx < W && ny >= 0 && ny < H && map[nx][ny] != 0) {
            current.x = nx;
            current.y = ny;
            
            // Win Check (Found Chalice)
            if (map[nx][ny] == 2) {
                isFinished = true;
                broadcast("GAME_OVER: " + player + " found the Golden Chalice!");
                Leaderboard.addWin(player);
            }
        }

        // Enemy Turn (They move every time you move)
        moveEnemies();
        checkDamage();
        sendState();
    }
    
    private void moveEnemies() {
        for(Point e : enemies) {
            // Find closest player
            Point target = pos1;
            if (!isSolo && pos2 != null) {
                if (e.distance(pos2) < e.distance(pos1)) target = pos2;
            }
            
            // Move randomly towards target (Stumble walk)
            if (rand.nextInt(3) == 0) { 
                if (e.x < target.x && map[e.x+1][e.y] != 0) e.x++;
                else if (e.x > target.x && map[e.x-1][e.y] != 0) e.x--;
                else if (e.y < target.y && map[e.x][e.y+1] != 0) e.y++;
                else if (e.y > target.y && map[e.x][e.y-1] != 0) e.y--;
            }
        }
    }

    private void checkDamage() {
        for(Point e : enemies) {
            if (e.equals(pos1)) { 
                p1.sendMessage("HINT: Ouch! A Skeleton attacked you!"); 
            }
            if (!isSolo && e.equals(pos2)) { 
                p2.sendMessage("HINT: Ouch! A Skeleton attacked you!"); 
            }
        }
    }

    private void sendState() {
        // Protocol: ROGUE:W,H:MapData:P1x,P1y:P2x,P2y:Enemies...
        StringBuilder sb = new StringBuilder("ROGUE:");
        sb.append(W).append(",").append(H).append(":");
        
        // 1. Flatten Map (0011010...)
        for(int y=0; y<H; y++) {
            for(int x=0; x<W; x++) sb.append(map[x][y]);
        }
        sb.append(":");
        
        // 2. Player Positions
        sb.append(pos1.x).append(",").append(pos1.y).append(":");
        sb.append(!isSolo ? pos2.x : "-1").append(",").append(!isSolo ? pos2.y : "-1").append(":");
        
        // 3. Enemies
        for(Point e : enemies) sb.append(e.x).append(",").append(e.y).append(",");
        
        broadcast(sb.toString());
    }

    @Override
    public boolean isGameOver() { return isFinished; }

    private void broadcast(String msg) {
        p1.sendMessage(msg);
        if (p2 != null) p2.sendMessage(msg);
    }
}