package us.cubk.openajs.api.fv;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocksProxyServer {

    private final FvConnection connection;
    private final int port;
    private ServerSocket server;

    public SocksProxyServer(FvConnection connection, int port) {
        this.connection = connection;
        this.port = port;
    }

    public void serve() throws Exception {
        server = new ServerSocket();
        server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                Thread thread = new Thread(() -> handle(client));
                thread.setDaemon(true);
                thread.start();
            } catch (Exception e) {
                if (server.isClosed()) {
                    break;
                }
            }
        }
    }

    public void stop() {
        try {
            if (server != null) {
                server.close();
            }
            connection.close();
        } catch (Exception ignored) {
        }
    }

    private void handle(Socket client) {
        try {
            client.setTcpNoDelay(true);
            DataInputStream in = new DataInputStream(client.getInputStream());
            OutputStream out = client.getOutputStream();

            int version = in.readUnsignedByte();
            if (version != 5) {
                client.close();
                return;
            }
            int methods = in.readUnsignedByte();
            in.skipBytes(methods);
            out.write(new byte[]{5, 0});
            out.flush();

            in.readUnsignedByte();
            int cmd = in.readUnsignedByte();
            in.readUnsignedByte();
            int atyp = in.readUnsignedByte();
            String host;
            if (atyp == 1) {
                byte[] ip = new byte[4];
                in.readFully(ip);
                host = (ip[0] & 0xFF) + "." + (ip[1] & 0xFF) + "." + (ip[2] & 0xFF) + "." + (ip[3] & 0xFF);
            } else if (atyp == 3) {
                int len = in.readUnsignedByte();
                byte[] domain = new byte[len];
                in.readFully(domain);
                host = new String(domain, StandardCharsets.UTF_8);
            } else {
                byte[] ip6 = new byte[16];
                in.readFully(ip6);
                host = InetAddress.getByAddress(ip6).getHostAddress();
            }
            int targetPort = in.readUnsignedShort();

            if (cmd != 1) {
                out.write(new byte[]{5, 7, 0, 1, 0, 0, 0, 0, 0, 0});
                out.flush();
                client.close();
                return;
            }

            FvStream stream;
            try {
                stream = connection.openStream(host, targetPort, 15000);
            } catch (Exception e) {
                System.out.println("connect failed " + host + ":" + targetPort + " -> " + e.getMessage());
                out.write(new byte[]{5, 5, 0, 1, 0, 0, 0, 0, 0, 0});
                out.flush();
                client.close();
                return;
            }
            out.write(new byte[]{5, 0, 0, 1, 0, 0, 0, 0, 0, 0});
            out.flush();
            System.out.println("tunnel " + host + ":" + targetPort);
            relay(client, in, out, stream);
        } catch (Exception e) {
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void relay(Socket client, InputStream clientIn, OutputStream clientOut, FvStream stream) {
        Thread upstream = new Thread(() -> {
            try {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = clientIn.read(buffer)) > 0) {
                    stream.write(buffer, read);
                }
            } catch (Exception ignored) {
            } finally {
                stream.close();
            }
        });
        upstream.setDaemon(true);
        upstream.start();
        try {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                clientOut.write(buffer, 0, read);
                clientOut.flush();
            }
        } catch (Exception ignored) {
        } finally {
            stream.close();
            try {
                client.close();
            } catch (Exception ignored) {
            }
        }
    }
}
