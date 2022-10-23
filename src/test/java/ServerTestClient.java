import com.almasb.fxgl.core.serialization.Bundle;
import io.vertx.core.buffer.Buffer;
import nz.proj.Server;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

public class ServerTestClient extends WebSocketClient {
    private boolean login = false;
    private boolean success = true;
    public ServerTestClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onMessage(ByteBuffer bytes) {
        Bundle mess = Server.buffer2Bundle(Buffer.buffer(bytes.array()));
        if (mess.getName().equals("login")) {
            this.login = true;
        } else if (mess.getName().equals("false")) {
            success = false;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {

    }

    @Override
    public void onError(Exception ex) {

    }

    public boolean isLogin() {
        return this.login;
    }

    public boolean isSuccess() {
        return this.success;
    }
}
