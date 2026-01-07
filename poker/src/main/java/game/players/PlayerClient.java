package game.players;

import org.jspace.RemoteSpace;
import org.jspace.Space;

import java.io.IOException;

/**
 * PlayerClient repræsenterer en spiller der forbinder til en eksisterende server.
 * Bruger jSpace RemoteSpace til at kommunikere med Host's tuple space over netværk.
 */
public class PlayerClient {
    
    // IP-adresse eller hostname på serveren (f.eks. "localhost" eller "192.168.1.5")
    private String host;

    // Port som serveren lytter på
    private int port;

    // Spillerens valgte brugernavn
    private String username;

    // Reference til det delte tuple space på serveren
    private Space gameSpace;

    public PlayerClient(String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
    }

    /**
     * Forbinder til serverens tuple space og registrerer spilleren.
     * URI formatet er: tcp://host:port/spacename?conn
     * - "game" er navnet på det space som Host har oprettet
     * - "?conn" angiver connection-protokollen
     */
    public void connect() throws IOException, InterruptedException {
        // Opbyg URI til serverens game space
        String uri = "tcp://" + host + ":" + port + "/game?conn";

        // Opret forbindelse til remote space
        gameSpace = new RemoteSpace(uri);

        // Send tuple til serveren for at registrere at vi er joined
        // Host lytter efter denne tuple og tilføjer os til spillerlisten
        gameSpace.put("newplayer", username);

        System.out.println("Connected to server at " + host + ":" + port);
        System.out.println("Registered as: " + username);
    }


    public Space getGameSpace() {
        return gameSpace;
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void disconnect() {
        gameSpace = null;
    }
}
