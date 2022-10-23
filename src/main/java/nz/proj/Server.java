package nz.proj;


import com.almasb.fxgl.core.serialization.Bundle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.util.*;

public class Server {
    private static final Vertx vertx = Vertx.vertx();
    public static ArrayList<String> names = new ArrayList<>();

    public static HttpServer server = vertx.createHttpServer();
    public static ArrayList<Room> rooms = new ArrayList<>();

    public static void main(String[] args) {
        MySQLConnectOptions sqlConnOption = new MySQLConnectOptions()
                .setHost("20.239.88.211")
                .setPort(3306)
                .setDatabase("players")
                .setUser("root")
                .setPassword("158356Proj.");
        SqlClient sqlClient = MySQLPool.client(sqlConnOption, new PoolOptions().setMaxSize(5));

        Map<String, Player> online_players = new HashMap<>();
        Map<String, ArrayList<Player>> waiting = new HashMap<>();
//        ArrayList<Room> rooms = new ArrayList<>();
        Random seed = new Random();
        server.webSocketHandler(websocket -> websocket.handler(buffer -> {
            Bundle message = buffer2Bundle(buffer);
            if (message.getName().equals("login")) {
                if (names.contains((String) message.get("name"))) {
                    websocket.write(bundle2Buffer(new Bundle("false")));
                } else {
                    sqlClient.query("select * from players.playersInfo where name = '%s' and password = '%s'".formatted(message.get("name"), string2Base64(message.get("pwd"))))
                            .execute(tab -> {
                                if (tab.succeeded() && tab.result().size() != 0) {
                                    names.add(message.get("name"));
                                    online_players.put(message.get("name"), new Player(message.get("name"), websocket));
                                    websocket.write(bundle2Buffer(new Bundle("login")));
                                } else {
                                    websocket.write(bundle2Buffer(new Bundle("false")));
                                }
                            });
                }
            } else if (message.getName().equals("signup")) {
                sqlClient.query("select max(id) from players.playersInfo")
                        .execute()
                        .onComplete((AsyncResult<RowSet<Row>> ar) -> {
                            if (ar.succeeded()) {
                                ar.result().forEach(row -> {
                                    int max = row.getInteger("max(id)") + 1;
                                    System.out.println(max);
                                    sqlClient.query("insert into players.playersInfo values (%d, '%s', '%s', '%s')".formatted(max, message.get("name"), string2Base64(message.get("pwd")), message.get("acc")))
                                            .execute(tab -> websocket.write(bundle2Buffer(new Bundle("signup"))));
                                });
                            } else {
                                websocket.write(bundle2Buffer(new Bundle("false")));
                            }
                        });
            } else if (message.getName().equals("reset")) {
                sqlClient.query("UPDATE players.playersInfo SET password = '%s' WHERE name = '%s'".formatted(message.get("pwd"), message.get("name")))
                        .execute(tab -> websocket.write(bundle2Buffer(new Bundle("reset"))));
            } else if (message.getName().equals("match")) {
                System.out.println(message);
                String mode = message.get("mode");
                if (waiting.containsKey(mode)) {
                    waiting.get(mode).add(online_players.get(message.get("name").toString()));
                    if (waiting.get(mode).size() >= Integer.parseInt(mode)) {
                        Player[] players = new Player[Integer.parseInt(mode)];
                        for (int i = 0; i < Integer.parseInt(mode); i++) {
                            players[i] = waiting.get(mode).remove(0);
                        }
                        Room r = new Room(mode, rooms.size() + 1, players);
                        int seed_int = seed.nextInt();
                        rooms.add(r);
                        for (ServerWebSocket socket : r.getSockets()) {
                            Bundle bundle = new Bundle("matchFind");
                            bundle.put("id", r.getId());
                            bundle.put("players", r.getNames());
                            bundle.put("next", r.getNames().get(0));
                            bundle.put("seed", seed_int);
                            System.out.println("send " + bundle + " to Room " + r.getId());
                            socket.write(bundle2Buffer(bundle));
                        }
                    }
                } else {
                    waiting.put(mode, new ArrayList<>(){{add(online_players.get(message.get("name").toString()));}});
                }
            } else if (message.getName().equals("act")) {
                Room room = null;
                for (Room r : rooms) {
                    if (r.getId() == (int) message.get("id")) {
                        room = r;
                    }
                }
                assert room != null;
                for (Player p : room.getPlayers()) {
                    if (!p.getName().equals(message.get("name"))) {
                        System.out.println("send " + message + "to " + p.getName());
                        p.getSocket().write(bundle2Buffer(message));
                    }
                }
            } else if (message.getName().equals("roundOver")) {
                Room room = rooms.get((int)message.get("id") - 1);
                message.put("next", room.getNextName(message.get("name")));
                for (Player p : room.getPlayers()) {
                    if (!p.getName().equals(message.get("name"))) {
                        System.out.println("send " + message + " to " + p.getName());
                        p.getSocket().write(bundle2Buffer(message));
                    }
                }
            } else if (message.getName().equals("close")) {
                String name = message.get("name");
                System.out.println(message.get("name") + "log out");
                names.removeIf(s -> s.equals(name));
                online_players.remove(name);
                websocket.closeHandler(h -> {
                    int temp = 0;
                    for (ArrayList<Player> players : waiting.values()) {
                        for (Player player : players) {
                            if (player.getName().equals(name)) {
                                temp = 1;

                                players.remove(player);
                                break;
                            }
                        }
                        if (temp == 1) {
                            temp = 0;
                            break;
                        }
                    }

                    for (Room room : rooms) {
                        if (room.getNames().contains(name)) {
                            for (Player p : room.getPlayers()) {
                                if (!p.getName().equals(name)) {
                                    temp = 1;
                                    Bundle b = new Bundle("roomStop");
                                    p.getSocket().write(bundle2Buffer(b));
                                    rooms.remove(room);
                                }
                            }
                        }
                        if (temp == 1) {
                            break;
                        }
                    }
                });
                websocket.close();
            } else if (message.getName().equals("roomStop")) {
                int temp = 0;
                String name = message.getName();
                for (Room room : rooms) {
                    if (room.getNames().contains(name)) {
                        for (Player p : room.getPlayers()) {
                            if (!p.getName().equals(name)) {
                                temp = 1;
                                Bundle b = new Bundle("roomStop");
                                p.getSocket().write(bundle2Buffer(b));
                                rooms.remove(room);
                            }
                        }
                    }
                    if (temp == 1) {
                        break;
                    }
                }
            } else if (message.getName().equals("cancel")) {
                waiting.get(message.get("mode").toString()).removeIf(player -> player.getName().equals(message.get("name")));
            } else if (message.getName().equals("friends")) {
                String[] friends = message.get("friends");
                System.out.println(Arrays.toString(friends));
                if (names.containsAll(List.of(friends))) {
                    Player[] players = new Player[friends.length + 1];

                    for (int i = 0; i < friends.length; i++) {
                        players[i] = online_players.get(friends[i]);
                    }
                    players[friends.length] = online_players.get(message.get("name").toString());
                    Room r = new Room(Integer.toString(friends.length), rooms.size() + 1, players);
                    int seed_int = seed.nextInt();
                    rooms.add(r);
                    for (ServerWebSocket socket : r.getSockets()) {
                        Bundle bundle = new Bundle("matchFind");
                        bundle.put("id", r.getId());
                        bundle.put("players", r.getNames());
                        bundle.put("next", r.getNames().get(0));
                        bundle.put("seed", seed_int);
                        bundle.put("mode", players.length);
                        System.out.println("send " + bundle + " to Room " + r.getId());
                        socket.write(bundle2Buffer(bundle));
                    }
                } else {
                    System.out.println(false);
                    websocket.write(bundle2Buffer(new Bundle("false")));
                }
            }
        }));
        server.listen(10100, "0.0.0.0");
    }

    public static Bundle buffer2Bundle(Buffer buffer) {
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer.getBytes());
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (Bundle) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Buffer bundle2Buffer(Bundle bundle) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(bundle);
            return Buffer.buffer(bos.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String base642String(String baseInfo) {
        Base64 base = new Base64();
        return new String(base.decode(baseInfo));
    }

    public static String string2Base64(String info) {
        Base64 base = new Base64();
        return base.encodeToString(info.getBytes());
    }
}
