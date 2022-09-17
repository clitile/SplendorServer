package nz.proj;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetSocket;

public class Player {
    private String name;
    private ServerWebSocket socket;

    public Player(String name, ServerWebSocket socket) {
        this.name = name;
        this.socket = socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServerWebSocket getSocket() {
        return socket;
    }

    public void setSocket(ServerWebSocket socket) {
        this.socket = socket;
    }
}
