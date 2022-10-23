package nz.proj;

import io.vertx.core.http.ServerWebSocket;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String mode;
    private ArrayList<Player> players = new ArrayList<>();

    private ArrayList<String> names = new ArrayList<>();

    private int id;
    public Room(String mode, int id, Player ...players) {
        this.players.addAll(List.of(players));
        this.mode = mode;
        this.id = id;
        for (Player player : players) {
            names.add(player.getName());
        }
    }

    public String getMode() {
        return mode;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ArrayList<String> getNames() {
        return names;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ArrayList<ServerWebSocket> getSockets() {
        ArrayList<ServerWebSocket> sockets = new ArrayList<>();
        for (Player p : players) {
            sockets.add(p.getSocket());
        }
        return sockets;
    }

    public Player getPlayerByName(String name) {
        for (Player p : this.players) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    public String getNextName(String thisRound) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(thisRound)) {
                return i + 1 < players.size() ? players.get(i + 1).getName() : players.get(0).getName();
            }
        }
        return null;
    }
}
