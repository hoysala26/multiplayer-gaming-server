package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    // Identity
    public String username;
    
    // State Management
    public Game currentGame;          // The actual game object
    public String waitingForGameType = null; // What game is this user waiting for?

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 1. Setup Streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 2. Authentication Protocol
            out.println("SERVER: Enter your username:");
            this.username = in.readLine();
            
            // Basic validation
            if (this.username == null || this.username.trim().isEmpty()) {
                this.username = "User-" + System.currentTimeMillis();
            }
            
            // Register with the Manager
            ClientManager.addClient(this);
            System.out.println(">> [LOGIN] " + username + " connected.");
            ClientManager.broadcast("SERVER: " + username + " has joined the lobby!", this);

            // 3. Main Command Loop
            String input;
            while ((input = in.readLine()) != null) {
                
                // --- COMMAND: PLAY ---
                // Usage: /play snake solo  OR  /play tictactoe
                if (input.startsWith("/play")) {
                    String[] parts = input.split(" ");
                    String gameType = (parts.length > 1) ? parts[1].toLowerCase() : "tictactoe"; 
                    String mode = (parts.length > 2) ? parts[2].toLowerCase() : "pvp";

                    // 1. CHECK FOR SOLO MODES (Skip Matchmaking)
                    if (mode.equals("solo")) {
                        if (gameType.equals("snake")) {
                            Game newGame = new SnakeBattle(this, null); // Null opponent = Bot
                            this.currentGame = newGame;
                            System.out.println(">> [SOLO] Started Snake vs Bot for " + this.username);
                        }
                        else if (gameType.equals("rogue")) {
                            Game newGame = new ShadowRogue(this, null); // Null opponent = Solo Dungeon
                            this.currentGame = newGame;
                            System.out.println(">> [SOLO] Started Shadow Rogue for " + this.username);
                        }
                        else {
                            out.println("SERVER: Solo mode not available for " + gameType);
                        }
                        continue; // Skip the rest of the loop
                    }

                    // 2. CHECK FOR PVP MATCHMAKING
                    // Ask Manager to find an opponent waiting for the SAME game
                    ClientHandler opponent = ClientManager.findOpponent(this, gameType);
                    
                    if (opponent != null) {
                        // FOUND A MATCH!
                        Game newGame;
                        
                        // --- FACTORY: Create the specific game ---
                        if (gameType.equals("rps")) {
                            newGame = new RockPaperScissors(this, opponent);
                        } 
                        else if (gameType.equals("guess")) {
                            newGame = new GuessNumber(this, opponent);
                        } 
                        else if (gameType.equals("memory")) { 
                            newGame = new MemoryGame(this, opponent);
                        }
                        else if (gameType.equals("sprint")) {
                            newGame = new CyberSprint(this, opponent);
                        }
                        else if (gameType.equals("space")) { 
                            newGame = new GalacticWar(this, opponent);
                        }
                        else if (gameType.equals("snake")) {
                            newGame = new SnakeBattle(this, opponent);
                        }
                        else if (gameType.equals("rogue")) {
                            newGame = new ShadowRogue(this, opponent);
                        }
                        else {
                            // Default is TicTacToe
                            newGame = new TicTacToe(this, opponent);
                        }
                        
                        // Assign the game to both players
                        this.currentGame = newGame;
                        opponent.currentGame = newGame;
                        
                        System.out.println(">> [MATCH] Started " + gameType + ": " + this.username + " vs " + opponent.username);
                    } else {
                        // NO MATCH YET -> WAIT
                        this.waitingForGameType = gameType; 
                        out.println("SERVER: Waiting for an opponent for " + gameType + "...");
                    }
                }
                
                // --- COMMAND: MOVE ---
                // Usage: /move 4  OR  /move UP
                else if (input.startsWith("/move ") && currentGame != null) {
                    try {
                        String moveCommand = input.substring(6).trim(); 
                        currentGame.makeMove(username, moveCommand);
                    } catch (Exception e) {
                        out.println("INVALID: Error processing move.");
                    }
                }

                // --- COMMAND: LEADERBOARD ---
                else if (input.equalsIgnoreCase("/leaderboard")) {
                    out.println("SERVER: TOP PLAYERS: " + Leaderboard.getTopScores());
                }
                
                // --- COMMAND: CHAT ---
                else {
                    ClientManager.broadcast(username + ": " + input, this);
                }
            }
        } catch (IOException e) {
            System.out.println(">> [DISCONNECT] " + username + " lost connection.");
        } finally {
            ClientManager.removeClient(this);
            try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }
}