package notepad;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Model: read/write .txt files (no JavaFX here). */
public class FileService {

    public String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public void write(Path path, String text) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }
}
