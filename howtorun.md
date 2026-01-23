# How to Run - Poker Game

This project is a distributed multiplayer Texas Hold'em poker game that uses jSpace for coordination and JavaFX for the user interface.

## Prerequisites
To run the project, you must have the following installed:
- **Java 21** or newer
- **Maven**
- **JavaFX**
- **jSpace Library** (managed via Maven)

## Installation and Build
1. Clone the repository.
2. Go to the `poker` folder:
   ```bash
   cd poker
   ```
3. Build the project with Maven:
   ```bash
   mvn clean compile
   ```

## How to Run the Game

### 1. Run locally on one computer
To test the game locally:
1. Ensure the IP in `PlayerClient.java` (line 28) is set to `localhost`:
   ```java
   protected String ip = "localhost";
   ```
2. Ensure `serverIp` and `serverPort` are commented out in the constructor in `PlayerClient.java` (lines 48-49).
3. Run the command in the `poker` folder:
   ```bash
   mvn javafx:run
   ```

### 2. Play over LAN (multiple computers on the same network)
1. Set the IP in `PlayerClient.java` (line 28) to `0.0.0.0` on the host computer:
   ```java
   protected String ip = "0.0.0.0";
   ```
2. Set `serverIp` in `PlayerClient.java` (line 48) to the host's local IP address (e.g., `192.168.x.x`). Can be found with `ifconfig` (Mac/Linux) or `ipconfig` (Windows).
3. Run the game on all computers:
   ```bash
   mvn javafx:run
   ```

### 3. Play online via Ngrok
The easiest way to play over the internet is by using Ngrok.
1. Install Ngrok.
2. Open a terminal and run:
   ```bash
   ngrok tcp 9001
   ```
3. Update `PlayerClient.java`:
   - Set `serverIp` (line 48) to the address Ngrok provides (e.g., `2.tcp.eu.ngrok.io`).
   - Set `serverPort` (line 49) to the port number Ngrok provides (e.g., `17205`).
   - Set `ip` (line 28) to call `getPublicIp()`:
     ```java
     protected String ip = getPublicIp();
     ```
4. Run the game:
   ```bash
   mvn javafx:run
   ```
The guide has been written in more detail with examples in the @README.md file.
---
**Support:** Contact our 24/7 support at +45 25 36 00 09 in case of issues.
