package client;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.*; 
import javax.swing.border.EmptyBorder;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    // --- THEME COLORS ---
    private final Color COL_DARK_BG = new Color(15, 15, 25);
    private final Color COL_ACCENT_CYAN = new Color(0, 255, 255);
    private final Color COL_ACCENT_MAGENTA = new Color(255, 0, 255);
    private final Color COL_ACCENT_GREEN = new Color(0, 255, 128);
    private final Color COL_CARD_BG = new Color(30, 30, 45, 200); 
    private final Color COL_DARK_LOGO = new Color(40, 40, 50); // THIS WAS MISSING
    private final Color COL_VAL_RED = new Color(255, 70, 85); 

    // GUI COMPONENTS
    private JFrame frame = new JFrame("GALAXY GAMING HUB | ULTIMATE EDITION");
    private JPanel mainContainer = new JPanel();
    private CardLayout cardLayout = new CardLayout();

    // PANELS
    private JPanel lobbyPanel, tttPanel, rpsPanel, guessPanel, memoryPanel, sprintPanel;
    private SpacePanel spacePanel;
    private SnakePanel snakePanel;
    private SurvivalPanel survivalPanel;

    // CHAT & UTILS
    private JTextArea chatArea = new JTextArea(6, 40);
    private JTextField chatInput = new JTextField(30);
    private JButton[] tttButtons = new JButton[9];
    private JButton[] memoryButtons = new JButton[16];
    private JLabel guessHintLabel = new JLabel("ENTER CODE: 0 - 100");
    private JLabel sprintCmdLabel = new JLabel("WAITING FOR SIGNAL...");
    private JLabel p1RunIcon = new JLabel("🏃"), p2RunIcon = new JLabel("🏃");

    // GAME VARIABLES
    private Timer runAnimTimer;
    private int p1X=10, p2X=10, targetP1X=10, targetP2X=10; 
    private Timer spaceLoop;
    private boolean isLeftPlayer = true;
    private int myX=50, myY=250, enemyX=700, enemyY=250;
    private int myHP=100, enemyHP=100;
    private Set<Integer> keysPressed = new HashSet<>();
    private ArrayList<Bullet> myBullets = new ArrayList<>();
    private ArrayList<Bullet> enemyBullets = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();

    public GameClient() {
        setupNetwork();
        setupGUI();
        login();
        startListener();
    }

    private void setupNetwork() {
        try {
            socket = new Socket("127.0.0.1", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "SERVER OFFLINE! Start GalaxyServer.jar first.");
            System.exit(0);
        }
    }

    private void setupGUI() {
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(COL_DARK_BG);

        mainContainer = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 20), getWidth(), getHeight(), new Color(40, 20, 60));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // --- CREATE ALL GAMES ---
        createLobby();
        createTicTacToe();
        createRPS();
        createGuess();
        createMemory();
        createSprint();
        createSpace();
        createSnake(); 
        createSurvival(); 

        // --- CHAT BAR ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(20, 20, 30));
        bottomPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        chatArea.setEditable(false);
        chatArea.setBackground(new Color(10, 10, 15));
        chatArea.setForeground(COL_ACCENT_CYAN);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        bottomPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(20, 20, 30));
        chatInput.setBackground(Color.WHITE);
        chatInput.setForeground(Color.BLACK);
        chatInput.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        chatInput.addActionListener(e -> { 
            safeSound("SELECT");
            out.println(chatInput.getText()); 
            chatInput.setText(""); 
            if(survivalPanel != null && survivalPanel.isVisible()) survivalPanel.requestFocusInWindow();
        });
        inputPanel.add(chatInput, BorderLayout.CENTER);

        JButton quitBtn = new JButton(" EXIT TO LOBBY ");
        quitBtn.setBackground(new Color(200, 50, 50));
        quitBtn.setForeground(Color.WHITE);
        quitBtn.setFocusPainted(false);
        quitBtn.addActionListener(e -> {
            safeSound("SELECT");
            stopAllGamesAndShowLobby();
        });
        inputPanel.add(quitBtn, BorderLayout.EAST);

        bottomPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(mainContainer, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setSize(1100, 850); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }

    private void safeSound(String type) {
        try {
            if(type.equals("SHOOT")) SoundEngine.playShoot();
            else if(type.equals("HIT")) SoundEngine.playHit();
            else if(type.equals("WIN")) SoundEngine.playWin();
            else SoundEngine.playSelect();
        } catch(Throwable t) {}
    }

    private void stopAllGamesAndShowLobby() {
        if(spaceLoop != null && spaceLoop.isRunning()) spaceLoop.stop();
        if(runAnimTimer != null && runAnimTimer.isRunning()) runAnimTimer.stop();
        if(survivalPanel != null) survivalPanel.stopGame();
        frame.setCursor(Cursor.getDefaultCursor());
        cardLayout.show(mainContainer, "LOBBY");
        chatInput.requestFocus();
    }

    // --- 1. LOBBY ---
    private void createLobby() {
        lobbyPanel = new JPanel(new GridLayout(2, 4, 20, 20)); 
        lobbyPanel.setOpaque(false);
        lobbyPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        addGameCard("TIC-TAC-TOE", "Classic", "images/tictactoe.png", "/play tictactoe", COL_ACCENT_CYAN, false);
        addGameCard("RPS ARENA", "PvP Battle", "images/rps.png", "/play rps", COL_ACCENT_MAGENTA, false);
        addGameCard("SAFE CRACKER", "Logic Puzzle", "images/guess.png", "/play guess", Color.ORANGE, false);
        addGameCard("MEMORY MATRIX", "Brain Test", "images/memory.png", "/play memory", new Color(255, 140, 0), false);
        addGameCard("CYBER SPRINT", "Racing", "images/sprint.png", "/play sprint", COL_ACCENT_GREEN, false);
        addGameCard("GALACTIC WAR", "Shooter", "images/space.png", "/play space", new Color(255, 50, 50), true);
        addGameCard("SNAKE", "Solo / PvP", "images/snake.png", "/play snake", Color.GREEN, true);
        addGameCard("CRIMSON SURVIVAL", "Zombie Horde", "images/rogue.png", "SURVIVAL", COL_VAL_RED, false); 

        mainContainer.add(lobbyPanel, "LOBBY");
    }

    private void addGameCard(String title, String sub, String imagePath, String cmd, Color glow, boolean hasModes) {
        JButton card = new JButton();
        card.setLayout(new BorderLayout());
        card.setBackground(COL_CARD_BG);
        card.setBorder(BorderFactory.createLineBorder(glow, 2));
        card.setFocusPainted(false);
        
        JLabel iconLbl = new JLabel();
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        try {
            ImageIcon originalIcon = new ImageIcon(imagePath);
            if (originalIcon.getIconWidth() > 0) {
                Image scaledImg = originalIcon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
                iconLbl.setIcon(new ImageIcon(scaledImg));
            } else {
                iconLbl.setText("IMG?");
                iconLbl.setForeground(COL_DARK_LOGO);
            }
        } catch (Exception e) { iconLbl.setText("ERR"); }
        
        JLabel titleLbl = new JLabel("<html><center><b>"+title+"</b><br><font size=3 color=gray>"+sub+"</font></center></html>", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(glow);
        card.add(iconLbl, BorderLayout.CENTER);
        card.add(titleLbl, BorderLayout.SOUTH);
        
        card.addActionListener(e -> { 
            safeSound("SELECT");
            if(cmd.equals("SURVIVAL")) {
                showSurvivalModeSelector();
            }
            else if (hasModes) showModeSelector(title, cmd);
            else out.println(cmd); 
        });
        lobbyPanel.add(card);
    }

    // --- POPUP FOR ZOMBIE GAME ---
    private void showSurvivalModeSelector() {
        String[] options = {"SOLO", "MULTIPLAYER"};
        int choice = JOptionPane.showOptionDialog(frame, "Select Mode for Crimson Survival:", "Game Mode", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        cardLayout.show(mainContainer, "SURVIVAL");
        if (choice == 0) {
            survivalPanel.startGame(false); // Solo
        } else {
            survivalPanel.startGame(true);  // Multiplayer
        }
    }

    private void showModeSelector(String title, String baseCmd) {
        String[] options = {"MULTIPLAYER", "SOLO"};
        int choice = JOptionPane.showOptionDialog(frame, "Select Mode:", title, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == 0) out.println(baseCmd); 
        else if (choice == 1) {
            if (baseCmd.contains("space")) out.println("/play space");
            else out.println(baseCmd + " solo");
        }
    }

    // --- GAMES ---
    private void createTicTacToe() {
        tttPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        tttPanel.setOpaque(false);
        tttPanel.setBorder(new EmptyBorder(50,150,50,150));
        for(int i=0; i<9; i++) {
            int x=i;
            tttButtons[i] = new JButton();
            tttButtons[i].setFont(new Font("Arial", Font.BOLD, 60));
            tttButtons[i].setBackground(new Color(30,30,40));
            tttButtons[i].setFocusPainted(false);
            tttButtons[i].addActionListener(e -> { safeSound("SELECT"); out.println("/move "+x); });
            tttPanel.add(tttButtons[i]);
        }
        mainContainer.add(tttPanel, "TICTACTOE");
    }

    private void createRPS() {
        rpsPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        rpsPanel.setOpaque(false);
        rpsPanel.setBorder(new EmptyBorder(100, 50, 100, 50));
        addRPSBtn("ROCK", "🪨", new Color(100, 100, 100));
        addRPSBtn("PAPER", "📄", new Color(200, 200, 200));
        addRPSBtn("SCISSORS", "✂️", new Color(255, 100, 100));
        mainContainer.add(rpsPanel, "RPS");
    }
    private void addRPSBtn(String txt, String ico, Color c) {
        JButton b = new JButton("<html><center><font size=20>"+ico+"</font><br>"+txt+"</center></html>");
        b.setBackground(c); b.setFocusPainted(false);
        b.addActionListener(e -> { safeSound("SELECT"); out.println("/move "+txt.toLowerCase()); });
        rpsPanel.add(b);
    }

    private void createGuess() {
        guessPanel = new JPanel(new GridBagLayout());
        guessPanel.setOpaque(false);
        JPanel vault = new JPanel(new GridLayout(3, 1, 10, 10));
        vault.setBackground(new Color(0,0,0,150));
        vault.setBorder(BorderFactory.createLineBorder(Color.ORANGE, 3));
        guessHintLabel.setFont(new Font("Consolas", Font.BOLD, 24));
        guessHintLabel.setForeground(Color.ORANGE);
        guessHintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JTextField numInput = new JTextField();
        numInput.setFont(new Font("Consolas", Font.BOLD, 30));
        numInput.setHorizontalAlignment(JTextField.CENTER);
        JButton submit = new JButton("UNLOCK VAULT");
        submit.setBackground(Color.ORANGE);
        submit.addActionListener(e -> { safeSound("SELECT"); out.println("/move "+numInput.getText()); numInput.setText(""); });
        vault.add(guessHintLabel); vault.add(numInput); vault.add(submit);
        guessPanel.add(vault);
        mainContainer.add(guessPanel, "GUESS");
    }

    private void createMemory() {
        memoryPanel = new JPanel(new GridLayout(4, 4, 10, 10));
        memoryPanel.setOpaque(false);
        memoryPanel.setBorder(new EmptyBorder(20,100,20,100));
        for(int i=0; i<16; i++) {
            int x=i;
            memoryButtons[i] = new JButton("?");
            memoryButtons[i].setFont(new Font("Segoe UI Emoji", Font.BOLD, 32));
            memoryButtons[i].setBackground(new Color(40,40,60)); 
            memoryButtons[i].setForeground(Color.WHITE);
            memoryButtons[i].setFocusPainted(false);
            memoryButtons[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            memoryButtons[i].addActionListener(e -> { safeSound("SELECT"); out.println("/move "+x); });
            memoryPanel.add(memoryButtons[i]);
        }
        mainContainer.add(memoryPanel, "MEMORY");
    }

    private void createSprint() {
        sprintPanel = new JPanel(null);
        sprintPanel.setOpaque(false);
        JLabel track1 = new JLabel("_________________________________________________________________________");
        track1.setForeground(Color.GRAY); track1.setBounds(50, 150, 800, 20); sprintPanel.add(track1);
        JLabel track2 = new JLabel("_________________________________________________________________________");
        track2.setForeground(Color.GRAY); track2.setBounds(50, 250, 800, 20); sprintPanel.add(track2);
        JLabel finish = new JLabel("🏁"); finish.setFont(new Font("Arial",0,40)); finish.setBounds(750, 100, 50, 200); sprintPanel.add(finish);
        p1RunIcon.setBounds(10, 110, 50, 50); p1RunIcon.setFont(new Font("Arial",0,40)); sprintPanel.add(p1RunIcon);
        p2RunIcon.setBounds(10, 210, 50, 50); p2RunIcon.setFont(new Font("Arial",0,40)); sprintPanel.add(p2RunIcon);
        sprintCmdLabel.setBounds(0, 300, 900, 50);
        sprintCmdLabel.setFont(new Font("Impact", Font.ITALIC, 40));
        sprintCmdLabel.setForeground(Color.WHITE);
        sprintCmdLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sprintPanel.add(sprintCmdLabel);
        JPanel ctrls = new JPanel(new GridLayout(1, 3, 20, 0));
        ctrls.setBounds(100, 400, 700, 80); ctrls.setOpaque(false);
        JButton b1 = new JButton("JUMP"); b1.setBackground(COL_ACCENT_CYAN); b1.addActionListener(e->{safeSound("SELECT");out.println("/move JUMP");});
        JButton b2 = new JButton("RUN"); b2.setBackground(COL_ACCENT_GREEN); b2.addActionListener(e->{safeSound("SELECT");out.println("/move RUN");});
        JButton b3 = new JButton("SLIDE"); b3.setBackground(COL_ACCENT_MAGENTA); b3.addActionListener(e->{safeSound("SELECT");out.println("/move SLIDE");});
        ctrls.add(b1); ctrls.add(b2); ctrls.add(b3);
        sprintPanel.add(ctrls);
        mainContainer.add(sprintPanel, "SPRINT");
        Timer animTimer = new Timer(16, e -> {
            if(p1X < targetP1X) p1X+=3; if(p2X < targetP2X) p2X+=3;
            p1RunIcon.setBounds(p1X, 110, 50, 50); p2RunIcon.setBounds(p2X, 210, 50, 50);
        });
        animTimer.start();
        runAnimTimer = new Timer(150, e->{
            p1RunIcon.setText(p1RunIcon.getText().equals("🏃")?"🚶":"🏃");
            p2RunIcon.setText(p2RunIcon.getText().equals("🏃")?"🚶":"🏃");
        });
    }

    private void createSpace() {
        spacePanel = new SpacePanel();
        mainContainer.add(spacePanel, "SPACE");
    }

    class SpacePanel extends JPanel {
        public SpacePanel() {
            setOpaque(false); setFocusable(true);
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { 
                    keysPressed.add(e.getKeyCode()); 
                    if(e.getKeyCode()==KeyEvent.VK_SPACE) {
                        safeSound("SHOOT");
                        myBullets.add(new Bullet(myX+(isLeftPlayer?40:-10), myY+15));
                        out.println("/move SHOOT");
                    }
                }
                public void keyReleased(KeyEvent e) { keysPressed.remove(e.getKeyCode()); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE); for(int i=0; i<30; i++) g2.fillOval((i*29)%900, (i*37)%700, 2, 2);
            g2.setColor(Color.GREEN); int[] xp = {myX, myX+40, myX}; int[] yp = {myY, myY+20, myY+40}; if(!isLeftPlayer) { xp[0]+=40; xp[1]-=40; xp[2]+=40; } g2.fillPolygon(xp, yp, 3);
            g2.setColor(Color.RED); int[] ex = {enemyX, enemyX+40, enemyX}; int[] ey = {enemyY, enemyY+20, enemyY+40}; if(isLeftPlayer) { ex[0]+=40; ex[1]-=40; ex[2]+=40; } g2.fillPolygon(ex, ey, 3);
            g2.setColor(Color.YELLOW); for(Bullet b:myBullets) g2.fillRect(b.x,b.y,10,4);
            g2.setColor(Color.MAGENTA); for(Bullet b:enemyBullets) g2.fillRect(b.x,b.y,10,4);
            g2.setColor(Color.ORANGE); for(Particle p:particles) g2.fillRect((int)p.x,(int)p.y,3,3);
            g2.setColor(Color.WHITE); g2.drawRect(20, 20, 200, 10); g2.drawRect(650, 20, 200, 10);
            g2.setColor(Color.GREEN); g2.fillRect(21, 21, myHP*2, 8);
            g2.setColor(Color.RED); g2.fillRect(651, 21, enemyHP*2, 8);
        }
    }
    
    private void startSpaceGame() {
        spaceLoop = new Timer(16, e -> {
            if(keysPressed.contains(KeyEvent.VK_W) && myY>0) myY-=5;
            if(keysPressed.contains(KeyEvent.VK_S) && myY<600) myY+=5;
            if(keysPressed.contains(KeyEvent.VK_A) && myX>0) myX-=5;
            if(keysPressed.contains(KeyEvent.VK_D) && myX<850) myX+=5;
            out.println("/move POS:"+myX+":"+myY);
            updateBullets(myBullets, 10); updateBullets(enemyBullets, -10);
            Rectangle me = new Rectangle(myX, myY, 40, 40);
            Iterator<Bullet> it = enemyBullets.iterator();
            while(it.hasNext()) { if(me.intersects(it.next().getRect())) { it.remove(); out.println("/move HIT"); createExplosion(myX, myY); safeSound("HIT"); } }
            Iterator<Particle> pi = particles.iterator(); while(pi.hasNext()) if(!pi.next().update()) pi.remove();
            spacePanel.repaint();
        });
        spaceLoop.start();
        spacePanel.requestFocusInWindow();
    }
    private void updateBullets(ArrayList<Bullet> list, int s) { int dir = isLeftPlayer?1:-1; Iterator<Bullet> it = list.iterator(); while(it.hasNext()) { Bullet b=it.next(); b.x+=(s*dir); if(b.x<0||b.x>900) it.remove(); } }
    private void createExplosion(int x, int y) { for(int i=0;i<10;i++) particles.add(new Particle(x,y)); }
    class Bullet { int x,y; public Bullet(int x,int y){this.x=x;this.y=y;} Rectangle getRect(){return new Rectangle(x,y,10,4);} }
    class Particle { float x,y,vx,vy; int life=20; public Particle(int x,int y){this.x=x;this.y=y; vx=(float)(Math.random()*6-3); vy=(float)(Math.random()*6-3);} boolean update(){x+=vx;y+=vy;life--;return life>0;} }

    // --- 8. SNAKE ---
    private void createSnake() {
        snakePanel = new SnakePanel();
        mainContainer.add(snakePanel, "SNAKE");
    }
    class SnakePanel extends JPanel {
        private ArrayList<Point> s1 = new ArrayList<>();
        private ArrayList<Point> s2 = new ArrayList<>();
        private Point food = new Point(-1,-1);
        private final int TILE_SIZE = 20;
        public SnakePanel() {
            setOpaque(false); setFocusable(true);
            addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) { int k=e.getKeyCode(); if(k==KeyEvent.VK_W||k==KeyEvent.VK_UP) out.println("/move UP"); if(k==KeyEvent.VK_D||k==KeyEvent.VK_RIGHT) out.println("/move RIGHT"); if(k==KeyEvent.VK_S||k==KeyEvent.VK_DOWN) out.println("/move DOWN"); if(k==KeyEvent.VK_A||k==KeyEvent.VK_LEFT) out.println("/move LEFT"); }});
        }
        public void updateState(String data) {
            try { String[] sections = data.split(":"); String[] f = sections[0].split(","); food = new Point(Integer.parseInt(f[0]), Integer.parseInt(f[1]));
            s1.clear(); String[] p1Raw = sections[1].split(","); for(int i=0; i<p1Raw.length; i+=2) s1.add(new Point(Integer.parseInt(p1Raw[i]), Integer.parseInt(p1Raw[i+1])));
            s2.clear(); if(sections.length > 2) { String[] p2Raw = sections[2].split(","); for(int i=0; i<p2Raw.length; i+=2) s2.add(new Point(Integer.parseInt(p2Raw[i]), Integer.parseInt(p2Raw[i+1]))); }
            repaint(); } catch(Exception e) {}
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g); Graphics2D g2 = (Graphics2D)g; g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(50,50,50)); for(int i=0; i<getWidth(); i+=TILE_SIZE) g2.drawLine(i,0,i,getHeight()); for(int i=0; i<getHeight(); i+=TILE_SIZE) g2.drawLine(0,i,getWidth(),i);
            g2.setColor(Color.RED); g2.fillOval(food.x*TILE_SIZE, food.y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
            g2.setColor(Color.GREEN); for(Point p : s1) g2.fillRect(p.x*TILE_SIZE, p.y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
            g2.setColor(Color.ORANGE); for(Point p : s2) g2.fillRect(p.x*TILE_SIZE, p.y*TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
    }

    // --- 9. CRIMSON SURVIVAL (UPDATED) ---
    private void createSurvival() {
        survivalPanel = new SurvivalPanel();
        mainContainer.add(survivalPanel, "SURVIVAL");
    }

    class SurvivalPanel extends JPanel implements ActionListener {
        Timer timer = new Timer(16, this);
        boolean running = false;
        double px=400, py=300;
        int score=0, wave=1;
        int fireCooldown = 0;
        int ammo = 30;
        boolean isMultiplayer = false; 

        // Multiplayer Friend
        double friendX = -100, friendY = -100, friendAng = 0;
        boolean friendShooting = false;
        
        class SBullet { 
            double x,y,vx,vy; 
            public SBullet(double x, double y, double ang) {
                this.x=x; this.y=y;
                this.vx = Math.cos(ang)*15; 
                this.vy = Math.sin(ang)*15;
            }
        }
        
        ArrayList<Point> zombies = new ArrayList<>();
        ArrayList<SBullet> bullets = new ArrayList<>(); 
        boolean w,a,s,d, mouseDown;
        int mx, my;

        public SurvivalPanel() {
            setOpaque(false); setFocusable(true);
            
            addKeyListener(new KeyAdapter() {
                public void keyPressed(KeyEvent e) { int k=e.getKeyCode(); if(k==KeyEvent.VK_W)w=true; if(k==KeyEvent.VK_A)a=true; if(k==KeyEvent.VK_S)s=true; if(k==KeyEvent.VK_D)d=true; }
                public void keyReleased(KeyEvent e) { int k=e.getKeyCode(); if(k==KeyEvent.VK_W)w=false; if(k==KeyEvent.VK_A)a=false; if(k==KeyEvent.VK_S)s=false; if(k==KeyEvent.VK_D)d=false; }
            });
            
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) { mouseDown=true; requestFocusInWindow(); }
                public void mouseReleased(MouseEvent e) { mouseDown=false; }
                public void mouseEntered(MouseEvent e) { requestFocusInWindow(); }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() { 
                public void mouseMoved(MouseEvent e){mx=e.getX();my=e.getY();} 
                public void mouseDragged(MouseEvent e){mx=e.getX();my=e.getY();} 
            });
        }

        public void startGame(boolean multiplayer) { 
            this.isMultiplayer = multiplayer;
            px=getWidth()/2; py=getHeight()/2; 
            score=0; wave=1; fireCooldown=0; ammo=30; 
            zombies.clear(); bullets.clear(); 
            w=false; a=false; s=false; d=false; mouseDown=false; 
            running=true; 
            
            // --- HIDE CURSOR ---
            BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
            frame.setCursor(blankCursor);
            
            timer.start(); 
            requestFocusInWindow(); 
        }
        
        public void stopGame() { running=false; timer.stop(); }

        public void updateFriend(double x, double y, double a, boolean shoot) {
            friendX = x; friendY = y; friendAng = a; friendShooting = shoot;
        }

        public void actionPerformed(ActionEvent e) {
            if(!running) return;
            try {
                if(!isFocusOwner()) requestFocusInWindow();

                if(w && py > 0) py-=4; 
                if(s && py < getHeight()) py+=4; 
                if(a && px > 0) px-=4; 
                if(d && px < getWidth()) px+=4;

                double angle = Math.atan2(my-py, mx-px);
                
                // --- MULTIPLAYER SYNC ---
                if(isMultiplayer && System.currentTimeMillis() % 30 == 0) { 
                    out.println("/move SURV:" + (int)px + ":" + (int)py + ":" + String.format("%.2f", angle) + ":" + (mouseDown?1:0));
                }

                if(mouseDown && fireCooldown <= 0 && ammo > 0) {
                    bullets.add(new SBullet(px, py, angle));
                    safeSound("SHOOT");
                    fireCooldown = 8; 
                    ammo--; 
                }
                if(fireCooldown > 0) fireCooldown--;

                Iterator<SBullet> bi = bullets.iterator();
                while(bi.hasNext()) {
                    SBullet b = bi.next();
                    b.x += b.vx; b.y += b.vy;
                    if(b.x<0||b.x>getWidth()||b.y<0||b.y>getHeight()) { bi.remove(); continue; }
                    
                    Iterator<Point> zi = zombies.iterator();
                    boolean hit = false;
                    while(zi.hasNext()) {
                        Point z = zi.next();
                        if(Math.abs(b.x - z.x) < 30 && Math.abs(b.y - z.y) < 30) {
                            zi.remove(); hit = true; 
                            score++; 
                            ammo += 2;
                            safeSound("HIT"); break;
                        }
                    }
                    if(hit) bi.remove(); 
                }
                
                if(Math.random() < 0.02 * wave && zombies.size() < 50) {
                    int side = (int)(Math.random()*4);
                    int zx=0, zy=0;
                    if(side==0){zx=(int)(Math.random()*getWidth()); zy=-20;}
                    if(side==1){zx=(int)(Math.random()*getWidth()); zy=getHeight()+20;}
                    if(side==2){zx=-20; zy=(int)(Math.random()*getHeight());}
                    if(side==3){zx=getWidth()+20; zy=(int)(Math.random()*getHeight());}
                    zombies.add(new Point(zx, zy));
                }
                
                for(Point z : zombies) {
                    double ang = Math.atan2(py-z.y, px-z.x);
                    z.x += Math.cos(ang)*2.0; 
                    z.y += Math.sin(ang)*2.0;
                    
                    if(Math.hypot(px-z.x, py-z.y) < 25) {
                        stopGame();
                        JOptionPane.showMessageDialog(this, "GAME OVER!\nYour Score: " + score);
                        stopAllGamesAndShowLobby(); 
                    }
                }
                repaint();
            } catch (Exception ex) { }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // --- DRAW FRIEND (TRIANGLE) ---
            if(isMultiplayer && friendX > -50) {
                g2.setColor(Color.GREEN);
                Polygon fp = new Polygon();
                fp.addPoint((int)(friendX + Math.cos(friendAng)*20), (int)(friendY + Math.sin(friendAng)*20)); // Tip
                fp.addPoint((int)(friendX + Math.cos(friendAng + 2.5)*15), (int)(friendY + Math.sin(friendAng + 2.5)*15)); // Back 1
                fp.addPoint((int)(friendX + Math.cos(friendAng - 2.5)*15), (int)(friendY + Math.sin(friendAng - 2.5)*15)); // Back 2
                g2.fillPolygon(fp);
                g2.drawString("ALLY", (int)friendX-15, (int)friendY-20);
            }

            // --- DRAW PLAYER (TRIANGLE) ---
            g2.setColor(Color.CYAN); 
            double aimAngle = Math.atan2(my-py, mx-px);
            Polygon p = new Polygon();
            p.addPoint((int)(px + Math.cos(aimAngle)*20), (int)(py + Math.sin(aimAngle)*20)); // Tip
            p.addPoint((int)(px + Math.cos(aimAngle + 2.5)*15), (int)(py + Math.sin(aimAngle + 2.5)*15)); // Back 1
            p.addPoint((int)(px + Math.cos(aimAngle - 2.5)*15), (int)(py + Math.sin(aimAngle - 2.5)*15)); // Back 2
            g2.fillPolygon(p);
            
            // Zombies
            g2.setColor(COL_VAL_RED); for(Point z : zombies) g2.fillRect(z.x-12, z.y-12, 24, 24);
            g2.setColor(Color.YELLOW); for(SBullet b : bullets) g2.fillOval((int)b.x-4, (int)b.y-4, 8, 8);
            
            // --- CUSTOM CROSSHAIR ---
            g2.setColor(new Color(0, 255, 0));
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(mx-10, my-10, 20, 20);
            g2.drawLine(mx, my-5, mx, my+5);
            g2.drawLine(mx-5, my, mx+5, my);

            // HUD
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.drawString("KILLS: " + score, 20, 30);
            g2.setColor(ammo > 0 ? Color.GREEN : Color.RED);
            g2.drawString("AMMO: " + ammo, 20, 55);
            
            if(isMultiplayer) {
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.setColor(Color.YELLOW);
                g2.drawString("[MULTIPLAYER ACTIVE]", getWidth()-150, 30);
            }
        }
    }

    // --- PROTOCOL ---
    private void processMessage(String msg) {
        if (msg.startsWith("GAME_START:")) {
            safeSound("WIN"); 
            String check = msg.toUpperCase(); 
            if(check.contains("SPRINT")) { cardLayout.show(mainContainer, "SPRINT"); if(runAnimTimer!=null) runAnimTimer.start(); }
            else if(check.contains("GALACTIC")) { cardLayout.show(mainContainer, "SPACE"); }
            else if(check.contains("SNAKE")) { cardLayout.show(mainContainer, "SNAKE"); }
            chatArea.append(">> " + msg + "\n");
        }
        else if (msg.startsWith("SURV:")) { // --- MULTIPLAYER UPDATE ---
            try {
                String[] parts = msg.split(":");
                double fx = Double.parseDouble(parts[1]);
                double fy = Double.parseDouble(parts[2]);
                double fa = Double.parseDouble(parts[3]);
                boolean fs = parts[4].equals("1");
                survivalPanel.updateFriend(fx, fy, fa, fs);
            } catch(Exception e) {}
        }
        else if (msg.equals("ENEMY_SHOOT") || (msg.startsWith("OBSTACLE:") && !msg.contains("RUN"))) safeSound("SHOOT");
        else if (msg.equals("ENEMY_HIT_CONFIRM") || msg.contains("stumbled") || msg.contains("Skeleton")) safeSound("HIT");
        else if (msg.startsWith("SPRINT_UPDATE:")) safeSound("SELECT");
        else if (msg.startsWith("OBSTACLE:")) sprintCmdLabel.setText(msg.split(":")[1]);
        else if (msg.startsWith("SPRINT_UPDATE:")) { String[] p=msg.split(":"); targetP1X=10+(Integer.parseInt(p[1])*6); targetP2X=10+(Integer.parseInt(p[2])*6); }
        else if (msg.equals("SETUP:LEFT")) { isLeftPlayer=true; myX=50; enemyX=800; }
        else if (msg.equals("SETUP:RIGHT")) { isLeftPlayer=false; myX=800; enemyX=50; }
        else if (msg.startsWith("ENEMY_POS:")) { String[] p=msg.split(":"); enemyX=Integer.parseInt(p[1]); enemyY=Integer.parseInt(p[2]); }
        else if (msg.equals("ENEMY_SHOOT")) enemyBullets.add(new Bullet(enemyX+(isLeftPlayer?-10:40), enemyY+15));
        else if (msg.startsWith("HP:")) { String[] p=msg.split(":"); if(isLeftPlayer){myHP=Integer.parseInt(p[1]); enemyHP=Integer.parseInt(p[2]);}else{myHP=Integer.parseInt(p[2]); enemyHP=Integer.parseInt(p[1]);} }
        else if (msg.equals("ENEMY_HIT_CONFIRM")) createExplosion(enemyX, enemyY);
        else if (msg.startsWith("BOARD:")) {
            String b = msg.substring(6);
            if(b.contains(",")) { 
                String[] c=b.split(","); 
                for(int i=0;i<16;i++) { 
                    String val = c[i];
                    boolean isHidden = val.equals("?");
                    memoryButtons[i].setText(isHidden ? "?" : val);
                    if(isHidden) { memoryButtons[i].setBackground(new Color(40,40,60)); memoryButtons[i].setForeground(Color.WHITE); } 
                    else { memoryButtons[i].setBackground(new Color(255, 140, 0)); memoryButtons[i].setForeground(Color.BLACK); }
                } 
            }
            else for(int i=0;i<9;i++) { char c=b.charAt(i); tttButtons[i].setText(c=='-'?"":c+""); tttButtons[i].setForeground(c=='X'?COL_ACCENT_MAGENTA:COL_ACCENT_CYAN); }
        }
        else if (msg.startsWith("SNAKE:")) snakePanel.updateState(msg.substring(6));
        else if (msg.startsWith("HINT:")) guessHintLabel.setText(msg.substring(5));
        else chatArea.append(msg+"\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void login() { username=JOptionPane.showInputDialog("ENTER AGENT NAME:"); if(username!=null) out.println(username); else System.exit(0); }
    private void startListener() { new Thread(()->{try{while(true)processMessage(in.readLine());}catch(Exception e){}}).start(); }

    public static void main(String[] args) { 
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e){}
        new GameClient(); 
    }
}