package game.model;

import java.io.IOException;
import java.util.List;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import org.jspace.Tuple;

import game.controller.TableController;
import game.players.PlayerClient;

public class ChatManager {
    
    public SequentialSpace chat;
    public SequentialSpace globalChatSpace; // For host: modtager beskeder fra alle klienter
    public SpaceRepository chats;
    public PlayerClient client;
    
    private TableController controller;
    private RemoteSpace remoteGlobalChat; // For klienter: forbindelse til hostens globalchat
    private volatile boolean running = true;

    public ChatManager(PlayerClient client) {
        this.client = client;
        chat = new SequentialSpace();
        globalChatSpace = new SequentialSpace();
        chats = new SpaceRepository();
    }

    public void setController(TableController controller) {
        this.controller = controller;
    }

    public void connectToGlobalChat(String serverUri) {
        try {
            remoteGlobalChat = new RemoteSpace(serverUri + "/globalchat?keep");
        } catch (IOException e) {
            System.err.println("Kunne ikke forbinde til global chat: " + e.getMessage());
        }
    }

    public void startMessageReceiver() {
        running = true;
        new Thread(() -> {
            while (running && client.isConnected()) {
                try {
                    Tuple messageTuple = new Tuple(chat.get(
                        new FormalField(String.class), // sender id
                        new FormalField(String.class), // message
                        new FormalField(Boolean.class) // isAllChat
                    ));
                    
                    String senderId = messageTuple.getElementAt(String.class, 0);
                    String message = messageTuple.getElementAt(String.class, 1);

                    // Try to resolve name from client's player list
                    String senderName = resolveName(senderId);

                    String formattedMsg = senderName + ": " + message;
                    
                    if (controller != null) {
                        controller.appendChatMessage(formattedMsg);
                    } else {
                        System.out.println(formattedMsg);
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    // For host: lyt efter beskeder i globalChatSpace og broadcast til alle spillere
    public void startGlobalChatBroadcaster() {
        running = true;
        new Thread(() -> {
            while (running && client.isConnected()) {
                try {
                    Tuple messageTuple = new Tuple(globalChatSpace.get(
                        new FormalField(String.class), // sender id
                        new FormalField(String.class), // sender name
                        new FormalField(String.class)  // message
                    ));

                    String senderId = messageTuple.getElementAt(String.class, 0);
                    String senderName = messageTuple.getElementAt(String.class, 1);
                    String message = messageTuple.getElementAt(String.class, 2);

                    // Broadcast til alle spillere (inkl. host selv)
                    List<Object[]> players = client.getLocalPlayers();
                    for (Object[] p : players) {
                        String pid = (String) p[0];
                        String puri = (String) p[2];

                        // Hvis det er hosten selv, send direkte til lokalt chat space
                        if (pid.equals(client.getId())) {
                            try {
                                chat.put(senderId, message, true);
                            } catch (Exception e) {
                                System.err.println("Fejl ved lokal broadcast: " + e.getMessage());
                            }
                        } else {
                            // Send til andre spilleres remote chat space
                            addChatToRepo(pid, puri);
                            Space peerChat = getPeerChat(pid);
                            if (peerChat != null) {
                                try {
                                    peerChat.put(senderId, message, true);
                                } catch (Exception e) {
                                    System.err.println("Fejl ved broadcast til " + pid + ": " + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private String resolveName(String id) {
        // Simple linear search in local players
        List<Object[]> players = client.getLocalPlayers();
        for(Object[] p : players) {
            if(((String)p[0]).equals(id)) return (String)p[1];
        }
        return "Unknown(" + id + ")";
    }

    public void sendMessage(String message, String receiverId, Boolean isAllChat) {
        try {
            Space peerChat = getPeerChat(receiverId);
            if(peerChat != null) {
                peerChat.put(client.getId(), message, isAllChat);
            }
        } catch(Exception e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    public void sendGlobalMessage(String message) {
        try {
            // Hvis vi er host, send direkte til globalChatSpace
            if (client.isHost()) {
                globalChatSpace.put(client.getId(), client.getUsername(), message);
            } else if (remoteGlobalChat != null) {
                // Hvis vi er klient, send til hostens globalchat
                remoteGlobalChat.put(client.getId(), client.getUsername(), message);
            } else {
                System.err.println("Ikke forbundet til global chat");
            }
        } catch (InterruptedException e) {
            System.err.println("Fejl ved afsendelse af besked: " + e.getMessage());
        }
    }

    public void addChatToRepo(String peerId, String peerUri){
        // Only add if not exists
        if(chats.get(peerId) == null) {
            try {
                chats.add(peerId, new RemoteSpace(peerUri + "/chat?keep"));
            } catch (IOException e) {
                System.err.println("Could not connect to chat for " + peerId);
            }
        }
    }

    public SequentialSpace getChat(){
        return chat;
    }

    public SequentialSpace getGlobalChatSpace() {
        return globalChatSpace;
    }

    public Space getPeerChat(String peerId){
        return chats.get(peerId);
    }

    public void stop() {
        running = false;
        if (remoteGlobalChat != null) {
            try {
                remoteGlobalChat.close();
            } catch (IOException e) {}
        }
    }
}
