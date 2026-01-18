package game.model;

import java.util.ArrayList;
import java.util.List;



import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

import game.controller.TableController;
import game.players.Host;
import game.players.PlayerClient;
import javafx.application.Platform;

/**
 * Model for poker-bordet.
 * Håndterer al forretningslogik og Space-kommunikation.
 */
public class TableModel {

    private Host host;
    private PlayerClient client;
    private volatile boolean running = true;
    private Thread updateThread;
    private Thread cardThread;
    private Thread turnThread;
    private Thread stateThread;
    private boolean isReady = false;

    private TableController controller;

    public TableModel(Host host, PlayerClient client, TableController controller) {
        this.host = host;
        this.client = client;
        this.controller = controller;
        setupClientEventListener();
    }

    public static class PlayerInfo {
        public final String id, name;
        public final int chips;
        public final boolean isMe, isHost, isReady;

        public PlayerInfo(String id, int chips, String name, boolean isMe, boolean isHost, boolean isReady) {
            this.id = id; this.chips = chips; this.name = name;
            this.isMe = isMe; this.isHost = isHost; this.isReady = isReady;
        }
    }

    private void setupClientEventListener() {
        if (client != null && host == null) {
            client.startEventListener(controller);
        }
    }

    public void startPlayerListUpdater() {
        updateThread = new Thread(() -> {
            while (running) {
                try {
                    List<PlayerInfo> players = getPlayerInfoList();
                    String status = getStatusText(players.size());

                    Platform.runLater(() -> {
                        if (controller != null) {
                            controller.updatePlayerSlots(players);
                            controller.updateStatus(status);
                        }
                    });

                    Thread.sleep(host != null ? 1000 : 500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private List<PlayerInfo> getPlayerInfoList() {
        List<PlayerInfo> result = new ArrayList<>();
        String myName = getMyName();

        if (host != null) {
            for (Object[] p : host.getLocalPlayers()) {
                String id = (String) p[0];
                String name = (String) p[1];
                boolean isReady = (Boolean) p[3];
                boolean isMe = name.equals(myName);
                boolean isHostPlayer = id.equals("0");

                int chips = 500; 
                if (host.getGame() != null) {
                    var player = host.getGame().getPlayer(name);
                    if (player != null) chips = player.getChips();
                }
                result.add(new PlayerInfo(id, chips, name, isMe, isHostPlayer, isReady));
            }
        } else if (client != null) {
            List<String> names = client.getPlayerNames();
            for (int i = 0; i < names.size(); i++) {
                String name = names.get(i);
                boolean isMe = name.equals(myName);
                boolean isHostPlayer = (i == 0);
                int chips = 500;
                try {
                    Space gameSpace = client.getGameSpace();
                    if (gameSpace != null) {
                        Object[] chipInfo = gameSpace.queryp(
                            new ActualField("playerChips"),
                            new ActualField(name),
                            new FormalField(Integer.class)
                        );
                        if (chipInfo != null) chips = (Integer) chipInfo[2];
                    }
                } catch (Exception e) {}
                result.add(new PlayerInfo(String.valueOf(i), chips, name, isMe, isHostPlayer, false));
            }
        }
        return result;
    }

    public void startGame() {
        if (host == null) return;
        new Thread(() -> {
            try { host.getGame().playCompleteHand(); } catch (InterruptedException e) {}
        }).start();
    }

    private String getStatusText(int playerCount) {
        if (host != null) return "Spillere: " + playerCount + "/" + Host.MAX_LOBBY_SIZE;
        return "Forbundet som: " + getMyName() + " (" + playerCount + " spillere)";
    }

    public String getMyName() {
        if (host != null) return host.getUsername();
        if (client != null) return client.getUsername();
        return "Unknown";
    }

    public String getMyId() {
        if (host != null) return host.getId();
        if (client != null) return client.getId();
        return null;
    }

    public boolean isHost() { return host != null; }

    public void toggleReady() {
        isReady = !isReady;
        if (host != null) host.sendReadyFlag(isReady);
        else if (client != null) client.sendReadyFlag(isReady);
    }

    public boolean isReady() { return isReady; }

    public void kickPlayer(String playerId) {
        if (host != null && !playerId.equals("0")) host.kickPlayer(playerId);
    }

    public void leave() {
        running = false;
        if (host != null) host.stop();
        if (client != null) client.disconnect();
    }

    public void shutdown() {
        running = false;
        if (updateThread != null) updateThread.interrupt();
        if (cardThread != null) cardThread.interrupt();
        if (turnThread != null) turnThread.interrupt();
        if (stateThread != null) stateThread.interrupt();
    }


    public void startCardListener() {
        cardThread = new Thread(() -> {
            try {
                Space gs = getGameSpace();
                while (running && gs == null) { gs = getGameSpace(); Thread.sleep(50); }
                if (!running) return;

                Object[] cards = gs.get(
                    new ActualField("dealtCards"), new ActualField(getMyName()),
                    new FormalField(String.class), new FormalField(String.class),
                    new FormalField(String.class), new FormalField(String.class)
                );
                if (!running) return;

                String file1 = cards[3] + "_of_" + cards[2] + ".png";
                String file2 = cards[5] + "_of_" + cards[4] + ".png";
                Platform.runLater(() -> { if (controller != null) controller.displayCards(file1, file2); });
            } catch (InterruptedException e) {}
        });
        cardThread.setDaemon(true);
        cardThread.start();
    }

    /** Start lytning efter "yourTurn" beskeder */
    public void startTurnListener() {
        turnThread = new Thread(() -> {
            while (running) {
                try {
                    Space gs = getGameSpace();
                    if (gs == null) { Thread.sleep(100); continue; }

                    Object[] t = gs.get(
                        new ActualField("yourTurn"), new ActualField(getMyName()),
                        new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class)
                    );
                    if (t != null) {
                        int[] info = {(Integer)t[2], (Integer)t[3], (Integer)t[4]};
                        Platform.runLater(() -> { if (controller != null) controller.handleMyTurn(info[0], info[1], info[2]); });
                    }
                    Thread.sleep(200);
                } catch (InterruptedException e) { break; }
            }
        });
        turnThread.setDaemon(true);
        turnThread.start();
    }

    /** Start lytning efter playerAction beskeder */
    public void startStateListener() {
        stateThread = new Thread(() -> {
            while (running) {
                try {
                    Space gs = getGameSpace();
                    if (gs == null) { Thread.sleep(100); continue; }

                    Object[] a = gs.getp(
                        new ActualField("playerAction"),
                        new FormalField(String.class), new FormalField(String.class),
                        new FormalField(Integer.class), new FormalField(Integer.class), new FormalField(Integer.class)
                    );
                    if (a != null) {
                        Object[] info = {a[1], a[4], a[5]}; // playerName, chipsLeft, pot
                        Platform.runLater(() -> { 
                            if (controller != null) controller.handlePlayerAction((String)info[0], (Integer)info[1], (Integer)info[2]); 
                        });
                    }
                    Thread.sleep(50);
                } catch (InterruptedException e) { break; }
            }
        });
        stateThread.setDaemon(true);
        stateThread.start();
    }

    /** Send en action til gameSpace */
    public void sendAction(String action, int amount) {
        new Thread(() -> {
            try {
                Space gs = getGameSpace();
                if (gs != null) gs.put("action", getMyName(), action, amount);
            } catch (InterruptedException e) {}
        }).start();
    }

    private Space getGameSpace() {
        if (host != null) return host.getGameSpace();
        if (client != null) return client.getGameSpace();
        return null;
    }
    
    public ChatManager getChatManager() {
        if (host != null) return host.getChatManager();
        return client.getChatManager();
    }
}