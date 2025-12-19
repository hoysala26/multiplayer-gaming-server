# Multiplayer Server

A robust Java-based gaming platform that connects clients to a central server for real-time multiplayer and solo arcade experiences. This project uses Java Swing for the graphical interface and Java Sockets (TCP/IP) for networking.

## 🎮 Game Library

The platform features a "Mega-Lobby" containing the following games:

* **Crimson Survival:** A top-down tactical zombie shooter. Features include:
    * Solo & Multiplayer Co-op modes.
    * Ammo management (scavenge on kill).
    * Custom crosshair and tactical movement.
* **Galactic War:** Fast-paced space shooter with particle effects.
* **Snake:** Classic gameplay with both Practice (Solo) and PvP modes.
* **Cyber Sprint:** A multiplayer racing mini-game.
* **Classic Arcade:** Includes Tic-Tac-Toe, Rock-Paper-Scissors, and Memory Matrix.

## 🛠️ Technology Stack

* **Language:** Java (JDK 8+)
* **Networking:** Java Sockets (ServerSocket & Socket)
* **GUI:** Java Swing (Custom 2D Graphics rendering)
* **Architecture:** Client-Server Model with multi-threading.

## 🚀 How to Run

### 1. Compile the Code
Open your terminal in the `src` folder and run:
```bash
javac server/*.java client/*.java