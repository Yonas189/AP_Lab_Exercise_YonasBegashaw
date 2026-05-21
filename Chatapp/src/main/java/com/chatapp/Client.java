package com.chatapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Run: mvn javafx:run
 */
public class Client extends Application {

    private TextField userField, partnerField, messageField;
    private TextArea chatArea;
    private ListView<String> userList;
    private Label statusLabel;
    private Button connectBtn, sendBtn;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String myName, partner;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        userField = new TextField();
        connectBtn = new Button("Connect");
        statusLabel = new Label("Not connected");
        statusLabel.setTextFill(Color.DARKRED);

        userList = new ListView<>();
        userList.setPrefHeight(90);
        userList.getSelectionModel().selectedItemProperty().addListener((o, a, sel) -> {
            if (sel != null) openChat(sel);
        });

        partnerField = new TextField();
        partnerField.setPromptText("other user e.g. bob");
        Button chatBtn = new Button("Chat");
        chatBtn.setOnAction(e -> {
            String p = partnerField.getText().trim();
            if (!p.isEmpty()) openChat(p);
        });

        chatArea = new TextArea();
        chatArea.setEditable(false);
        messageField = new TextField();
        sendBtn = new Button("Send");
        sendBtn.setDisable(true);
        messageField.setDisable(true);

        connectBtn.setOnAction(e -> connect());
        sendBtn.setOnAction(e -> send());
        messageField.setOnAction(e -> send());

        VBox left = new VBox(8, new Label("Online:"), userList,
                new Label("Or type name:"), partnerField, chatBtn);
        left.setPrefWidth(150);

        VBox root = new VBox(10,
                new HBox(10, new Label("You:"), userField, connectBtn),
                statusLabel,
                new HBox(15, left, new VBox(8, chatArea, new HBox(10, messageField, sendBtn))));
        HBox.setHgrow(root.getChildren().get(2), Priority.ALWAYS);
        root.setPadding(new Insets(12));

        stage.setTitle("Chat App");
        stage.setScene(new Scene(root, 600, 450));
        stage.setOnCloseRequest(e -> { disconnect(); Platform.exit(); });
        stage.show();
    }

    private void connect() {
        myName = userField.getText().trim();
        if (myName.isEmpty()) {
            alert("Enter your username");
            return;
        }
        connectBtn.setDisable(true);
        userField.setDisable(true);

        new Thread(() -> {
            try {
                socket = new Socket("localhost", 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out.println("LOGIN " + myName);

                boolean ok = false;
                for (int i = 0; i < 5; i++) {
                    String line = in.readLine();
                    if (line == null) throw new IOException("Server closed");
                    if (line.startsWith("ERROR ")) throw new IOException(line.substring(6));
                    if (line.startsWith("OK ")) { ok = true; break; }
                }
                if (!ok) throw new IOException("No OK from server");

                new Thread(this::listen, "listener").start();

                Platform.runLater(() -> {
                    statusLabel.setText("Connected");
                    statusLabel.setTextFill(Color.DARKGREEN);
                    chatArea.setText("Connected as " + myName + ".\n"
                            + "Open another window as a different user to chat.\n");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setText("Failed");
                    statusLabel.setTextFill(Color.DARKRED);
                    alert("Cannot connect.\n" + ex.getMessage()
                            + "\n\nStart server first:\nmvn compile exec:java@server");
                    connectBtn.setDisable(false);
                    userField.setDisable(false);
                });
            }
        }).start();
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("USERS ")) {
                    List<String> users = new ArrayList<>(Arrays.asList(line.substring(6).split(",")));
                    users.removeIf(u -> u.isEmpty() || u.equalsIgnoreCase(myName));
                    Platform.runLater(() -> userList.setItems(FXCollections.observableArrayList(users)));
                } else if (line.startsWith("CHAT ")) {
                    partner = line.substring(5).trim();
                    Platform.runLater(() -> {
                        chatArea.clear();
                        sendBtn.setDisable(false);
                        messageField.setDisable(false);
                    });
                } else if (line.startsWith("HISTORY ")) {
                    String msg = line.substring(8);
                    Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                } else if (line.startsWith("PRIV ")) {
                    handleMsg(line.substring(5), false);
                } else if (line.startsWith("SENT ")) {
                    handleMsg(line.substring(5), true);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void handleMsg(String payload, boolean sent) {
        int t = payload.indexOf('\t');
        if (t < 1) return;
        String who = payload.substring(0, t);
        String text = payload.substring(t + 1);
        if (!who.equals(partner)) return;
        Platform.runLater(() -> chatArea.appendText((sent ? "You: " : who + ": ") + text + "\n"));
    }

    private void openChat(String name) {
        if (out == null) return;
        partner = name;
        partnerField.setText(name);
        chatArea.clear();
        out.println("OPEN " + name);
    }

    private void send() {
        if (partner == null || partner.isEmpty()) {
            alert("Pick a user or type a name and click Chat");
            return;
        }
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        out.println("SEND " + partner + "\t" + text);
        messageField.clear();
    }

    private void disconnect() {
        try {
            if (out != null) out.println("/quit");
            if (socket != null) socket.close();
        } catch (Exception ignored) { }
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
