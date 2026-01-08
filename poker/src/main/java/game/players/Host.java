package game.players;

import java.util.ArrayList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RandomSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

import game.model.DeckModel;
import game.model.GameModel;

/**
 * Host repræsenterer serveren i poker-spillet.
 * Opretter et tuple space som andre spillere kan forbinde til.
 * Lytter efter nye spillere i en baggrundstråd.
 */
public class Host {
    
    // Port som serveren lytter på (f.eks. 9001)
    private int port;

    // Hostens brugernavn
    private String username;

    // SpaceRepository håndterer netværksforbindelser til tuple spaces
    // Det er "serveren" der eksponerer spaces over netværk
    private SpaceRepository repository;

    // Det delte tuple space hvor al kommunikation sker
    // Både host og clients læser/skriver til dette space
    private Space gameSpace;
    private GameModel game;
    // Liste over alle spillere der er joined (inkl. host)
    private List<String> players = new ArrayList<>();

    // Baggrundstråd der lytter efter nye spillere
    private Thread listenerThread;
    
    // deck space
    private Space deckSpace;
    private DeckModel deck;

    // game space
   

    // Flag til at stoppe listener-tråden
    private boolean running = false;

    public Host(int port, String username) {
        this.port = port;
        this.username = username;
    }

    /**
     * Starter serveren og begynder at lytte efter spillere.
     *
     * URI formatet er: tcp://localhost:port/?conn
     * - "localhost" betyder serveren kun lytter lokalt (LAN)
     * - "?conn" angiver connection-protokollen
     */
    public void start() throws Exception {
        // Opbyg URI - serveren lytter på denne adresse
        String uri = "tcp://localhost:" + port + "/?conn";

        // Opret repository - dette er "netværkslaget" i jSpace
        repository = new SpaceRepository();

        // Åbn en "gate" så clients kan forbinde via TCP
        repository.addGate(uri);

        // Opret det faktiske tuple space hvor data gemmes
        // SequentialSpace betyder tuples behandles i rækkefølge
        gameSpace = new SequentialSpace();
        deckSpace = new RandomSpace();

        // Registrer space med navnet "game" - clients forbinder til dette navn
        repository.add("game", gameSpace);
        repository.add("deck", deckSpace);
        
        // init dick
        initModels();

        //maaske flyt til initmodels
        // Registrer host som den første spiller
        gameSpace.put("player", username, "host");
        players.add(username);

        System.out.println("Server started on port " + port);
        System.out.println("Host player: " + username);

        // Start baggrundstråd der lytter efter nye spillere
        running = true;
        listenerThread = new Thread(this::listenForPlayers);
        listenerThread.start();
    }

    /**
     * Lytter efter nye spillere i en uendelig løkke.
     * Kører i en separat tråd så den ikke blokerer hovedprogrammet.
     *
     * Bruger get() som er et BLOKERENDE kald - den venter indtil
     * en tuple der matcher findes i space.
     */
    private void listenForPlayers() {
        System.out.println("Listener startet - venter på spillere...");
        while (running) {
            try {
                // get() BLOKERER indtil en matching tuple findes
                // ActualField("newplayer") = matcher præcis strengen "newplayer"
                // FormalField(String.class) = matcher enhver String (spillerens navn)
                Object[] player = gameSpace.get(
                    new ActualField("newplayer"),
                    new FormalField(String.class)
                );

                // Tuple blev fundet og FJERNET fra space
                // player[0] = "newplayer", player[1] = username
                String playerName = (String) player[1];
                players.add(playerName);

                System.out.println("\n>>> Ny spiller joined: " + playerName);
                System.out.println(">>> Antal spillere: " + players.size());
                System.out.println(">>> Spillere: " + players);

            } catch (InterruptedException e) {
                // Tråden blev afbrudt (f.eks. ved stop())
                break;
            }
        }
    }


    private void initModels() throws InterruptedException {
        deck =  new DeckModel(deckSpace);
        deck.initialize();
        System.out.println("deck initialized");

        // virker ikke (gameModel)
        //game = new gameModel(gameSpace, deck);
    }
    
    /**
     * Stopper serveren og lukker alle forbindelser.
     */
    public void stop() {
        running = false;
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (repository != null) {
            repository.closeGate("tcp://localhost:" + port + "/?conn");
        }
    }
    
    public DeckModel getDeck() {
        return deck;
    }
    
    public Space getDeckSpace() {
        return deckSpace;
    }
    
    public Space getGameSpace() {
        return gameSpace;
    }
    
    public List<String> getPlayers() {
        return new ArrayList<>(players);
    }
    public GameModel getGame() {
        return game;
    }
}
