package game.players;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RandomSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import game.model.DeckModel;
import game.model.GameModel;

/**
 * Host - server der extender PlayerClient.
 * Ligesom MasterPeer extends Peer.
 */
public class Host extends PlayerClient {

    public static final int MAX_LOBBY_SIZE = 4;

    private SequentialSpace requestSpace;
    private SequentialSpace lockSpace;
    private RandomSpace deckSpace;

    private DeckModel deck;
    private GameModel game;
    private int idTracker = 0;
    private boolean running = false;

    public Host(int port, String username) {
        super(username, String.valueOf(port));
        this.id = generateNewPlayerId();
        this.hostId = this.id;
    }

    public void start() throws Exception {
        initSpaces();
        playersSpace.put(id, username, uri, false);
        lockSpace.put("loginLock");
        initModels();
        running = true;
        connected = true;
        System.out.println("Server startet på " + uri + " - Host: " + username);
        awaitLobbyRequest();
        awaitLeaveRequests();
    }

    @Override
    protected void initSpaces() {
        try {
            repository = new SpaceRepository();
            playersSpace = new SequentialSpace();
            gameSpace = new SequentialSpace();
            requestSpace = new SequentialSpace();
            readySpace = new SequentialSpace();
            lockSpace = new SequentialSpace();
            deckSpace = new RandomSpace();

            repository.add("game", gameSpace);
            repository.add("requests", requestSpace);
            repository.add("ready", readySpace);
            repository.add("deck", deckSpace);
            repository.addGate(uri + "/?keep");
        } catch (Exception e) {
            System.err.println("initSpaces fejl: " + e.getMessage());
        }
    }

    private void initModels() throws InterruptedException {
        deck = new DeckModel(deckSpace);
        deck.initialize();
        game = new GameModel(gameSpace, deck);  
        game.addPlayer(username);
    }

    public void awaitLobbyRequest() {
        new Thread(() -> {
            while (running) {
                try {
                    lockSpace.get(new ActualField("loginLock"));
                    String newPlayerId = generateNewPlayerId();
                    lockSpace.put("awaiting_" + newPlayerId);

                    Object[] request = requestSpace.get(
                        new ActualField("Helo"),
                        new FormalField(String.class),
                        new FormalField(String.class)
                    );
                    String playerName = (String) request[1];
                    String playerUri = (String) request[2];

                    if (isLobbyFull()) {
                        requestSpace.put("Lobby is full", playerUri);
                        lockSpace.put("loginLock");
                        continue;
                    }

                    LinkedList<Object[]> currentPlayers = new LinkedList<>(playersSpace.queryAll(
                        new FormalField(String.class), new FormalField(String.class),
                        new FormalField(String.class), new FormalField(Boolean.class)
                    ));

                    requestSpace.put("Approved", playerUri);
                    requestSpace.put("Helo", this.id, newPlayerId, currentPlayers, playerUri);
                    playersSpace.put(newPlayerId, playerName, playerUri, false);
                    game.addPlayer(playerName);

                    System.out.println(">>> Ny spiller: " + playerName + " (" + getLobbySize() + " spillere)");
                    broadcastPlayerList();
                    lockSpace.put("loginLock");
                } catch (InterruptedException e) {
                    if (running) e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    /**
     * Lytter efter spillere der forlader.
     */
    public void awaitLeaveRequests() {
        new Thread(() -> {
            while (running) {
                try {
                    Object[] leave = gameSpace.get(
                        new ActualField("leave"),
                        new FormalField(String.class),
                        new FormalField(String.class)
                    );
                    String playerId = (String) leave[1];
                    String playerName = (String) leave[2];

                    // Fjern spiller fra playersSpace
                    playersSpace.getp(
                        new ActualField(playerId),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                    );

                    System.out.println("<<< Spiller forlod: " + playerName + " (" + getLobbySize() + " spillere)");
                    broadcastPlayerList();
                } catch (InterruptedException e) {
                    if (running) e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    public void awaitReadyFlags() {
        new Thread(() -> {
            try {
                HashMap<String, Boolean> readyMap = new HashMap<>();
                while (running) {
                    Object[] info = readySpace.get(
                        new ActualField("ready"),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                    );
                    readyMap.put((String) info[1], (Boolean) info[2]);

                    if (isAllReady(readyMap) && getLobbySize() >= 2) {
                        gameSpace.put("gamestart", true);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                if (running) e.printStackTrace();
            }
        }).start();
    }

    private boolean isAllReady(HashMap<String, Boolean> readyMap) {
        if (readyMap.size() < getLobbySize()) return false;
        for (Boolean ready : readyMap.values()) {
            if (!ready) return false;
        }
        return true;
    }

    private void broadcastPlayerList() {
        try {
            gameSpace.getp(new ActualField("playerlist"), new FormalField(String.class));
            StringBuilder sb = new StringBuilder();
            var players = playersSpace.queryAll(
                new FormalField(String.class), new FormalField(String.class),
                new FormalField(String.class), new FormalField(Boolean.class)
            );
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append((String) players.get(i)[1]);
            }
            gameSpace.put("playerlist", sb.toString());
        } catch (InterruptedException e) {
        }
    }
    
    public String generateNewPlayerId() { return String.valueOf(idTracker++); }
    public boolean isLobbyFull() { return getLobbySize() >= MAX_LOBBY_SIZE; }

    /**
     * Smid en spiller ud af lobbyen.
     */
    public void kickPlayer(String playerId) {
        try {
            Object[] kicked = playersSpace.getp(
                new ActualField(playerId),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(Boolean.class)
            );
            if (kicked != null) {
                String playerName = (String) kicked[1];
                // Send kick besked til spilleren
                gameSpace.put("kicked", playerId, "Du blev smidt ud af hosten");
                System.out.println("Kicked: " + playerName);
                broadcastPlayerList();
            }
        } catch (InterruptedException e) {
            System.err.println("Kick fejl: " + e.getMessage());
        }
    }

    /**
     * Stop serveren og send shutdown til alle spillere.
     */
    public void stop() {
        running = false;
        connected = false;

        // Broadcast shutdown til alle spillere
        try {
            gameSpace.put("shutdown", "Host har lukket serveren");
        } catch (InterruptedException e) {
            System.err.println("Shutdown broadcast fejl: " + e.getMessage());
        }

        // Luk gate
        if (repository != null) {
            try {
                repository.closeGate(uri + "/?keep");
            } catch (Exception e) {
                // Ignore
            }
        }
        System.out.println("Server lukket");
    }

    @Override
    public void sendReadyFlag(boolean isReady) {
        try { readySpace.put("ready", id, isReady); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    public Space getDeckSpace() { return deckSpace; }
    public DeckModel getDeck() { return deck; }
    public GameModel getGame() { return game; }
    public int getPort() { return Integer.parseInt(port); }

    /**
     * Hent alle spillere med deres ID (til kick-funktion).
     */
    public List<Object[]> getPlayersWithIds() {
        return playersSpace.queryAll(
            new FormalField(String.class), new FormalField(String.class),
            new FormalField(String.class), new FormalField(Boolean.class)
        );
    }
}
