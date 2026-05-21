# Chat App (simple)

Only **2 Java files**:
- `Server.java` — database + chat server
- `Client.java` — JavaFX window

## Setup

1. XAMPP: start **MySQL**
2. phpMyAdmin → run `database/schema.sql`
3. Check `server.properties` if needed

## Run

**Terminal 1 — server (keep open):**
```
mvn compile exec:java@server
```

**Terminal 2 — client:**
```
mvn javafx:run
```

**Second user:** run `mvn javafx:run` again with a different name.

## Chat

1. Connect as `yoni`
2. Open another window, connect as `bob`
3. Click `bob` in the list (or type `bob` → Chat)
4. Send messages

Messages save to MySQL on localhost. Check in phpMyAdmin: **chatapp → messages → Browse**.
