# JavaFX Notepad

```
Note/
├── src/
│   ├── notepad/          ← Java sources
│   └── notepad.fxml
├── pom.xml
└── run.ps1               ← mvn javafx:run
```

Requires **JDK 17+** and **Maven**.

## Run

```powershell
mvn javafx:run
```

Or:

```powershell
.\run.ps1
```

## IntelliJ / VS Code

Open the folder as a **Maven** project. Run with Maven goal `javafx:run`, or main class `notepad.NotepadApp` via the JavaFX plugin.

`target/` is build output (safe to delete; recreated on build).
