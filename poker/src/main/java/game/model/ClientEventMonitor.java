package game.model;

import java.util.function.Consumer;
import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;

public class ClientEventMonitor {
    private final Space remoteGameSpace;
    private final String clientId;
    private volatile boolean running = true;
    private Consumer<String> onKicked;
    private Consumer<String> onServerShutdown;

    public ClientEventMonitor(Space remoteGameSpace, String clientId) {
        this.remoteGameSpace = remoteGameSpace;
        this.clientId = clientId;
    }

    public void start() {
        new Thread(() -> {
            while (running && remoteGameSpace != null) {
                try {
                    Object[] kicked = remoteGameSpace.getp(
                        new ActualField("kicked"), new ActualField(clientId), new FormalField(String.class));
                    if (kicked != null) {
                        running = false;
                        if (onKicked != null) onKicked.accept((String) kicked[2]);
                        break;
                    }

                    Object[] shutdown = remoteGameSpace.queryp(
                        new ActualField("shutdown"), new FormalField(String.class));
                    if (shutdown != null) {
                        running = false;
                        if (onServerShutdown != null) onServerShutdown.accept((String) shutdown[1]);
                        break;
                    }
                    Thread.sleep(500);
                } catch (Exception e) { break; }
            }
        }).start();
    }

    public void stop() { running = false; }
    public void setOnKicked(Consumer<String> cb) { this.onKicked = cb; }
    public void setOnServerShutdown(Consumer<String> cb) { this.onServerShutdown = cb; }
}
