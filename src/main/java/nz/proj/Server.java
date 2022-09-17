package nz.proj;


import com.almasb.fxgl.core.serialization.Bundle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Server {
    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        MySQLConnectOptions sqlConnOption = new MySQLConnectOptions()
                .setHost("20.239.88.211")
                .setPort(3306)
                .setDatabase("players")
                .setUser("root")
                .setPassword("158356Proj.");
        SqlClient sqlClient = MySQLPool.client(sqlConnOption, new PoolOptions().setMaxSize(5));

        Map<String, ArrayList<Player>> waiting = new HashMap<>();
        ArrayList<Room> rooms = new ArrayList<>();
        Random seed = new Random();
        var server = vertx.createHttpServer().webSocketHandler(websocket -> {
            System.out.println("connected");
            websocket.handler(buffer -> {
                Bundle message = buffer2Bundle(buffer);
                if (message.getName().equals("login")) {
                    sqlClient.query("select * from players.playersInfo where name = '%s' and password = '%s'".formatted(message.get("name"), message.get("pwd")))
                            .execute(tab -> accountAct(websocket, "login", tab));
                } else if (message.getName().equals("signup")) {
                    int[] max = new int[1];
                    sqlClient.query("select max(id) from players.playersInfo")
                            .execute(tab -> tab.result().forEach(row -> max[0] = row.getInteger("max(id)")));
                    sqlClient.query("insert into players.playersInfo values (%d, %s, %s, %d)".formatted(max[0], message.get("name"), message.get("pwd"), (int) message.get("acc")))
                            .execute(tab -> accountAct(websocket, "signup", tab));
                } else if (message.getName().equals("reset")) {
                    sqlClient.query("UPDATE players.playersInfo SET password = '%s' WHERE name = '%s'".formatted(message.get("pwd"), message.get("name")))
                            .execute(tab -> accountAct(websocket, "reset", tab));
                } else if (message.getName().equals("match")) {
                    System.out.println(message);
                    String mode = message.get("mode");
                    System.out.println(mode);
                    if (waiting.containsKey(mode)) {
                        waiting.get(mode).add(new Player(message.get("name"), websocket));
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
                                socket.write(bundle2Buffer(bundle));
                            }
                        }
                    } else {
                        waiting.put(mode, new ArrayList<>(){{add(new Player(message.get("name"), websocket));}});
                    }
                } else if (message.getName().equals("act")) {
                    Room room = rooms.get((int)message.get("id") - 1);
                    for (Player p : room.getPlayers()) {
                        if (!p.getName().equals(message.get("name"))) {
                            p.getSocket().write(bundle2Buffer(message));
                        }
                    }
                } else if (message.getName().equals("roundOver")) {
                    Room room = rooms.get((int)message.get("id") - 1);
                    Bundle bundle = new Bundle("nextRound");
                    bundle.put("id", message.get("id"));
                    bundle.put("next", room.getNextName(message.get("name")));
                    for (Player p : room.getPlayers()) {
                        if (!p.getName().equals(message.get("name"))) {
                            p.getSocket().write(bundle2Buffer(message));
                        }
                    }
                } else if (message.getName().equals("close")) {
                    String name = message.get("name");
                    websocket.closeHandler(h -> {
                        int temp = 0;
                        for (ArrayList<Player> players : waiting.values()) {
                            for (Player player : players) {
                                if (player.getName().equals(name)) {
                                    temp = 1;
                                    System.out.println(player.getName() + "log out");
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
                                        break;
                                    }
                                }
                            }
                            if (temp == 1) {
                                break;
                            }
                        }
                    });
                    websocket.close();
                }
            });
        });
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

    public static void accountAct(ServerWebSocket socket, String act, AsyncResult<RowSet<Row>> tab) {
        if (tab.succeeded() && tab.result().size() != 0) {
            socket.write(bundle2Buffer(new Bundle(act)));
        } else {
            socket.write(bundle2Buffer(new Bundle("false")));
        }
    }
}
