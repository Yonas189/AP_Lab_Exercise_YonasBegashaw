package notepad;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
/** Controller: menus, shortcuts, dialogs, and status bar. */
public class NotepadController {

    private static final String TITLE = "Notepad";
    private static final FileChooser.ExtensionFilter TXT =
            new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");

    @FXML private TextArea textArea;
    @FXML private Label statusLabel;
    @FXML private MenuItem menuNew, menuOpen, menuSave, menuSaveAs, menuExit;
    @FXML private MenuItem menuCut, menuCopy, menuPaste;

    private Stage stage;
    private final FileService files = new FileService();
    private Path currentPath;
    private boolean modified;

    @FXML
    private void initialize() {
        textArea.textProperty().addListener((o, a, b) -> {
            if (!modified) {
                modified = true;
                updateStatus();
            }
        });
        menuNew.setOnAction(e -> onNew());
        menuOpen.setOnAction(e -> onOpen());
        menuSave.setOnAction(e -> onSave());
        menuSaveAs.setOnAction(e -> onSaveAs());
        menuExit.setOnAction(e -> onExit());
        menuCut.setOnAction(e -> textArea.cut());
        menuCopy.setOnAction(e -> textArea.copy());
        menuPaste.setOnAction(e -> textArea.paste());
        updateStatus();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        updateStatus();
    }

    public void bindShortcuts(Scene scene) {
        scene.getAccelerators().put(ctrl(KeyCode.N), this::onNew);
        scene.getAccelerators().put(ctrl(KeyCode.O), this::onOpen);
        scene.getAccelerators().put(ctrl(KeyCode.S), this::onSave);
    }

    private void onNew() {
        if (!askToSaveIfNeeded()) return;
        textArea.clear();
        currentPath = null;
        modified = false;
        updateStatus();
    }

    private void onOpen() {
        if (!askToSaveIfNeeded()) return;
        File file = chooser("Open").showOpenDialog(stage);
        if (file == null) return;
        try {
            textArea.setText(files.read(file.toPath()));
            currentPath = file.toPath();
            modified = false;
            updateStatus();
        } catch (IOException ex) {
            showError("Open failed", ex.getMessage());
        }
    }

    private void onSave() {
        if (currentPath == null) {
            onSaveAs();
        } else {
            saveTo(currentPath);
        }
    }

    private void onSaveAs() {
        File file = chooser("Save As").showSaveDialog(stage);
        if (file == null) return;
        saveTo(withTxt(file.toPath()));
    }

    private void onExit() {
        if (askToSaveIfNeeded()) Platform.exit();
    }

    private void saveTo(Path path) {
        try {
            files.write(path, textArea.getText());
            currentPath = path;
            modified = false;
            updateStatus();
        } catch (IOException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    private boolean askToSaveIfNeeded() {
        if (!modified) return true;
        ButtonType choice = new Alert(Alert.AlertType.CONFIRMATION,
                "Save changes before continuing?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
                .showAndWait().orElse(ButtonType.CANCEL);
        if (choice == ButtonType.YES) {
            if (currentPath == null) onSaveAs();
            else saveTo(currentPath);
            return !modified;
        }
        return choice == ButtonType.NO;
    }

    private FileChooser chooser(String title) {
        FileChooser c = new FileChooser();
        c.setTitle(title);
        c.getExtensionFilters().add(TXT);
        c.setSelectedExtensionFilter(TXT);
        if (currentPath != null) {
            File f = currentPath.toFile();
            if (f.getParentFile() != null) c.setInitialDirectory(f.getParentFile());
            c.setInitialFileName(f.getName());
        }
        return c;
    }

    private Path withTxt(Path path) {
        String name = path.getFileName().toString();
        return name.toLowerCase().endsWith(".txt") ? path : path.resolveSibling(name + ".txt");
    }

    private void updateStatus() {
        String status = currentPath == null ? "Untitled" : currentPath.toAbsolutePath().toString();
        if (modified) status += "*";
        statusLabel.setText(status);
        if (stage != null) {
            String window = TITLE + " - " + (currentPath == null ? "Untitled" : currentPath.getFileName());
            if (modified) window += "*";
            stage.setTitle(window);
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static KeyCombination ctrl(KeyCode key) {
        return new KeyCodeCombination(key, KeyCombination.CONTROL_DOWN);
    }
}
