import com.almasb.fxgl.core.serialization.Bundle;
import nz.proj.Server;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class TestServer {
    static ServerTestClient client1;
    static ServerTestClient client2;

    @BeforeAll
    public static void init() {
        Server.main(new String[1]);
        try {
            client1 = new ServerTestClient(new URI("ws://localhost:10100/websocket"));

            client2 = new ServerTestClient(new URI("ws://localhost:10100/websocket"));
//            client2.connect();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        if (client1.getReadyState().equals(ReadyState.NOT_YET_CONNECTED)) {
            try {
                client1.connect();
            } catch (IllegalStateException e) {
            }
        } else if (client1.getReadyState().equals(ReadyState.CLOSING) || client1.getReadyState().equals(ReadyState.CLOSED)) {
            client1.reconnect();
        }

        if (client2.getReadyState().equals(ReadyState.NOT_YET_CONNECTED)) {
            try {
                client2.connect();
            } catch (IllegalStateException e) {
            }
        } else if (client2.getReadyState().equals(ReadyState.CLOSING) || client2.getReadyState().equals(ReadyState.CLOSED)) {
            client2.reconnect();
        }
    }

    @AfterEach
    void tearDown() {
        client1.close();
        client2.close();
    }

    @Test
    void testLogin1() {
        Bundle b = new Bundle("login");
        b.put("name", "777");
        b.put("pwd", "777");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(b).getBytes());
    }

    @Test
    void testLogin2() {
        Bundle b = new Bundle("login");
        b.put("name", "888");
        b.put("pwd", "888");
        while (!client2.isOpen()) {

        }
        client2.send(Server.bundle2Buffer(b).getBytes());
    }

    @Test
    void testBundle_Buffer() {
        Bundle b = new Bundle("test");
        b.put("test", "test");
        var buffer = Server.bundle2Buffer(b);
        var bundle = Server.buffer2Bundle(buffer);
        assertEquals(bundle.getName(), b.getName());
        assertEquals(bundle.get("test").toString(), b.get("test").toString());
    }

    @Test
    void testBase64_String() {
        String pwd = "rrwrwer";
        String base64 = Server.string2Base64(pwd);
        assertEquals(pwd, Server.base642String(base64));
    }

    @Test
    void testSignup() {
        Bundle sign_up = new Bundle("signup");
        sign_up.put("name", "asd");
        sign_up.put("pwd", "asd");
        sign_up.put("acc", "asd");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(sign_up).getBytes());
    }

    @Test
    void testReset() {
        Bundle re = new Bundle("reset");
        re.put("name", "asd");
        re.put("pwd", "uio");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(re).getBytes());
    }

    @Test
    void testAct() {
        Bundle b = new Bundle("login");
        b.put("name", "777");
        b.put("pwd", "777");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(b).getBytes());

        Bundle b1 = new Bundle("login");
        b.put("name", "888");
        b.put("pwd", "888");
        while (!client2.isOpen()) {

        }
        client2.send(Server.bundle2Buffer(b1).getBytes());

        Bundle match = new Bundle("match");
        match.put("mode", Integer.toString(2));
        match.put("name", "777");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(match).getBytes());

        Bundle match1 = new Bundle("match");
        match1.put("mode", Integer.toString(2));
        match1.put("name", "888");
        while (!client2.isOpen()) {

        }
        client2.send(Server.bundle2Buffer(match1).getBytes());

        Bundle act = new Bundle("act");
        act.put("name", "777");
        act.put("x", "0");
        act.put("y", "0");
        act.put("id", 1);
        act.put("activity", "getTwoSameCoin");
        client1.send(Server.bundle2Buffer(act).getBytes());

        Bundle roundOver = new Bundle("roundOver");
        roundOver.put("name", "777");
        roundOver.put("x", "0");
        roundOver.put("y", "0");
        roundOver.put("id", 1);
        roundOver.put("activity", "getTwoSameCoin");
        client1.send(Server.bundle2Buffer(roundOver).getBytes());

        Bundle stop = new Bundle("roomStop");
        stop.put("name", "777");
        client1.send(Server.bundle2Buffer(stop).getBytes());
    }

    @Test
    void testClose() {
        Bundle b = new Bundle("close");
        b.put("name", "777");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(b).getBytes());
    }

    @Test
    void testCancel() {
        Bundle match = new Bundle("match");
        match.put("mode", Integer.toString(2));
        match.put("name", "777");
        while (!client1.isOpen()) {

        }
        client1.send(Server.bundle2Buffer(match).getBytes());

        Bundle b = new Bundle("cancel");
        b.put("name", "777");
        b.put("mode", Integer.toString(2));
        client1.send(Server.bundle2Buffer(b).getBytes());
    }
}
