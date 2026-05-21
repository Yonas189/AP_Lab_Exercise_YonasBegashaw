# Simple Poker (JavaFX)

A small **5-card draw** poker game for a class project: you play against the dealer with chips and a simple UI.

## How to play

1. Press **Deal** (costs 25 chips).
2. Click your cards to **hold** them (yellow = held).
3. Press **Draw** to replace the cards you did not hold.
4. The dealer keeps a pair or better; otherwise they draw a new hand.
5. Better poker hand wins **50 chips**; a tie returns your ante.

## Requirements

- Java 17+
- Maven 3.6+

## Run

**Easiest (recommended):** double-click `run.bat` or run:

```bash
mvn javafx:run
```

### "JavaFX runtime components are missing"

That happens if you run `PokerApp` with plain Java (Run button) without JavaFX on the module path.

**Fix — pick one:**

1. Use `mvn javafx:run` or `run.bat` (always works).
2. **VS Code / Cursor:** run once `mvn compile`, then use the launch config **"Poker (with JavaFX)"** (F5).
3. **IntelliJ:** Run → Edit Configurations → VM options:
   ```
   --module-path target/javafx-lib --add-modules javafx.controls
   ```
   (Run `mvn compile` first so `target/javafx-lib` exists.)

## Project layout

```
src/main/java/
  PokerApp.java   ← entire game (UI + cards + hand scoring)
```
