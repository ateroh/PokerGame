package game.test;

import game.players.Host;
import game.players.PlayerClient;

import java.io.IOException;

/**
 * Terminal 1: mvn exec:java -Dexec.mainClass="game.test.ConnectionTest" -Dexec.args="host"
 * Terminal 2: mvn exec:java -Dexec.mainClass="game.test.ConnectionTest" -Dexec.args="client"
 */
public class ConnectionTest {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Brug: ConnectionTest <host|client>");
            System.out.println("  host   - Start server på port 9001");
            System.out.println("  client - Forbind til localhost:9001");
            return;
        }

        String mode = args[0].toLowerCase();

        if (mode.equals("host")) {
            runHost();
        } else if (mode.equals("client")) {
            runClient();
        } else {
            System.out.println("Ukendt mode: " + mode);
            System.out.println("Brug 'host' eller 'client'");
        }
    }

    private static void runHost() throws Exception {
        Host host = new Host(9001, "HostPlayer");
        host.start();

        System.out.println("\n=== SERVER KØRER ===");
        System.out.println("Venter på spillere...");
        System.out.println("Tryk Enter for at stoppe serveren.\n");

        // Hold serveren kørende
        System.in.read();

        host.stop();
        System.out.println("Server stoppet.");
    }

    private static void runClient() throws IOException, InterruptedException {
        PlayerClient client = new PlayerClient("localhost", 9001, "ClientPlayer");

        System.out.println("Forsøger at forbinde til server...");
        client.connect();

        System.out.println("\n=== FORBUNDET TIL SERVER ===");
        System.out.println("Du er nu forbundet som: " + client.getUsername());
        System.out.println("Tryk Enter for at afbryde.\n");

        // Hold forbindelsen
        System.in.read();

        client.disconnect();
        System.out.println("Afbrudt fra server.");
    }
}

