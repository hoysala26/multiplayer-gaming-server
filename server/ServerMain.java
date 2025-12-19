package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int PORT = 5000;
    // Thread pool to handle up to 20 players at once
    private static ExecutorService pool = Executors.newFixedThreadPool(20);

    public static void main(String[] args) {
        System.out.println(">> [SYSTEM] Server Starting on Port " + PORT);

        // --- NEW: Load Persistent Scores from File ---
        Leaderboard.load(); 
        // ---------------------------------------------

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println(">> [SYSTEM] Waiting for connections...");

            while (true) {
                // 1. Accept new connection (Blocks until someone joins)
                Socket clientSocket = serverSocket.accept();
                
                // 2. Create a worker (ClientHandler) for this specific player
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                
                // 3. Hand it off to a background thread so the Main loop can keep listening
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            System.out.println(">> [ERROR] Could not start server: " + e.getMessage());
        }
    }
}