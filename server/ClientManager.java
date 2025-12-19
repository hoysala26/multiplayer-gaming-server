package server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientManager {
    // Thread-safe list to store all connected players
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    // Add a player to the list when they connect
    public static void addClient(ClientHandler client) {
        clients.add(client);
    }

    // Remove a player when they disconnect
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    // Broadcast a message to everyone (Global Chat)
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // Don't echo the message back to the person who sent it
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Matchmaking Logic:
     * Finds an opponent who is:
     * 1. NOT the person asking (requestor)
     * 2. NOT currently in a game
     * 3. Waiting for the SAME game type (e.g., both want "rps")
     */
    public static ClientHandler findOpponent(ClientHandler requestor, String gameType) {
        for (ClientHandler client : clients) {
            if (client != requestor 
                && client.currentGame == null 
                && gameType.equalsIgnoreCase(client.waitingForGameType)) {
                
                // Match Found!
                
                // Clear the waiting status for both
                client.waitingForGameType = null;
                requestor.waitingForGameType = null;
                
                return client;
            }
        }
        return null; // No match found yet
    }
}