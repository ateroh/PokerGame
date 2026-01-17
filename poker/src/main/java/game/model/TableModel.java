package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspace.Space;

import game.chat.ChatManager;
import game.players.Host;
import game.players.PlayerClient;
import javafx.application.Platform;

/**
 * Model for poker-bordet.
 * Håndterer al logik for spilleropdatering, ready-status, kick osv.
 */
public class TableModel {

    private Host host;
    private PlayerClient client;
    private boolean running = true;
    private Thread updateThread;
    private boolean isReady = false;

    // Callbacks til view
    private Consumer<List<PlayerInfo>> onPlayersUpdated;
    private Consumer<String> onStatusUpdated;
    private Runnable onKicked;
    private Runnable onServerShutdown;

    public TableModel(Host host, PlayerClient client) {
        this.host = host;
        this.client = client;
        setupClientEventListener();
    }

    /**
     * Player info klasse til view.
     */
    public static class PlayerInfo {
        public final String id;
        public final int chips;
        public final String name;
        public final boolean isMe;
        public final boolean isHost;
        public final boolean isReady;

        public PlayerInfo(String id, int chips, String name, boolean isMe, boolean isHost, boolean isReady) {
            this.id = id;
            this.chips = chips;
            this.name = name;
            this.isMe = isMe;
            this.isHost = isHost;
            this.isReady = isReady;
        }
    }

    /**
     * Sætter event listener op for client.
     */
    private void setupClientEventListener() {
        if (client != null && host == null) {
            client.setEventListener(new PlayerClient.ClientEventListener() {
                @Override
                public void onKicked(String reason) {
                    Platform.runLater(() -> {
                        if (onKicked != null) onKicked.run();
                    });
                }

                @Override
                public void onServerShutdown(String reason) {
                    Platform.runLater(() -> {
                        if (onServerShutdown != null) onServerShutdown.run();
                    });
                }
            });
            client.startEventListener();
        }
    }

    /**
     * Starter opdatering af spillerlisten.
     */
    public void startPlayerListUpdater() {
        updateThread = new Thread(() -> {
            while (running) {
                try {
                    List<PlayerInfo> players = getPlayerInfoList();
                    String status = getStatusText(players.size());

                    Platform.runLater(() -> {
                        if (onPlayersUpdated != null) onPlayersUpdated.accept(players);
                        if (onStatusUpdated != null) onStatusUpdated.accept(status);
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

    /**
     * Henter spillerliste med info.
     */
    private List<PlayerInfo> getPlayerInfoList() {
        List<PlayerInfo> result = new ArrayList<>();
        String myName = getMyName();
        String myId = getMyId();

        if (host != null) {
            // Host: hent fra playersSpace med IDs
            for (Object[] p : host.getLocalPlayers()) {
                String id = (String) p[0];
                String name = (String) p[1];
                boolean isReady = (Boolean) p[3];
                boolean isMe = name.equals(myName);
                boolean isHostPlayer = id.equals("0");

                int chips = 500; 
                if (host.getGame() != null) {
                    var player = host.getGame().getPlayer(name);
                    if (player != null) {
                        chips = player.getChips();
                    }   
                }
                result.add(new PlayerInfo(id, chips, name, isMe, isHostPlayer, isReady));
            }
        } else if (client != null) {
            // Client: hent navne fra server
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
                            new org.jspace.ActualField("playerChips"),
                            new org.jspace.ActualField(name),
                            new org.jspace.FormalField(Integer.class)
                        );
                        if (chipInfo != null) {
                            chips = (Integer) chipInfo[2];
                        }
                    }
                } catch (Exception e) {
                    // if nothing then 500
                }
                result.add(new PlayerInfo(String.valueOf(i), chips, name, isMe, isHostPlayer, false));
            }
        }
        return result;
    }


    public void startGame() {
        if (host == null) {
            System.err.println("Only host can start game!");
            return;
        }
        
        new Thread(() -> {
            try {
                
                host.getGame().playCompleteHand();
            } catch (InterruptedException e) {
            }
        }).start();
    }

    private String getStatusText(int playerCount) {
        if (host != null) {
            return "Spillere: " + playerCount + "/" + Host.MAX_LOBBY_SIZE;
        } else {
            return "Forbundet som: " + getMyName() + " (" + playerCount + " spillere)";
        }
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

    public boolean isHost() {
        return host != null;
    }

    /**
     * Toggle ready status.
     */
    public void toggleReady() {
        isReady = !isReady;
        if (host != null) {
            host.sendReadyFlag(isReady);
        } else if (client != null) {
            client.sendReadyFlag(isReady);
        }
    }

    public boolean isReady() {
        return isReady;
    }

    /**
     * Kick en spiller (kun for host).
     */
    public void kickPlayer(String playerId) {
        if (host != null && !playerId.equals("0")) {
            host.kickPlayer(playerId);
        }
    }

    /**
     * Forlad spillet og luk forbindelse.
     */
    public void leave() {
        running = false;
        if (host != null) {
            host.stop();
        }
        if (client != null) {
            client.disconnect();
        }
    }

    /**
     * Stop model og tråde.
     */
    public void shutdown() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
        }
    }

    public Space getGameSpace() {
        if (host != null) {
            return host.getGameSpace();
        } else if (client != null) {
            return client.getGameSpace();
        }
        return null;
    }
    
    public ChatManager getChatManager() {
        if (host != null) {
            return host.getChatManager();
        }
        return client.getChatManager();
    }

    // Setters for callbacks
    public void setOnPlayersUpdated(Consumer<List<PlayerInfo>> callback) {
        this.onPlayersUpdated = callback;
    }

    public void setOnStatusUpdated(Consumer<String> callback) {
        this.onStatusUpdated = callback;
    }

    public void setOnKicked(Runnable callback) {
        this.onKicked = callback;
    }

    public void setOnServerShutdown(Runnable callback) {
        this.onServerShutdown = callback;
    }

}