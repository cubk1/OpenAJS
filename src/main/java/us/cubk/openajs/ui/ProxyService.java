package us.cubk.openajs.ui;

import lombok.Getter;
import us.cubk.openajs.api.AjsClient;
import us.cubk.openajs.api.fv.FvConnection;
import us.cubk.openajs.api.fv.SocksProxyServer;
import us.cubk.openajs.api.model.VpnServer;

public class ProxyService {

    public enum State {STOPPED, CONNECTING, CONNECTED, ERROR}

    @Getter
    private volatile State state = State.STOPPED;
    @Getter
    private volatile String message = "";
    @Getter
    private volatile VpnServer server;
    @Getter
    private volatile int localPort;

    private FvConnection connection;
    private SocksProxyServer socks;
    private Thread serveThread;

    public synchronized void start(AjsClient client, VpnServer server, int localPort) throws Exception {
        stop();
        this.server = server;
        this.localPort = localPort;
        this.state = State.CONNECTING;
        this.message = "authenticating " + server.getSvr() + ":" + server.port();

        FvConnection conn = new FvConnection(server.getSvr(), server.port(), client.getUserName(), client.getConnectPassword(), client.getDevice());
        if (!conn.authenticate(15000)) {
            this.state = State.ERROR;
            this.message = "authentication rejected by server";
            throw new IllegalStateException(message);
        }
        this.connection = conn;
        this.socks = new SocksProxyServer(conn, localPort);
        this.serveThread = new Thread(() -> {
            try {
                socks.serve();
            } catch (Exception ignored) {
            }
        });
        this.serveThread.setDaemon(true);
        this.serveThread.start();
        this.state = State.CONNECTED;
        this.message = "SOCKS5 127.0.0.1:" + localPort + " via " + server.getTit();
    }

    public synchronized void stop() {
        if (socks != null) {
            socks.stop();
            socks = null;
        }
        connection = null;
        state = State.STOPPED;
        message = "";
    }

    public String testExitIp() throws Exception {
        if (connection == null) {
            throw new IllegalStateException("not connected");
        }
        String request = "GET /ip HTTP/1.1\r\nHost: httpbin.org\r\nUser-Agent: OpenAJS\r\nConnection: close\r\n\r\n";
        String response = connection.proxyHttp("httpbin.org", 80, request, 10000);
        int origin = response.indexOf("\"origin\"");
        return origin >= 0 ? response.substring(origin) : response;
    }

    public boolean isConnected() {
        return state == State.CONNECTED;
    }
}
