package game.players;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import org.jspace.Tuple;

import game.model.ChatManager;
import game.model.ClientEventMonitor;

/**
 * PlayerClient - base klasse for netværksspillere.
 */
public class PlayerClient {

    protected String ip = "localhost";
    protected String port;
    protected String uri;
    protected String username;
    protected String id;
    protected String hostId;

    protected SpaceRepository repository;
    protected SequentialSpace playersSpace;
    protected SequentialSpace gameSpace;
    protected Space readySpace;
    protected ChatManager chatManager;
    protected boolean connected = false;

    private String serverUri;
    private RemoteSpace remoteRequestSpace;
    private RemoteSpace remoteReadySpace;
    private RemoteSpace remoteGameSpace;
    private ClientEventMonitor eventMonitor;

    public PlayerClient(String serverIp, int serverPort, String username) {
        this.username = username;
        this.port = String.valueOf(9100 + (int)(Math.random() * 900));
        this.uri = formatURI(ip, port);
        this.serverUri = formatURI(serverIp, String.valueOf(serverPort));
        this.chatManager = new ChatManager(this);
    }

    protected PlayerClient(String username, String port) {
        this.username = username;
        this.port = port;
        this.uri = formatURI(ip, port);
        this.chatManager = new ChatManager(this);
    }

    protected void initSpaces() {
        try {
            playersSpace = new SequentialSpace();
            gameSpace = new SequentialSpace();
            repository = new SpaceRepository();
            repository.add("game", gameSpace);
            repository.add("chat", chatManager.getChat());
            repository.addGate(uri + "/?keep");
        } catch (Exception e) {
            System.err.println("initSpaces fejl: " + e.getMessage());
        }
    }

    public boolean connect() {
        try {
            initSpaces();
            remoteRequestSpace = new RemoteSpace(serverUri + "/requests?keep");
            remoteRequestSpace.put("Helo", username, uri);

            Tuple response = new Tuple(remoteRequestSpace.get(
                new FormalField(String.class), new ActualField(uri)));

            if (response.getElementAt(String.class, 0).equals("Lobby is full")) {
                disconnect();
                return false;
            }

            Tuple data = new Tuple(remoteRequestSpace.get(
                new ActualField("Helo"), new FormalField(String.class),
                new FormalField(String.class), new FormalField(LinkedList.class), new ActualField(uri)));

            hostId = data.getElementAt(String.class, 1);
            id = data.getElementAt(String.class, 2);
            playersSpace.put(id, username, uri, false);

            @SuppressWarnings("unchecked")
            LinkedList<?> rawPeerList = (LinkedList<?>) data.getElementAt(3);
            for (Object peer : rawPeerList) {
                String peerId, peerName, peerUri;
                if (peer instanceof Object[]) {
                    Object[] arr = (Object[]) peer;
                    peerId = (String) arr[0]; peerName = (String) arr[1]; peerUri = (String) arr[2];
                } else if (peer instanceof List) {
                    List<?> list = (List<?>) peer;
                    peerId = (String) list.get(0); peerName = (String) list.get(1); peerUri = (String) list.get(2);
                } else continue;
                playersSpace.put(peerId, peerName, peerUri, false);
            }

            remoteReadySpace = new RemoteSpace(serverUri + "/ready?keep");
            remoteGameSpace = new RemoteSpace(serverUri + "/game?keep");
            readySpace = remoteReadySpace;
            connected = true;

            chatManager.startMessageReceiver();
            return true;
        } catch (Exception e) {
            System.err.println("Kunne ikke forbinde: " + e.getMessage());
            return false;
        }
    }

    public void startEventListener(Runnable onKicked, Runnable onServerShutdown) {
        eventMonitor = new ClientEventMonitor(remoteGameSpace, id);
        eventMonitor.setOnKicked(reason -> { connected = false; onKicked.run(); });
        eventMonitor.setOnServerShutdown(reason -> { connected = false; onServerShutdown.run(); });
        eventMonitor.start();
    }

    public void sendReadyFlag(boolean isReady) {
        try {
            if (connected && readySpace != null) readySpace.put("ready", id, isReady);
        } catch (InterruptedException e) {}
    }

    public List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
        if (remoteGameSpace != null && connected) {
            try {
                Object[] result = remoteGameSpace.queryp(
                    new ActualField("playerlist"), new FormalField(String.class));
                if (result != null) {
                    String playerListStr = (String) result[1];
                    if (!playerListStr.isEmpty()) {
                        for (String name : playerListStr.split(",")) names.add(name);
                    }
                }
            } catch (InterruptedException e) {}
        }
        if (names.isEmpty()) {
            for (Object[] p : getLocalPlayers()) names.add((String) p[1]);
        }
        return names;
    }

    public List<Object[]> getLocalPlayers() {
        return playersSpace.queryAll(
            new FormalField(String.class), new FormalField(String.class),
            new FormalField(String.class), new FormalField(Boolean.class));
    }

    public void disconnect() {
        if (!connected) return;
        try {
            if (remoteGameSpace != null) remoteGameSpace.put("leave", id, username);
        } catch (Exception e) {}

        connected = false;
        if (eventMonitor != null) eventMonitor.stop();

        try {
            if (remoteRequestSpace != null) remoteRequestSpace.close();
            if (remoteReadySpace != null) remoteReadySpace.close();
            if (remoteGameSpace != null) remoteGameSpace.close();
        } catch (Exception e) {}
        
        if (repository != null) {
            try { repository.closeGate(uri + "/?keep"); repository.shutDown(); } catch (Exception e) {}
        }
    }

    public String formatURI(String ip, String port) { return "tcp://" + ip + ":" + port; }
    public int getLobbySize() { return getLocalPlayers().size(); }
    public Space getGameSpace() { return connected ? remoteGameSpace : gameSpace; }
    public String getUsername() { return username; }
    public String getId() { return id; }
    public String getUri() { return uri; }
    public boolean isConnected() { return connected; }
    public ChatManager getChatManager() { return chatManager; }
}