package client; // Simple package name

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 5001);
            
            // Listener Thread (Reads messages from server)
            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String msg;
                    while ((msg = in.readLine()) != null) System.out.println(msg);
                } catch (IOException e) {}
            }).start();

            // Sender (Sends your typing to server)
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            Scanner scanner = new Scanner(System.in);
            while (true) out.println(scanner.nextLine());
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}