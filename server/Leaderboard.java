package server;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Leaderboard {
    private static final String FILE_NAME = "scores.txt";
    private static Map<String, Integer> scores = new HashMap<>();

    // Load scores when server starts
    public static void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    scores.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
            System.out.println(">> [SYSTEM] Leaderboard loaded.");
        } catch (IOException e) {
            System.out.println(">> [SYSTEM] No previous scores found. Starting fresh.");
        }
    }

    // Save scores after every win
    public static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                pw.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void addWin(String username) {
        scores.put(username, scores.getOrDefault(username, 0) + 1);
        save(); // Auto-save
    }

    public static String getTopScores() {
        if (scores.isEmpty()) return "NO SCORES YET";
        
        // Sort by Score (Descending) using Streams
        return scores.entrySet().stream()
                .sorted((k1, k2) -> -k1.getValue().compareTo(k2.getValue()))
                .limit(5) // Top 5
                .map(e -> e.getKey() + " (" + e.getValue() + " Wins)")
                .collect(Collectors.joining(", "));
    }
}