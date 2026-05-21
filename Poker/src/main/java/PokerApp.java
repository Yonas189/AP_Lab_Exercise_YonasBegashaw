import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 5-card draw poker vs dealer — all code in this one file. */
public class PokerApp extends Application {

    private static final int STARTING_CHIPS = 500;
    private static final int ANTE = 25;

    private final Deck deck = new Deck();
    private final List<Card> playerHand = new ArrayList<>();
    private final List<Card> dealerHand = new ArrayList<>();
    private final List<Button> playerCardButtons = new ArrayList<>();
    private final boolean[] held = new boolean[5];

    private Label statusLabel, chipsLabel, playerHandLabel, dealerHandLabel;
    private Button dealButton, drawButton;
    private HBox dealerCardsBox;
    private int chips = STARTING_CHIPS;
    private boolean roundActive;

    @Override
    public void start(Stage stage) {
        statusLabel = new Label("Press Deal to start.");
        statusLabel.setTextFill(Color.WHITE);
        chipsLabel = new Label();
        chipsLabel.setTextFill(Color.GOLD);
        chipsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        playerHandLabel = new Label();
        playerHandLabel.setTextFill(Color.LIGHTGRAY);
        dealerHandLabel = new Label("Dealer: ???");
        dealerHandLabel.setTextFill(Color.LIGHTGRAY);

        dealerCardsBox = new HBox(10);
        dealerCardsBox.setAlignment(Pos.CENTER);
        HBox playerCardsBox = new HBox(10);
        playerCardsBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 5; i++) {
            int index = i;
            Button cardBtn = new Button("?");
            cardBtn.setPrefSize(70, 100);
            cardBtn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            cardBtn.setOnAction(e -> toggleHold(index));
            playerCardButtons.add(cardBtn);
            playerCardsBox.getChildren().add(cardBtn);
        }

        dealButton = new Button("Deal (-" + ANTE + ")");
        dealButton.setOnAction(e -> deal());
        drawButton = new Button("Draw");
        drawButton.setDisable(true);
        drawButton.setOnAction(e -> draw());

        Button newGameButton = new Button("New Game");
        newGameButton.setOnAction(e -> resetChips());

        HBox actions = new HBox(15, dealButton, drawButton, newGameButton);
        actions.setAlignment(Pos.CENTER);

        VBox root = new VBox(20,
                title("5-Card Draw Poker", 26),
                chipsLabel,
                section("Dealer", dealerCardsBox, dealerHandLabel),
                section("You", playerCardsBox, playerHandLabel),
                actions,
                statusLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.setBackground(new Background(new BackgroundFill(Color.web("#1b5e20"), CornerRadii.EMPTY, Insets.EMPTY)));
        updateChips();

        stage.setTitle("Simple Poker");
        stage.setScene(new Scene(root, 620, 520));
        stage.show();
    }

    private VBox section(String name, HBox cards, Label info) {
        Label heading = new Label(name);
        heading.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        heading.setTextFill(Color.WHITE);
        VBox box = new VBox(8, heading, cards, info);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label title(String text, int size) {
        Label l = new Label(text);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, size));
        l.setTextFill(Color.WHITE);
        return l;
    }

    private void deal() {
        if (chips < ANTE) {
            statusLabel.setText("Out of chips! Press New Game.");
            return;
        }
        chips -= ANTE;
        updateChips();
        deck.reset();
        playerHand.clear();
        dealerHand.clear();
        for (int i = 0; i < 5; i++) {
            held[i] = false;
            playerHand.add(deck.draw());
            dealerHand.add(deck.draw());
        }
        roundActive = true;
        refreshCards();
        playerHandLabel.setText("Click cards to hold, then Draw");
        dealerHandLabel.setText("Dealer: ???");
        showDealerHidden();
        dealButton.setDisable(true);
        drawButton.setDisable(false);
        statusLabel.setText("Select cards to keep (yellow), then Draw.");
    }

    private void draw() {
        for (int i = 0; i < 5; i++) {
            if (!held[i]) {
                playerHand.set(i, deck.draw());
            }
        }
        if (Hand.score(dealerHand) < 1_000_000) {
            for (int i = 0; i < 5; i++) {
                dealerHand.set(i, deck.draw());
            }
        }
        roundActive = false;
        refreshCards();
        showDealerCards();
        int p = Hand.score(playerHand), d = Hand.score(dealerHand);
        playerHandLabel.setText("Your hand: " + Hand.name(playerHand));
        dealerHandLabel.setText("Dealer: " + Hand.name(dealerHand));
        if (p > d) {
            chips += ANTE * 2;
            statusLabel.setText("You win! +" + (ANTE * 2));
        } else if (p < d) {
            statusLabel.setText("Dealer wins.");
        } else {
            chips += ANTE;
            statusLabel.setText("Push — ante returned.");
        }
        updateChips();
        dealButton.setDisable(chips < ANTE);
        drawButton.setDisable(true);
    }

    private void toggleHold(int i) {
        if (!roundActive) return;
        held[i] = !held[i];
        refreshCards();
    }

    private void refreshCards() {
        for (int i = 0; i < 5; i++) {
            Button btn = playerCardButtons.get(i);
            if (!roundActive && playerHand.isEmpty()) {
                btn.setText("?");
                btn.setStyle("");
                continue;
            }
            Card c = playerHand.get(i);
            btn.setText(c.text());
            boolean red = c.suit == Suit.HEARTS || c.suit == Suit.DIAMONDS;
            btn.setStyle(held[i]
                    ? "-fx-background-color: #ffeb3b; -fx-text-fill: #000;"
                    : "-fx-background-color: #fff; -fx-text-fill: " + (red ? "#c62828;" : "#000;"));
        }
    }

    private void showDealerHidden() {
        dealerCardsBox.getChildren().clear();
        for (int i = 0; i < 5; i++) {
            Label back = new Label("?");
            back.setPrefSize(70, 100);
            back.setAlignment(Pos.CENTER);
            back.setStyle("-fx-background-color: #1565c0; -fx-text-fill: white;");
            dealerCardsBox.getChildren().add(back);
        }
    }

    private void showDealerCards() {
        dealerCardsBox.getChildren().clear();
        for (Card c : dealerHand) {
            Label lbl = new Label(c.text());
            lbl.setPrefSize(70, 100);
            lbl.setAlignment(Pos.CENTER);
            lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
            boolean red = c.suit == Suit.HEARTS || c.suit == Suit.DIAMONDS;
            lbl.setStyle("-fx-background-color: white; -fx-text-fill: " + (red ? "#c62828;" : "#000;"));
            dealerCardsBox.getChildren().add(lbl);
        }
    }

    private void resetChips() {
        chips = STARTING_CHIPS;
        roundActive = false;
        playerHand.clear();
        dealerHand.clear();
        for (int i = 0; i < 5; i++) held[i] = false;
        refreshCards();
        showDealerHidden();
        playerHandLabel.setText("");
        dealerHandLabel.setText("Dealer: ???");
        updateChips();
        dealButton.setDisable(false);
        drawButton.setDisable(true);
        statusLabel.setText("Chips reset. Press Deal.");
    }

    private void updateChips() {
        chipsLabel.setText("Chips: " + chips);
    }

    public static void main(String[] args) {
        launch(args);
    }

    // --- cards & deck ---

    enum Suit { CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠");
        final String sym;
        Suit(String sym) { this.sym = sym; }
    }

    enum Rank {
        TWO(2,"2"), THREE(3,"3"), FOUR(4,"4"), FIVE(5,"5"), SIX(6,"6"),
        SEVEN(7,"7"), EIGHT(8,"8"), NINE(9,"9"), TEN(10,"T"),
        JACK(11,"J"), QUEEN(12,"Q"), KING(13,"K"), ACE(14,"A");
        final int v;
        final String lbl;
        Rank(int v, String lbl) { this.v = v; this.lbl = lbl; }
    }

    static class Card {
        final Rank rank;
        final Suit suit;
        Card(Rank rank, Suit suit) { this.rank = rank; this.suit = suit; }
        String text() { return rank.lbl + suit.sym; }
    }

    static class Deck {
        private final List<Card> cards = new ArrayList<>();
        void reset() {
            cards.clear();
            for (Suit s : Suit.values())
                for (Rank r : Rank.values())
                    cards.add(new Card(r, s));
            Collections.shuffle(cards);
        }
        Card draw() { return cards.remove(cards.size() - 1); }
    }

    // --- hand ranking ---

    static class Hand {
        static int score(List<Card> hand) {
            int[] v = hand.stream().mapToInt(c -> c.rank.v).sorted().toArray();
            boolean flush = hand.stream().map(c -> c.suit).distinct().count() == 1;
            boolean straight = straight(v);
            Map<Integer, Long> counts = hand.stream()
                    .collect(Collectors.groupingBy(c -> c.rank.v, Collectors.counting()));
            List<Integer> g = counts.entrySet().stream()
                    .sorted((a, b) -> {
                        int c = Long.compare(b.getValue(), a.getValue());
                        return c != 0 ? c : Integer.compare(b.getKey(), a.getKey());
                    }).map(Map.Entry::getKey).toList();

            if (flush && straight) return 8_000_000 + high(v);
            if (counts.containsValue(4L)) return 7_000_000 + g.get(0) * 100 + g.get(1);
            if (counts.containsValue(3L) && counts.containsValue(2L)) return 6_000_000 + g.get(0) * 100 + g.get(1);
            if (flush) return 5_000_000 + tie(v);
            if (straight) return 4_000_000 + high(v);
            if (counts.containsValue(3L)) return 3_000_000 + g.get(0) * 10_000 + g.get(1);
            if (counts.values().stream().filter(n -> n == 2).count() == 2)
                return 2_000_000 + g.get(0) * 10_000 + g.get(1) * 100 + g.get(2);
            if (counts.containsValue(2L))
                return 1_000_000 + g.get(0) * 10_000 + g.stream().skip(1).max(Integer::compareTo).orElse(0);
            return tie(v);
        }

        static String name(List<Card> hand) {
            int[] v = hand.stream().mapToInt(c -> c.rank.v).sorted().toArray();
            boolean flush = hand.stream().map(c -> c.suit).distinct().count() == 1;
            boolean straight = straight(v);
            Map<Integer, Long> counts = hand.stream()
                    .collect(Collectors.groupingBy(c -> c.rank.v, Collectors.counting()));
            if (flush && straight) return v[4] == Rank.ACE.v ? "Royal Flush" : "Straight Flush";
            if (counts.containsValue(4L)) return "Four of a Kind";
            if (counts.containsValue(3L) && counts.containsValue(2L)) return "Full House";
            if (flush) return "Flush";
            if (straight) return "Straight";
            if (counts.containsValue(3L)) return "Three of a Kind";
            if (counts.values().stream().filter(n -> n == 2).count() == 2) return "Two Pair";
            if (counts.containsValue(2L)) return "One Pair";
            return "High Card";
        }

        private static boolean straight(int[] s) {
            if (s[0] == 2 && s[1] == 3 && s[2] == 4 && s[3] == 5 && s[4] == Rank.ACE.v) return true;
            for (int i = 1; i < 5; i++) if (s[i] != s[i - 1] + 1) return false;
            return true;
        }

        private static int high(int[] s) {
            return (s[0] == 2 && s[4] == Rank.ACE.v) ? 5 : s[4];
        }

        private static int tie(int[] s) {
            int t = 0;
            for (int i = 4; i >= 0; i--) t = t * 15 + s[i];
            return t;
        }
    }
}
