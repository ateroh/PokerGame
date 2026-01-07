package game.players;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import java.util.ArrayList;
import java.util.List;

public class Host {
    
    private int port;
    private String username;
    private SpaceRepository repository;
    private Space gameSpace;
    private List<String> players = new ArrayList<>();
    private Thread listenerThread;
    private boolean running = false;

    public Host(int port, String username) {
        this.port = port;
        this.username = username;
    }

    public void start() throws Exception {
        // Opret repository
        String uri = "tcp://localhost:" + port + "/?conn";

        repository = new SpaceRepository();
        repository.addGate(uri);

        // Opret game space
        gameSpace = new SequentialSpace();
        repository.add("game", gameSpace);

        // Registrer host som spiller
        gameSpace.put("player", username, "host");
        players.add(username);

        System.out.println("Server started on port " + port);
        System.out.println("Host player: " + username);

        // Start listener for nye spillere
        running = true;
        listenerThread = new Thread(this::listenForPlayers);
        listenerThread.start();
    }

    private void listenForPlayers() {
        System.out.println("Listener startet - venter på spillere...");
        while (running) {
            try {
                // Vent på en ny client spiller (blokerende kald)
                Object[] player = gameSpace.get(
                    new ActualField("newplayer"),
                    new FormalField(String.class)
                );

                String playerName = (String) player[1];
                players.add(playerName);

                System.out.println("\n>>> Ny spiller joined: " + playerName);
                System.out.println(">>> Antal spillere: " + players.size());
                System.out.println(">>> Spillere: " + players);

            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public List<String> getPlayers() {
        return players;
    }

    public Space getGameSpace() {
        return gameSpace;
    }

    public String getUsername() {
        return username;
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (repository != null) {
            repository.closeGate("tcp://localhost:" + port + "/?conn");
        }
    }
}
