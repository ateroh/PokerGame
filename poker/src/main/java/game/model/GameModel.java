package game.model;

import java.util.ArrayList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;



public class GameModel {
    
    private Space gameSpace;
    private List<Card> communityCards = new ArrayList<>();
    private java.util.Map<String, Card[]> playerHoleCards = new java.util.HashMap<>();

    private int pot;
    private int currentBet;
    private List<PlayerModel> players;
    private DealerModel dealer;
    private String phase;
    private final int StartingChips = 500;
    private final int maxPlayers = 4;
    private final int minPlayers = 2;
    public static final int SMALL_BLIND = 5;
    public static final int BIG_BLIND = 10;
    private int determineSBBB = 0;
    private int lastRaiseAmount;


    public GameModel(Space gameSpace, DeckModel deck) {
        this.gameSpace = gameSpace;
        this.dealer = new DealerModel(gameSpace, deck);
        this.players = new ArrayList<>();
        this.pot = 0;
        this.phase = "waiting";

    }
    public void addPlayer(String name) {
        if (players.size() >= maxPlayers) {
            return;
        }

        PlayerModel player = new PlayerModel(name, StartingChips);
        players.add(player);
    }

    public PlayerModel getPlayer(String name) {
        for (PlayerModel player : players) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    public List<PlayerModel> getPlayers() { return players; }

    public boolean canStart() {
        return players.size() >= minPlayers;
    }

    public void gameDealFlop() throws InterruptedException {
        List<String> names = new ArrayList<>();
        for (PlayerModel p : players) {
            names.add(p.getName());
        }
        phase = "FLOP";
        dealer.dealFlop(names);
        System.out.println("Flop dealt");
    }
    public void gameDealTurn() throws InterruptedException {
        List<String> names = new ArrayList<>();
        for (PlayerModel p : players) {
            names.add(p.getName());
        }
        phase = "TURN";
        dealer.dealTurn(names);
        System.out.println("turn  dealt");
    }
    public void gameDealRiver() throws InterruptedException {
        List<String> names = new ArrayList<>();
        for (PlayerModel p : players) {
            names.add(p.getName());
        }
        phase = "TURN";
        dealer.dealRiver(names);
        System.out.println("river dealt");
    }


    public void startNewHand() throws InterruptedException {
        System.out.println("new game");
        
        if (!canStart()) {
            System.out.println("need at least " + minPlayers + " players");
            return;
        }
        
        for (PlayerModel player : players) {
            player.resetForNewHand();
        }
        pot = 0;
        currentBet = 0;
        lastRaiseAmount = BIG_BLIND;
        phase = "PREFLOP";
        
        assignPositionsAndBlinds();
        
        List<String> names = new ArrayList<>();
        for (PlayerModel p : players) {
            names.add(p.getName());
        }
        System.out.println("Dealing to: " + names);

        dealer.dealCards(names);
        updateGameStatus();
    
        System.out.println("hand started");
    }

    private void postBlinds() {
        int sbIndex = (determineSBBB + 1) % players.size();
        int bbIndex = (determineSBBB + 2) % players.size();
        
        PlayerModel smallBlind = players.get(sbIndex);
        PlayerModel bigBlind = players.get(bbIndex);
        
        int sbAmount = smallBlind.placeBet(SMALL_BLIND);
        int bbAmount = bigBlind.placeBet(BIG_BLIND);
        
        pot += sbAmount + bbAmount;
        currentBet = BIG_BLIND;
        
        System.out.println(smallBlind.getName() + " has small blind: " + sbAmount);
        System.out.println(bigBlind.getName() + " has big blind: " + bbAmount);
    }
    // assignpositions og postblind goer det samme. assign har bare cases
    @SuppressWarnings("unused")
    private void assignPositionsAndBlinds() {
        int numPlayers = players.size();
        
            switch (numPlayers) {
                case 2 ->                 {
                    
                    PlayerModel bigBlind = players.get(0);
                    PlayerModel smallBlind = players.get(1);
                    bigBlind.setPosition("bigBlind");
                    smallBlind.setPosition("smallBlind");
                    int sbAmount = smallBlind.placeBet(SMALL_BLIND);
                    pot += sbAmount;
                    System.out.println(smallBlind.getName() + " has small blind: " + sbAmount);

                    try {
                        for (PlayerModel p : players) {
                            gameSpace.put("playerAction", smallBlind.getName(), "smallBlind", sbAmount, smallBlind.getChips(), pot);
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    int bbAmount = bigBlind.placeBet(BIG_BLIND);
                    pot += bbAmount;
                    currentBet = BIG_BLIND;
                    lastRaiseAmount = BIG_BLIND - SMALL_BLIND;
                    System.out.println(bigBlind.getName() + " has big blind: " + bbAmount);
                    try {
                        for (PlayerModel p : players) {
                            gameSpace.put("playerAction", bigBlind.getName(), "bigBlind", bbAmount, bigBlind.getChips(), pot);
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            case 3 ->                 {
                    
                    PlayerModel bigBlind = players.get(0);
                    PlayerModel dealer2 = players.get(1);
                    PlayerModel regular = players.get(2);
                    bigBlind.setPosition("bigBlind");
                    dealer2.setPosition("dealer");
                    regular.setPosition("player");
                    int sbAmount = dealer2.placeBet(SMALL_BLIND);
                    pot += sbAmount;
                    System.out.println(dealer2.getName() + " (also dealer) has small blind: " + sbAmount);

                    try {
                        gameSpace.put("playerAction", dealer2.getName(), "smallBlind", sbAmount, dealer2.getChips(), pot);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    int bbAmount = bigBlind.placeBet(BIG_BLIND);
                    pot += bbAmount;
                    currentBet = BIG_BLIND;
                    System.out.println(bigBlind.getName() + " has big blind: " + bbAmount);
                    try {
                        gameSpace.put("playerAction", bigBlind.getName(), "bigBlind", bbAmount, bigBlind.getChips(), pot);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            case 4 ->                 {
                    
                    PlayerModel bigBlind = players.get(0);
                    PlayerModel regular = players.get(1);
                    PlayerModel dealer2 = players.get(2);
                    PlayerModel smallBlind = players.get(3);

                    bigBlind.setPosition("bigBlind");
                    regular.setPosition("player");
                    dealer2.setPosition("dealer");
                    smallBlind.setPosition("smallBlind");

                    int sbAmount = smallBlind.placeBet(SMALL_BLIND);
                    pot += sbAmount;
                    System.out.println(smallBlind.getName() + " has small blind: " + sbAmount);

                    try {
                        gameSpace.put("playerAction", smallBlind.getName(), "smallBlind", sbAmount, smallBlind.getChips(), pot);
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    int bbAmount = bigBlind.placeBet(BIG_BLIND);
                    pot += bbAmount;
                    currentBet = BIG_BLIND;
                    System.out.println(bigBlind.getName() + " has big blind: " + bbAmount);
                    
                    try {
                        gameSpace.put("playerAction", bigBlind.getName(), "bigBlind", bbAmount, bigBlind.getChips(), pot);
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            default -> {
            }
        }
        System.out.println("asssigned roles complete");
    }
    
    public void bettingRound() throws InterruptedException {
        System.out.println("betting round");
        if (!phase.equals("PREFLOP")) {
            lastRaiseAmount = BIG_BLIND;
        }
        
        int numPlayers = players.size();
        int PlayersActed = 0;
        int lastPersonToRaise = -1;
        boolean someoneRaised = true;
        boolean firstLoop = true;
        int activePlayers = 0;
        for (PlayerModel p : players) {
            if (!p.hasFolded() && !p.isAllIn()) {
                activePlayers++;
            }
        }
        int i = 0;
        int maxIterations = numPlayers*10;
        while (i < maxIterations) {
            int playerIndex = i % numPlayers;
            PlayerModel player = players.get(playerIndex);
            if (player.hasFolded() || player.isAllIn()) {
                i++;
                continue;
            }

            int playersRemaining = 0;
            for (PlayerModel p : players) {
                if (!p.hasFolded()) playersRemaining++;
            }
            if (playersRemaining <= 1) {
                System.out.println("Only one player left");
                break;
            }

            if (lastPersonToRaise != -1 && playerIndex == lastPersonToRaise) {
                break;
            }
            
            if (lastPersonToRaise == -1 && PlayersActed >= activePlayers) {
                break;
            }   

            System.out.println(player.getName() + " turn");
            System.out.println("Pot: " + pot + ". player needs to call: " + (currentBet - player.getBetAmount()));
            
            gameSpace.put("yourTurn", player.getName(), currentBet, player.getChips(), lastRaiseAmount);   
            
            Object[] action = gameSpace.get(
                new ActualField("action"),
                new ActualField(player.getName()),
                new FormalField(String.class),
                new FormalField(Integer.class)
            );
            String actionType = (String) action[2];
            int amount = (Integer) action[3];

            processAction(player.getName(), actionType, amount);
            PlayersActed++;

            if (actionType.equalsIgnoreCase("raise")) {
            lastPersonToRaise = i % numPlayers;
            PlayersActed = 0; // alle skal goere noget igen
            }   
            i++;
            
        }
        
        System.out.println("betting complete. Pot: " + pot);
        
        for (PlayerModel p : players) {
            p.resetForNewRound();
        }
        currentBet = 0;
    }

    
    private void processAction(String playerName, String action, int amount) 
        throws InterruptedException {
        
        PlayerModel player = getPlayer(playerName);
        if (player == null) return;
        
        int actualAmount = 0;
        
        switch (action.toLowerCase()) {
            case "fold" -> {
                player.fold();
                System.out.println("player:" + playerName + " folded");
            }
                
            case "call" -> {
                actualAmount = player.call(currentBet);
                pot += actualAmount;
                System.out.println(playerName + " called " + actualAmount + ", chips now: " + player.getChips());
            }
                
            case "raise" -> {
                int previousBet = player.getBetAmount();
                int amountToCall = currentBet - previousBet;  
                System.out.println("check: previousBet=" + previousBet + ", currentBet=" + currentBet + ", lastRaiseAmount=" + lastRaiseAmount + ", amountToCall=" + amountToCall);
                
                int totalToAdd = amountToCall + amount;  
                actualAmount = player.raise(totalToAdd);  
                pot += actualAmount;
                currentBet = player.getBetAmount();
                lastRaiseAmount = amount;  
                
                System.out.println("error chekcs : amount from slider=" + amount + ", totalToAdd=" + totalToAdd + ", actualAmount=" + actualAmount + ", currentBet=" + currentBet + ", lastRaiseAmount=" + lastRaiseAmount);
                System.out.println("player " + playerName + " called " + amountToCall + " and raised " + amount + " (total: " + actualAmount + ") to " + currentBet);
            }
                
            case "check" -> System.out.println("player:  " + playerName + " checked");
        }
        
        for (PlayerModel p : players) {
            gameSpace.put("playerAction", playerName, action, actualAmount, player.getChips(), pot);
        }
        updateGameStatus();
        
    }

    private void updateGameStatus() throws InterruptedException {
        gameSpace.getp(new ActualField("gameStatus"), 
                    new FormalField(Integer.class));
        
        gameSpace.put("gameStatus", pot);
        
        for (PlayerModel p : players) {
            gameSpace.getp(new ActualField("playerChips"), new ActualField(p.getName()), new FormalField(Integer.class));
            gameSpace.put("playerChips", p.getName(), p.getChips());
        }
    }

    private void endHand() throws InterruptedException {
        System.out.println("Hand complete");
        
        List<PlayerModel> activePlayers = new ArrayList<>();
        for (PlayerModel p : players) {
            if (!p.hasFolded()) activePlayers.add(p);
        }
        
        if (activePlayers.size() == 1) {
            PlayerModel winner = activePlayers.get(0);
            winner.addPotToPlayer(pot);
            System.out.println("" + winner.getName() + " wins: " + pot + "");
            announceWinner(winner.getName(), "alle andre foldede", pot);
        } else {
            List<Card> community = new ArrayList<>();
            Object[] flop = gameSpace.getp(new ActualField("communityFlop"),
                new FormalField(String.class), new FormalField(String.class),
                new FormalField(String.class), new FormalField(String.class),
                new FormalField(String.class), new FormalField(String.class));
            if (flop != null) {
                community.add(new Card((String)flop[1], (String)flop[2]));
                community.add(new Card((String)flop[3], (String)flop[4]));
                community.add(new Card((String)flop[5], (String)flop[6]));
            }
            Object[] turn = gameSpace.getp(new ActualField("communityTurn"),
                new FormalField(String.class), new FormalField(String.class));
            if (turn != null) {
                community.add(new Card((String)turn[1], (String)turn[2]));
            }
            Object[] river = gameSpace.getp(new ActualField("communityRiver"),
                new FormalField(String.class), new FormalField(String.class));
            if (river != null) {
                community.add(new Card((String)river[1], (String)river[2]));
            }

            PlayerModel winner = null;
            HandModel winnerHand = null;
            
            for (PlayerModel p : activePlayers) {
                // Get this player's hole cards
                Object[] hole = gameSpace.getp(new ActualField("holeCards"), new ActualField(p.getName()),
                    new FormalField(String.class), new FormalField(String.class),
                    new FormalField(String.class), new FormalField(String.class));
                
                if (hole != null) {
                    List<Card> holeCards = new ArrayList<>();
                    holeCards.add(new Card((String)hole[2], (String)hole[3]));
                    holeCards.add(new Card((String)hole[4], (String)hole[5]));

                    System.out.println("DEBUG hole cards: " + hole[0] + ", " + hole[1] + ", " + hole[2] + ", " + hole[3]);
                    System.out.println("DEBUG community size: " + community.size());
                    for (Card c : community) {
                        System.out.println("DEBUG community card: suit=" + c.getSuit() + " rank=" + c.getRank() + " value=" + c.getValue());
                    }
                    
                    HandModel hand = new HandModel(community, holeCards);
                    System.out.println(p.getName() + " has: " + hand.getHandName());
                    if(winnerHand == null || hand.compareTo(winnerHand) > 0) {
                        winnerHand = hand;
                        winner = p;
                    }
                }
            }
            
            if (winner != null && winnerHand != null) {
                winner.addPotToPlayer(pot);
                System.out.println("Player " + winner.getName() + " wins " + pot + " with " + winnerHand.getHandName());
                announceWinner(winner.getName(), winnerHand.getHandName(), pot);
            }
        }
        
        pot = 0;
        updateGameStatus();
    }

    private void announceWinner(String winnerName, String handName, int potAmount) throws InterruptedException {
        // Sender vinder information til alle spillere
        for (PlayerModel p : players) {
            gameSpace.put("handResult", p.getName(), winnerName, handName, potAmount);
        }
    }

    public void playCompleteHand() throws InterruptedException {
        
        // Reset deck for ny hånd
        dealer.resetForNewHand();

        startNewHand();

        bettingRound();  
        Thread.sleep(500);
        gameDealFlop();
        
        bettingRound();  
        Thread.sleep(500);
        gameDealTurn();

        bettingRound();  
        Thread.sleep(500);
        gameDealRiver();

        bettingRound();  
        Thread.sleep(500);
        endHand();
    }

    /**
     * Starter en ny runde (bruges til at fortsætte spillet efter en hånd er afsluttet)
     */
    public void startNewRound() throws InterruptedException {
        // Fjern spillere uden chips
        players.removeIf(p -> p.getChips() <= 0);

        if (!canStart()) {
            System.out.println("Ikke nok spillere til at fortsætte");
            gameSpace.put("gameOver", "Ikke nok spillere");
            return;
        }

        // Rotér dealer/blind positioner
        rotateDealerButton();

        // Spil en ny hånd
        playCompleteHand();
    }

    // Kører en fuld poker session med flere hænder indtil kun en spiller har chips
    public void runGameLoop() throws InterruptedException {
        while (canStart()) {
            playCompleteHand();

            // Fjern spillere der er busted
            int playersWithChips = 0;
            for (PlayerModel p : players) {
                if (p.getChips() > 0) playersWithChips++;
            }

            if (playersWithChips < minPlayers) {
                System.out.println("Spillet er slut - ikke nok spillere med chips");
                break;
            }

            // Rotér dealer button
            rotateDealerButton();

            // Vent lidt før næste hånd
            Thread.sleep(3000);
        }

        // Annoncér spillet slut
        gameSpace.put("gameOver", "Spillet er slut");
    }

    private void rotateDealerButton() {
        determineSBBB = (determineSBBB + 1) % players.size();
    }

    public int getPot() {
        return pot;
    }

    public void storeHoleCards(String playerName, Card c1, Card c2) {
        playerHoleCards.put(playerName, new Card[]{c1, c2});
    }

    public void storeCommunityCard(Card card) {
        communityCards.add(card);
    }
}
