package game.model;

import java.util.LinkedList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.SequentialSpace;
import org.jspace.Space;

/**
 * Håndterer lobby-logik: spillere der joiner, forlader, bliver kicked.
 */
public class LobbyManager {

    public static final int MAX_LOBBY_SIZE = 4;

    private final SequentialSpace playersSpace;
    private final SequentialSpace requestSpace;
    private final SequentialSpace lockSpace;
    private final Space gameSpace;
    private final GameModel game;
    private final String hostId;

    private int idTracker = 1; // Host har ID 0
    private volatile boolean running = true;

    public LobbyManager(SequentialSpace playersSpace, SequentialSpace requestSpace,
                        SequentialSpace lockSpace, Space gameSpace, GameModel game, String hostId) {
        this.playersSpace = playersSpace;
        this.requestSpace = requestSpace;
        this.lockSpace = lockSpace;
        this.gameSpace = gameSpace;
        this.game = game;
        this.hostId = hostId;
    }

    /** Start lytning efter join-requests */
    public void startJoinListener() {
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
                    requestSpace.put("Helo", hostId, newPlayerId, currentPlayers, playerUri);
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

    /** Start lytning efter leave-requests */
    public void startLeaveListener() {
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

                    playersSpace.getp(
                        new ActualField(playerId),
                        new FormalField(String.class),
                        new FormalField(String.class),
                        new FormalField(Boolean.class)
                    );

                    System.out.println("Spiller forlod: " + playerName + " (" + getLobbySize() + " spillere)");
                    broadcastPlayerList();
                } catch (InterruptedException e) {
                    if (running) e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    /** Kick en spiller */
    public void kickPlayer(String playerId) {
        if (playerId.equals("0")) return; // Kan ikke kicke host
        try {
            Object[] kicked = playersSpace.getp(
                new ActualField(playerId),
                new FormalField(String.class),
                new FormalField(String.class),
                new FormalField(Boolean.class)
            );
            if (kicked != null) {
                String playerName = (String) kicked[1];
                gameSpace.put("kicked", playerId, "Du blev smidt ud af hosten");
                System.out.println("Kicked: " + playerName);
                broadcastPlayerList();
            }
        } catch (InterruptedException e) {
            System.err.println("Kick fejl: " + e.getMessage());
        }
    }

    /** Broadcast spillerliste til alle */
    public void broadcastPlayerList() {
        try {
            gameSpace.getp(new ActualField("playerlist"), new FormalField(String.class));
            StringBuilder sb = new StringBuilder();
            List<Object[]> players = playersSpace.queryAll(
                new FormalField(String.class), new FormalField(String.class),
                new FormalField(String.class), new FormalField(Boolean.class)
            );
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append((String) players.get(i)[1]);
            }
            gameSpace.put("playerlist", sb.toString());
        } catch (InterruptedException e) {}
    }

    public String generateNewPlayerId() { return String.valueOf(idTracker++); }
    public boolean isLobbyFull() { return getLobbySize() >= MAX_LOBBY_SIZE; }
    public int getLobbySize() {
        return playersSpace.queryAll(
            new FormalField(String.class), new FormalField(String.class),
            new FormalField(String.class), new FormalField(Boolean.class)
        ).size();
    }

    public void shutdown() { running = false; }
}
