package com.chatapp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.*;

/**
 * Run: mvn compile exec:java@server
 */
public class Server {

    private static String dbUrl = "jdbc:mysql://localhost:3306/chatapp?useSSL=false&allowPublicKeyRetrieval=true";
    private static String dbUser = "root";
    private static String dbPass = "";
    private static int port = 5000;

    private final Map<String, Client> online = new LinkedHashMap<>();

    public static void main(String[] args) {
        new Server().run();
    }

    private void run() {
        try {
            loadProps();
            testDb();
            System.out.println("DATABASE OK - " + dbUrl);
        } catch (Exception e) {
            System.err.println("DATABASE FAILED: " + e.getMessage());
            System.err.println("Start MySQL, run database/schema.sql, check server.properties");
            return;
        }

        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("SERVER OK - localhost:" + port);
            while (true) {
                new Thread(new Client(ss.accept())).start();
            }
        } catch (Exception e) {
            System.err.println("SERVER FAILED: " + e.getMessage());
        }
    }

    private static void loadProps() throws IOException {
        Properties p = new Properties();
        File f = new File("server.properties");
        if (!f.exists()) f = new File(System.getProperty("user.dir"), "server.properties");
        p.load(new FileInputStream(f));
        dbUrl = p.getProperty("db.url", dbUrl);
        dbUser = p.getProperty("db.user", dbUser);
        dbPass = p.getProperty("db.password", dbPass);
        port = Integer.parseInt(p.getProperty("server.port", "5000"));
    }

    private static Connection db() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }

    private static void testDb() throws SQLException {
        try (Connection c = db()) { }
        try (Connection c = db();
             PreparedStatement ps = c.prepareStatement("SELECT sender FROM messages LIMIT 1")) {
            ps.executeQuery();
        }
    }

    private static void save(String from, String to, String text) throws SQLException {
        String sql = "INSERT INTO messages (sender, receiver, content) VALUES (?,?,?)";
        try (Connection c = db(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            ps.setString(3, text);
            ps.executeUpdate();
        }
    }

    private static List<String> history(String u1, String u2) throws SQLException {
        String sql = """
            SELECT sender, content, sent_at FROM messages
            WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?)
            ORDER BY id DESC LIMIT 50
            """;
        List<String> temp = new ArrayList<>();
        try (Connection c = db(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u1); ps.setString(2, u2);
            ps.setString(3, u2); ps.setString(4, u1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                temp.add("[" + rs.getTimestamp("sent_at") + "] "
                        + rs.getString("sender") + ": " + rs.getString("content"));
            }
        }
        Collections.reverse(temp);
        return temp;
    }

    private synchronized boolean login(String name, Client c) {
        if (online.containsKey(name)) return false;
        online.put(name, c);
        return true;
    }

    private synchronized void logout(Client c) {
        if (c.name != null) online.remove(c.name);
        sendUserListToAll();
    }

    private synchronized void sendUserListToAll() {
        String list = "USERS " + String.join(",", online.keySet());
        for (Client c : online.values()) c.send(list);
    }

    private synchronized void notifyOthers(Client except) {
        String list = "USERS " + String.join(",", online.keySet());
        for (Client c : online.values()) {
            if (c != except) c.send(list);
        }
    }

    private synchronized void sendPrivate(String from, String to, String text) throws Exception {
        save(from, to, text);
        Client r = online.get(to);
        if (r != null) r.send("PRIV " + from + "\t" + text);
        Client s = online.get(from);
        if (s != null) s.send("SENT " + to + "\t" + text);
    }

    private class Client implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private String name;

        Client(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                String line = in.readLine();
                if (line == null || !line.startsWith("LOGIN ")) {
                    send("ERROR Login first: LOGIN yourname");
                    return;
                }
                name = line.substring(6).trim();
                if (name.isEmpty() || !login(name, this)) {
                    send("ERROR Username empty or already online");
                    return;
                }

                send("OK Welcome " + name);
                send("USERS " + String.join(",", online.keySet()));
                notifyOthers(this);
                System.out.println(name + " online");

                while ((line = in.readLine()) != null) {
                    if (line.startsWith("/quit")) break;
                    if (line.startsWith("OPEN ")) {
                        String other = line.substring(5).trim();
                        send("CHAT " + other);
                        for (String h : history(name, other)) send("HISTORY " + h);
                    } else if (line.startsWith("SEND ")) {
                        int t = line.indexOf('\t');
                        if (t > 0) sendPrivate(name, line.substring(5, t).trim(), line.substring(t + 1).trim());
                    }
                }
            } catch (Exception e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                logout(this);
                try { socket.close(); } catch (IOException ignored) { }
            }
        }

        void send(String msg) { if (out != null) out.println(msg); }
    }
}
