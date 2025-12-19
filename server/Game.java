package server;

public interface Game {
    void makeMove(String player, String move);
    boolean isGameOver();
}