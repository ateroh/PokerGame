package game.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SCENE MANAGER - Centraliseret navigation i JavaFX
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * HVAD ER EN SCENE MANAGER?
 * ─────────────────────────
 * En Scene Manager håndterer skift mellem forskellige skærme (scenes) i din app.
 * I stedet for at hver controller selv skal loade FXML og håndtere Stage,
 * gør SceneManager det hele ét centralt sted.
 *
 *
 * SINGLETON PATTERN
 * ─────────────────
 * SceneManager bruger Singleton pattern - der eksisterer KUN ÉN instans.
 * Dette sikrer at alle controllers bruger samme Stage og samme scene-register.
 *
 *   SceneManager.getInstance()  ← Returnerer altid samme instans
 *
 *
 * SÅDAN BRUGES DEN
 * ────────────────
 *
 * 1. I Main.java (ved app-start):
 *
 *    SceneManager.getInstance().setStage(primaryStage);
 *    SceneManager.getInstance().switchScene("menu");
 *
 * 2. I enhver Controller (for at skifte scene):
 *
 *    SceneManager.getInstance().switchScene("lobby");
 *    SceneManager.getInstance().switchScene("game");
 *
 * 3. Hvis du skal sende data til næste controller:
 *
 *    LobbyController ctrl = SceneManager.getInstance().switchSceneAndGetController("lobby");
 *    ctrl.setPlayerName("Emil");
 *
 *
 * FLOW EKSEMPEL
 * ─────────────
 *
 *   Menu ──[CREATE clicked]──> Lobby ──[START clicked]──> Game
 *     │                          │                          │
 *     └──────────────────────────┴──────────────────────────┘
 *                    Alle bruger SceneManager
 *
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class SceneManager {

    // ═══════════════════════════════════════════════════════════════════════
    // SINGLETON INSTANCE
    // ═══════════════════════════════════════════════════════════════════════

    // Den eneste instans af SceneManager (singleton)
    private static SceneManager instance;

    // ═══════════════════════════════════════════════════════════════════════
    // INSTANCE VARIABLER
    // ═══════════════════════════════════════════════════════════════════════

    // Reference til applikationens hovedvindue (Stage)
    // Stage er "vinduet" - Scene er "indholdet" i vinduet
    private Stage stage;

    // Map der forbinder scene-navne med FXML-fil stier
    // Eksempel: "menu" -> "/fxml_files/PokerMenu.fxml"
    private Map<String, String> scenes = new HashMap<>();

    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR (privat - brug getInstance())
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Privat constructor - kan ikke kaldes udefra.
     * Brug SceneManager.getInstance() i stedet.
     */
    private SceneManager() {
        // Registrer alle scenes i applikationen
        // Tilføj nye scenes her når du laver dem
        scenes.put("menu", "/fxml_files/PokerMenu.fxml");
        scenes.put("lobby", "/fxml_files/Lobby.fxml");
        scenes.put("game", "/fxml_files/Game.fxml");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SINGLETON GETTER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Henter den eneste instans af SceneManager.
     * Opretter den hvis den ikke findes (lazy initialization).
     *
     * @return Den globale SceneManager instans
     */
    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SETUP METODER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Sætter hovedvinduet (Stage).
     * SKAL kaldes én gang ved app-start i Main.java.
     *
     * @param stage Hovedvinduet fra Application.start()
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Tilføjer en ny scene til registret.
     * Brug dette hvis du vil tilføje scenes dynamisk.
     *
     * @param name Navnet du vil bruge (f.eks. "settings")
     * @param fxmlPath Stien til FXML filen (f.eks. "/fxml_files/Settings.fxml")
     */
    public void addScene(String name, String fxmlPath) {
        scenes.put(name, fxmlPath);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // NAVIGATION METODER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Skifter til en anden scene.
     *
     * Eksempel:
     *   SceneManager.getInstance().switchScene("lobby");
     *
     * @param sceneName Navnet på scenen (registreret i constructor)
     */
    public void switchScene(String sceneName) {
        // Find FXML-stien ud fra scene-navnet
        String fxmlPath = scenes.get(sceneName);

        if (fxmlPath == null) {
            System.err.println("FEJL: Scene ikke fundet: " + sceneName);
            System.err.println("Tilgængelige scenes: " + scenes.keySet());
            return;
        }

        try {
            // Load FXML filen og få rod-elementet
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Opret ny Scene med rod-elementet
            Scene scene = new Scene(root);

            // Sæt scenen på stage og vis den
            stage.setScene(scene);
            stage.show();

            System.out.println("→ Skiftet til scene: " + sceneName);

        } catch (IOException e) {
            System.err.println("FEJL: Kunne ikke loade " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Skifter scene OG returnerer den nye controller.
     * Brug dette når du skal sende data til den nye scene.
     *
     * Eksempel:
     *   LobbyController ctrl = SceneManager.getInstance().switchSceneAndGetController("lobby");
     *   ctrl.setHost(myHost);
     *   ctrl.setPlayerName("Emil");
     *
     * @param sceneName Navnet på scenen
     * @return Controlleren for den nye scene (cast til korrekt type)
     */
    public <T> T switchSceneAndGetController(String sceneName) {
        String fxmlPath = scenes.get(sceneName);

        if (fxmlPath == null) {
            System.err.println("FEJL: Scene ikke fundet: " + sceneName);
            return null;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

            System.out.println("→ Skiftet til scene: " + sceneName);

            // Returner controlleren så caller kan sende data til den
            return loader.getController();

        } catch (IOException e) {
            System.err.println("FEJL: Kunne ikke loade " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // GETTER METODER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Returnerer hovedvinduet.
     * Nyttigt hvis du skal ændre vinduestitel, størrelse, etc.
     */
    public Stage getStage() {
        return stage;
    }
}

