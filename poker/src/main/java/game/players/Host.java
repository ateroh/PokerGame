package game.players;

import org.jspace.RandomSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;

import game.model.DeckModel;
import game.model.GameModel;
import game.model.LobbyManager;

/**
 * Host - server der extender PlayerClient.
 */
public class Host extends PlayerClient {

    public static final int MAX_LOBBY_SIZE = 4;

    private SequentialSpace requestSpace;
    private SequentialSpace lockSpace;
    private RandomSpace deckSpace;

    private DeckModel deck;
    private GameModel game;
    private LobbyManager lobbyManager;
    private boolean running = false;

    public Host(int port, String username) {
        super(username, String.valueOf(port));
        this.id = "0";
        this.hostId = this.id;
    }

    public void start() throws Exception {
        initSpaces();
        playersSpace.put(id, username, uri, false);
        lockSpace.put("loginLock");
        initModels();
        running = true;
        connected = true;
        
        chatManager.startMessageReceiver();
        
        System.out.println("Server startet på " + uri + " - Host: " + username);
        lobbyManager.startJoinListener();
        lobbyManager.startLeaveListener();
    }

    @Override
    protected void initSpaces() {
        super.initSpaces();
        try {
            requestSpace = new SequentialSpace();
            readySpace = new SequentialSpace();
            lockSpace = new SequentialSpace();
            deckSpace = new RandomSpace();

            repository.add("requests", requestSpace);
            repository.add("ready", readySpace);
            repository.add("deck", deckSpace);
        } catch (Exception e) {
            System.err.println("initSpaces fejl: " + e.getMessage());
        }
    }

    private void initModels() throws InterruptedException {
        deck = new DeckModel(deckSpace);
        deck.initialize();
        game = new GameModel(gameSpace, deck);
        game.addPlayer(username);
        lobbyManager = new LobbyManager(playersSpace, requestSpace, lockSpace, gameSpace, game, id);
    }

    public void kickPlayer(String playerId) {
        lobbyManager.kickPlayer(playerId);
    }

    public void stop() {
        running = false;
        connected = false;
        if (lobbyManager != null) lobbyManager.shutdown();

        try {
            gameSpace.put("shutdown", "Host har lukket serveren");
            Thread.sleep(200);
        } catch (InterruptedException e) {}

        if (repository != null) {
            try {
                repository.closeGate(uri + "/?keep");
                repository.shutDown();
            } catch (Exception e) {}
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

    @Override
    public Space getGameSpace() { return gameSpace; }
}