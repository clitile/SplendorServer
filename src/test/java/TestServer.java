import com.almasb.fxgl.core.serialization.Bundle;
import nz.proj.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class TestServer {
    static ServerTestClient client;

    @BeforeAll
    public static void init() {
        Server.main(new String[1]);
        try {
            client = new ServerTestClient(new URI("ws://localhost:10100/websocket"));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void initEach() {
        if (!client.isOpen()) {
            client.connect();
        }
    }

    @Test
    void testLogin() {
        Bundle b = new Bundle("login");
        b.put("name", "555");
        b.put("pwd", "555");
        while (!client.isOpen()) {

        }
        client.send(Server.bundle2Buffer(b).getBytes());
    }

    @Test
    void testBundle_Buffer() {
        Bundle b = new Bundle("test");
        b.put("test", "test");
        var buffer = Server.bundle2Buffer(b);
        var bundle = Server.buffer2Bundle(buffer);
        Assertions.assertEquals(bundle.getName(), b.getName());
        Assertions.assertEquals(bundle.get("test").toString(), b.get("test").toString());
    }
}
