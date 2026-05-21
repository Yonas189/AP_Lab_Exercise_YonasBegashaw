package notepad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/** Main entry — loads FXML and starts the window. */
public class NotepadApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/notepad.fxml"), "FXML missing"));
        Parent root = loader.load();

        NotepadController controller = loader.getController();
        Scene scene = new Scene(root, 900, 600);
        controller.setStage(stage);
        controller.bindShortcuts(scene);

        stage.setTitle("Notepad");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
