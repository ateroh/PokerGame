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
    public SpaceRepository chats;
    public PlayerClient client;
    
    private TableController controller;

    public ChatManager(PlayerClient client) {
        this.client = client;
        chat = new SequentialSpace();
        chats = new SpaceRepository();
    }

    public void setController(TableController controller) {
        this.controller = controller;
    }

    public void startMessageReceiver() {
        new Thread(() -> {
            while (client.isConnected()) {
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
        // Send to all  players
        List<Object[]> players = client.getLocalPlayers();
        for (Object[] p : players) {
            String pid = (String)p[0];
            String puri = (String)p[2];

            // Connect if not already connected
            addChatToRepo(pid, puri);

            sendMessage(message, pid, true);
        }
    }

    public void addChatToRepo(String peerId, String peerUri){
        // Only add if not exists
        if(chats.get(peerId) == null) {
            try {
                // peerUri e.g. tcp://ip:port
                // Chat space is at peerUri + "/chat?keep"
                // Standard PlayerClient sets gate at uri + "/?keep"
                // And adds "chat" to repo.
                // So RemoteSpace string is uri + "/chat?keep"
                chats.add(peerId, new RemoteSpace(peerUri + "/chat?keep"));
            } catch (IOException e) {
                System.err.println("Could not connect to chat for " + peerId);
            }
        }
    }

    public SequentialSpace getChat(){
        return chat;
    }

    public Space getPeerChat(String peerId){
        return chats.get(peerId);
    }
}
