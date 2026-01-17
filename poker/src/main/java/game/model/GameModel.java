package game.model;

import java.util.ArrayList;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.Space;



public class GameModel {
    
    private Space gameSpace;
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

    public List<PlayerModel> getPlayers() {
        return players;
    }

    public boolean canStart() {
        return players.size() >= minPlayers;
    }

    public void gameDealFlop() throws InterruptedException {
        phase = "FLOP";
        dealer.dealFlop();
        System.out.println("Flop dealt");
    }
    public void gameDealCard() throws InterruptedException {
        phase = "TURN";
        dealer.postFlopDeal();
        System.out.println("one post card dealt");
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
        phase = "PREFLOP";
        
        assignPositionsAndBlinds();
        
        List<String> names = new ArrayList<>();
        for (PlayerModel p : players) {
            names.add(p.getName());
        }
        System.out.println("Dealing to: " + names);

        dealer.dealCards(names);
    
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
                        gameSpace.put("playerAction", smallBlind.getName(), "smallBlind", sbAmount, smallBlind.getChips(), pot);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    int bbAmount = bigBlind.placeBet(BIG_BLIND);
                    pot += bbAmount;
                    currentBet = BIG_BLIND;
                    System.out.println(bigBlind.getName() + " has big blind: " + bbAmount);
                    try {
                        gameSpace.put("playerAction", bigBlind.getName(), "bigBlind", bbAmount, bigBlind.getChips(), pot);
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

        int numPlayers = players.size();
        
        for (int i = 0; i < numPlayers; i++) {
            PlayerModel player = players.get(i);
            
            if (player.hasFolded() || player.isAllIn()) {
                continue;
            }
            
            System.out.println(player.getName() + " turn");
            System.out.println("Pot: " + pot + ". player needs to call: " + (currentBet - player.getBetAmount()));
            

                
            gameSpace.put("yourTurn", player.getName(), currentBet, player.getChips());
            
            Object[] action = gameSpace.get(
                new ActualField("action"),
                new ActualField(player.getName()),
                new FormalField(String.class),
                new FormalField(Integer.class)
            );
            
            String actionType = (String) action[2];
            int amount = (Integer) action[3];
            
            processAction(player.getName(), actionType, amount);
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
                System.out.println(playerName + " called " + actualAmount);
            }
                
            case "raise" -> {
                actualAmount = player.raise(amount);
                pot += actualAmount;
                currentBet = player.getBetAmount();
                System.out.println("player " + playerName + " raised to " + currentBet);
            }
                
            case "check" -> System.out.println("player:  " + playerName + " checked");
        }
        
        gameSpace.put("playerAction", playerName, action, actualAmount, player.getChips(), pot);
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

    private void endHand() {
        System.out.println("Hand complete");
        
        for (PlayerModel p : players) {
            if (!p.hasFolded()) {
                p.addPotToPlayer(pot);
                System.out.println("Player " + p.getName() + " wins " + pot);
                break;
            }
        }
        pot = 0;
    }

    public void playCompleteHand() throws InterruptedException {
        startNewHand();
        bettingRound();  
        
        gameDealFlop();
        bettingRound();  
        
        gameDealCard();
        bettingRound();  
        
        gameDealCard();
        bettingRound();  
        
        endHand();
    }

    public int getPot() {
        return pot;
    }
    
}
