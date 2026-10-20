import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BattleVisualizer extends Application {
    private static List<BattleEvent> events = new ArrayList<>();

    private final Map<String, PokemonPanel> panels = new LinkedHashMap<>();
    private TextArea battleLog;
    private int eventIndex = 0;
    private Timeline playback;

    public static void setEvents(List<BattleEvent> newEvents) {
        events = new ArrayList<>(newEvents);
    }

    public static void openWindow(List<BattleEvent> newEvents) {
        events = new ArrayList<>(newEvents);

        Runnable showWindow = () -> {
            try {
                BattleVisualizer visualizer = new BattleVisualizer();
                Stage stage = new Stage();
                visualizer.start(stage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        try {
            Platform.startup(showWindow);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(showWindow);
        }
    }

    @Override
    public void start(Stage stage) {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fbff, #eef3f8);");

        Label title = new Label("Pokémon Battle Visualization");
        title.setFont(Font.font(28));

        HBox battlefield = new HBox(80);
        battlefield.setAlignment(Pos.CENTER);

        this.createPokemonPanelsFromEvents();
        for (PokemonPanel panel : this.panels.values()) {
            battlefield.getChildren().add(panel.container);
        }

        if (this.panels.isEmpty()) {
            Label empty = new Label("No Pokémon was loaded by the interpreter.");
            empty.setFont(Font.font(18));
            battlefield.getChildren().add(empty);
        }

        this.battleLog = new TextArea();
        this.battleLog.setEditable(false);
        this.battleLog.setPrefHeight(240);
        this.battleLog.setWrapText(true);
        this.battleLog.setStyle("-fx-control-inner-background: #101820; -fx-text-fill: #f7f7f7; -fx-font-family: 'Monospaced';");

        root.getChildren().addAll(title, battlefield, this.battleLog);

        Scene scene = new Scene(root, 850, 600);
        stage.setTitle("Pokémon Battle");
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.show();
        stage.toFront();

        System.out.println("BattleVisualizer stage.show() finished.");

        this.playEvents();
    }

    private void createPokemonPanelsFromEvents() {
        for (BattleEvent event : events) {
            if (event.type == BattleEvent.Type.POKEMON_LOADED
                    && !this.panels.containsKey(event.pokemonName)) {
                this.panels.put(event.pokemonName, new PokemonPanel(event.pokemonName,
                        event.hp, event.attack, event.defend));
            }
        }
    }

    private void playEvents() {
        this.playback = new Timeline(
                new KeyFrame(Duration.seconds(1.2), e -> this.playNextEvent()));
        this.playback.setCycleCount(Animation.INDEFINITE);
        this.playback.play();
    }

    private void playNextEvent() {
        if (this.eventIndex >= events.size()) {
            if (this.playback != null) {
                this.playback.stop();
            }
            return;
        }

        BattleEvent event = events.get(this.eventIndex++);

        if (event.type == BattleEvent.Type.TEXT) {
            this.appendLog(event.message);
            return;
        }

        if (event.type == BattleEvent.Type.POKEMON_LOADED) {
            this.appendLog("Loaded " + event.pokemonName + " with hp=" + event.hp
                    + ", attack=" + event.attack + ", defend=" + event.defend + ".");
            return;
        }

        if (event.type == BattleEvent.Type.SPELL) {
            this.appendLog(event.attacker + " uses " + event.moveName + " on "
                    + event.target + ".");
            this.appendLog(event.target + " takes " + event.damage + " damage: hp "
                    + event.hpBefore + " -> " + event.hpAfter + ".");

            PokemonPanel attackerPanel = this.panels.get(event.attacker);
            PokemonPanel targetPanel = this.panels.get(event.target);

            if (attackerPanel != null) {
                this.attackAnimation(attackerPanel.sprite,
                        this.isLeftSide(event.attacker));
            }
            if (targetPanel != null) {
                targetPanel.setHp(event.hpAfter);
                this.damageAnimation(targetPanel.sprite);
                if (event.hpAfter <= 0) {
                    this.appendLog(event.target + " has fainted.");
                }
            }
        }

        if (this.eventIndex >= events.size() && this.playback != null) {
            this.playback.stop();
        }
    }

    private boolean isLeftSide(String pokemonName) {
        int index = 0;
        for (String name : this.panels.keySet()) {
            if (name.equals(pokemonName)) {
                return index == 0;
            }
            index++;
        }
        return true;
    }

    private void appendLog(String text) {
        this.battleLog.appendText(text + "\n");
    }

    private void attackAnimation(Label sprite, boolean moveRight) {
        TranslateTransition move = new TranslateTransition(Duration.millis(250), sprite);
        move.setByX(moveRight ? 45 : -45);
        move.setAutoReverse(true);
        move.setCycleCount(2);
        move.play();
    }

    private void damageAnimation(Label sprite) {
        FadeTransition fade = new FadeTransition(Duration.millis(150), sprite);
        fade.setFromValue(1.0);
        fade.setToValue(0.2);
        fade.setAutoReverse(true);
        fade.setCycleCount(4);
        fade.play();
    }

    private static class PokemonPanel {
        private final int maxHp;
        private final Label hpLabel;
        private final ProgressBar hpBar;
        private final Label sprite;
        private final VBox container;

        PokemonPanel(String name, int hp, int attack, int defend) {
            this.maxHp = Math.max(1, hp);

            Label nameLabel = new Label(name);
            nameLabel.setFont(Font.font(22));

            this.sprite = new Label("◓ " + name);
            this.sprite.setFont(Font.font(30));

            this.hpLabel = new Label();
            this.hpBar = new ProgressBar();
            this.hpBar.setPrefWidth(250);

            Label statsLabel = new Label("ATK: " + attack + "    DEF: " + defend);

            this.container = new VBox(10, nameLabel, this.sprite, this.hpLabel,
                    this.hpBar, statsLabel);
            this.container.setAlignment(Pos.CENTER);
            this.container.setPadding(new Insets(20));
            this.container.setMinWidth(280);
            this.container.setStyle(
                    "-fx-background-color: white; -fx-border-color: #b8c2cc; -fx-border-radius: 10; -fx-background-radius: 10;");

            this.setHp(hp);
        }

        void setHp(int currentHp) {
            int safeHp = Math.max(0, currentHp);
            this.hpLabel.setText("HP: " + safeHp + " / " + this.maxHp);
            this.hpBar.setProgress((double) safeHp / this.maxHp);
        }
    }
}
