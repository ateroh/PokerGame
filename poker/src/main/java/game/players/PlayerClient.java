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

/**
 * PlayerClient - base klasse for netværksspillere.
 * Host extender denne klasse.
 */
public class PlayerClient {

    protected String ip = "localhost";
    protected String port;
    protected String uri;
    protected String username;
    protected String id;
    protected String hostId;

    protected SpaceRepository repository;
    protected SequentialSpace playersSpace;  // (id, name, uri, isReady)
    protected SequentialSpace gameSpace;
    protected Space readySpace;

    protected boolean connected = false;

    // Kun brugt af clients (ikke host)
    private String serverUri;
    private RemoteSpace remoteRequestSpace;
    private RemoteSpace remoteReadySpace;
    private RemoteSpace remoteGameSpace;

    public PlayerClient(String serverIp, int serverPort, String username) {
        this.username = username;
        this.port = String.valueOf(9100 + (int)(Math.random() * 900));
        this.uri = formatURI(ip, port);
        this.serverUri = formatURI(serverIp, String.valueOf(serverPort));
    }

    // Constructor til Host
    protected PlayerClient(String username, String port) {
        this.username = username;
        this.port = port;
        this.uri = formatURI(ip, port);
    }

    protected void initSpaces() {
        try {
            playersSpace = new SequentialSpace();
            gameSpace = new SequentialSpace();
            repository = new SpaceRepository();
            repository.add("game", gameSpace);
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
            System.out.println("Sendt join request...");

            Tuple response = new Tuple(remoteRequestSpace.get(
                new FormalField(String.class),
                new ActualField(uri)
            ));

            if (response.getElementAt(String.class, 0).equals("Lobby is full")) {
                System.out.println("Lobby er fuld!");
                disconnect();
                return false;
            }

            Tuple data = new Tuple(remoteRequestSpace.get(
                new ActualField("Helo"),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(LinkedList.class),
                new ActualField(uri)
            ));

            hostId = data.getElementAt(String.class, 1);
            id = data.getElementAt(String.class, 2);
            playersSpace.put(id, username, uri, false);

            @SuppressWarnings("unchecked")
            LinkedList<?> rawPeerList = (LinkedList<?>) data.getElementAt(3);
            for (Object peer : rawPeerList) {
                String peerId, peerName, peerUri;
                if (peer instanceof Object[]) {
                    Object[] arr = (Object[]) peer;
                    peerId = (String) arr[0];
                    peerName = (String) arr[1];
                    peerUri = (String) arr[2];
                } else if (peer instanceof List) {
                    List<?> list = (List<?>) peer;
                    peerId = (String) list.get(0);
                    peerName = (String) list.get(1);
                    peerUri = (String) list.get(2);
                } else {
                    continue;
                }
                playersSpace.put(peerId, peerName, peerUri, false);
            }

            remoteReadySpace = new RemoteSpace(serverUri + "/ready?keep");
            remoteGameSpace = new RemoteSpace(serverUri + "/game?keep");
            readySpace = remoteReadySpace;

            connected = true;
            System.out.println("Forbundet! ID: " + id);
            return true;

        } catch (Exception e) {
            System.err.println("Kunne ikke forbinde: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void sendReadyFlag(boolean isReady) {
        try {
            if (connected && readySpace != null) {
                readySpace.put("ready", id, isReady);
            }
        } catch (InterruptedException e) {
            System.err.println("Ready fejl: " + e.getMessage());
        }
    }

    public List<String> getPlayerNames() {
        List<String> names = new ArrayList<>();
        // For client: hent fra remote gameSpace
        if (remoteGameSpace != null && connected) {
            try {
                Object[] result = remoteGameSpace.queryp(
                    new ActualField("playerlist"),
                    new FormalField(String.class)
                );
                if (result != null) {
                    String playerListStr = (String) result[1];
                    if (!playerListStr.isEmpty()) {
                        for (String name : playerListStr.split(",")) {
                            names.add(name);
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Fallback til lokal
            }
        }
        // Fallback: lokal playersSpace
        if (names.isEmpty()) {
            for (Object[] p : getLocalPlayers()) {
                names.add((String) p[1]);
            }
        }
        return names;
    }

    public List<Object[]> getLocalPlayers() {
        return playersSpace.queryAll(
            new FormalField(String.class),
            new FormalField(String.class),
            new FormalField(String.class),
            new FormalField(Boolean.class)
        );
    }

    public void disconnect() {
        if (!connected) return;

        // Send leave besked til host FØRST mens vi stadig er forbundet
        try {
            if (remoteGameSpace != null) {
                remoteGameSpace.put("leave", id, username);
                System.out.println("Sendt leave besked til host");
            }
        } catch (Exception e) {
            System.err.println("Kunne ikke sende leave: " + e.getMessage());
        }

        connected = false;

        try {
            if (remoteRequestSpace != null) remoteRequestSpace.close();
            if (remoteReadySpace != null) remoteReadySpace.close();
            if (remoteGameSpace != null) remoteGameSpace.close();
        } catch (Exception e) {}
        
        if (repository != null) {
            try {
                repository.closeGate(uri + "/?keep");
                repository.shutDown();
            } catch (Exception e) {}
        }

        System.out.println("Disconnected");
    }

    public String formatURI(String ip, String port) {
        return "tcp://" + ip + ":" + port;
    }

    public int getLobbySize() { 
        return getLocalPlayers().size();
    }
    public Space getGameSpace() { return connected ? remoteGameSpace : gameSpace; }
    public String getUsername() { return username; }
    public String getId() { return id; }
    public String getUri() { return uri; }
    public boolean isConnected() { return connected; }

    // Callback interface for events
    public interface ClientEventListener {
        void onKicked(String reason);
        void onServerShutdown(String reason);
    }

    private ClientEventListener eventListener;

    public void setEventListener(ClientEventListener listener) {
        this.eventListener = listener;
    }

    /**
     * Start lytter for kicked og shutdown beskeder.
     */
    public void startEventListener() {
        new Thread(() -> {
            while (connected && remoteGameSpace != null) {
                try {
                    // Check for kicked besked
                    Object[] kicked = remoteGameSpace.getp(
                        new ActualField("kicked"),
                        new ActualField(id),
                        new FormalField(String.class)
                    );
                    if (kicked != null) {
                        String reason = (String) kicked[2];
                        connected = false;
                        if (eventListener != null) {
                            eventListener.onKicked(reason);
                        }
                        break;
                    }

                    // Check for shutdown besked
                    Object[] shutdown = remoteGameSpace.queryp(
                        new ActualField("shutdown"),
                        new FormalField(String.class)
                    );
                    if (shutdown != null) {
                        String reason = (String) shutdown[1];
                        connected = false;
                        if (eventListener != null) {
                            eventListener.onServerShutdown(reason);
                        }
                        break;
                    }

                    Thread.sleep(500);
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }
}