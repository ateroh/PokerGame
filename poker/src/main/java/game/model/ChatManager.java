package game.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;
import org.jspace.Tuple;

import game.controller.TableController;
import game.players.PlayerClient;

public class ChatManager {
    
    public SequentialSpace chat; // Local chat for display
    public SequentialSpace globalChatSpace; // Host: receives all messages
    
    // Host specific
    private SpaceRepository hostRepository;
    private Map<String, SequentialSpace> clientMailboxes = new ConcurrentHashMap<>();

    // Client specific
    private RemoteSpace remoteGlobalChat; // To send messages to host
    private RemoteSpace personalMailbox;  // To receive messages from host
    
    public PlayerClient client;
    private TableController controller;
    private volatile boolean running = true;

    public ChatManager(PlayerClient client) {
        this.client = client;
        chat = new SequentialSpace();
        globalChatSpace = new SequentialSpace();
    }

    public void setController(TableController controller) {
        this.controller = controller;
    }

    public void setupHost(SpaceRepository repository) {
        this.hostRepository = repository;
    }

    public void createMailbox(String playerId) {
        clientMailboxes.computeIfAbsent(playerId, k -> {
            SequentialSpace mailbox = new SequentialSpace();
            if (hostRepository != null) {
                hostRepository.add("chat_" + k, mailbox);
            }
            return mailbox;
        });
    }

    public void setupClient(String serverUri, String myId) {
        try {
            remoteGlobalChat = new RemoteSpace(serverUri + "/globalchat?keep");
            personalMailbox = new RemoteSpace(serverUri + "/chat_" + myId + "?keep");
        } catch (IOException e) {
            System.err.println("Chat setup failed: " + e.getMessage());
        }
    }

    public void startMessageReceiver() {
        running = true;
        new Thread(() -> {
            Space source = (client.isHost()) ? chat : personalMailbox;
            if (source == null) return;

            while (running && client.isConnected()) {
                try {
                    Tuple messageTuple = new Tuple(source.get(
                        new FormalField(String.class), // sender id
                        new FormalField(String.class), // message
                        new FormalField(Boolean.class) // isAllChat
                    ));
                    
                    String senderId = messageTuple.getElementAt(String.class, 0);
                    String message = messageTuple.getElementAt(String.class, 1);

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

    // For host: lyt efter beskeder i globalChatSpace og broadcast til alle spillere via mailboxes
    public void startGlobalChatBroadcaster() {
        if (!client.isHost()) return;
        
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

                    // Broadcast til alle spillere
                    List<Object[]> players = client.getLocalPlayers();
                    for (Object[] p : players) {
                        String pid = (String) p[0];

                        if (pid.equals(client.getId())) {
                            // Host selv: send til lokal chat
                            chat.put(senderId, message, true);
                        } else {
                            // Clients: send til deres mailbox på hosten
                            createMailbox(pid);
                            SequentialSpace mailbox = clientMailboxes.get(pid);
                            if (mailbox != null) {
                                mailbox.put(senderId, message, true);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void ensureMailboxExists(String pid) {
        createMailbox(pid);
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
        // Unused in current implementation, but could be adapted for DMs
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

    public SequentialSpace getChat(){
        return chat;
    }

    public SequentialSpace getGlobalChatSpace() {
        return globalChatSpace;
    }

    public void stop() {
        running = false;
        if (remoteGlobalChat != null) {
            try { remoteGlobalChat.close(); } catch (IOException e) {}
        }
        if (personalMailbox != null) {
            try { personalMailbox.close(); } catch (IOException e) {}
        }
    }
}
