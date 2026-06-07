package us.cubk.openajs.api.fv;

import us.cubk.openajs.api.AjsDevice;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

public class FvConnection {

    private final String serverIp;
    private final int serverPort;
    private final String userName;
    private final String connectPassword;
    private final AjsDevice device;
    private final SecureRandom random = new SecureRandom();

    private byte[] sessionKey;
    private byte[] bodyIv;
    private byte[] headerIv;
    private String sid;
    private Socket controlSocket;

    public FvConnection(String serverIp, int serverPort, String userName, String connectPassword, AjsDevice device) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.userName = userName;
        this.connectPassword = connectPassword;
        this.device = device;
    }

    public Map<String, String> authFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("t", Long.toString(System.currentTimeMillis() / 1000L));
        fields.put("c", "au");
        fields.put("u", userName);
        fields.put("p", connectPassword);
        fields.put("cuid", device.getDeviceId());
        fields.put("cseid", device.getDeviceId());
        fields.put("st", device.getSite());
        fields.put("v", device.getAppVersion());
        fields.put("cv", "20260525-1");
        fields.put("osp", device.getPlatform());
        fields.put("osd", device.getOsDevice());
        fields.put("osv", device.getOsVersion());
        fields.put("dim", device.getOsDim());
        fields.put("ce", "");
        return fields;
    }

    public String buildBody(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(entry.getKey()).append('=').append(entry.getValue().replace('\n', ' ')).append('\n');
        }
        return sb.toString();
    }

    public boolean authenticate(int timeoutMs) throws Exception {
        sessionKey = new byte[16];
        bodyIv = new byte[16];
        headerIv = new byte[16];
        random.nextBytes(sessionKey);
        random.nextBytes(bodyIv);
        random.nextBytes(headerIv);

        byte[] body = aesCtr(sessionKey, bodyIv, buildBody(authFields()).getBytes(StandardCharsets.UTF_8));
        String text = "s=\nn=" + hex(bodyIv) + "\nk=" + base64(oaepBlock(sessionKey)) + "\nl=" + body.length + "\n_=" + randomPad();
        byte[] frame = buildFrame(headerIv, text);

        controlSocket = new Socket();
        controlSocket.connect(new InetSocketAddress(serverIp, serverPort), timeoutMs);
        controlSocket.setSoTimeout(timeoutMs);
        OutputStream os = controlSocket.getOutputStream();
        os.write(concat(frame, body));
        os.flush();

        byte[] response = readAvailable(controlSocket.getInputStream(), timeoutMs);
        Map<String, String> fields = decodeResponseFields(response, headerIv, sessionKey, bodyIv);
        this.sid = fields.get("sid");
        return "ok".equals(fields.get("r"));
    }

    public String proxyHttp(String host, int port, String httpRequest, int timeoutMs) throws Exception {
        byte[] div = new byte[16];
        byte[] hiv = new byte[16];
        random.nextBytes(div);
        random.nextBytes(hiv);
        String text = "s=" + sid + "\nn=" + hex(div) + "\nk=\n_=" + randomPad();
        byte[] frame = buildFrame(hiv, text);

        Cipher send = ctrCipher(sessionKey, div);
        Cipher recv = ctrCipher(sessionKey, div);

        byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
        byte[] socks = new byte[5 + hostBytes.length + 2];
        socks[0] = 5;
        socks[1] = 1;
        socks[2] = 0;
        socks[3] = 3;
        socks[4] = (byte) hostBytes.length;
        System.arraycopy(hostBytes, 0, socks, 5, hostBytes.length);
        socks[5 + hostBytes.length] = (byte) (port >> 8);
        socks[6 + hostBytes.length] = (byte) port;
        byte[] socksEnc = send.update(socks);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverIp, serverPort), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            OutputStream os = socket.getOutputStream();
            os.write(concat(frame, socksEnc));
            os.flush();

            InputStream is = socket.getInputStream();
            byte[] resp = readAvailable(is, timeoutMs);
            byte[] headerDec = aesCtr(hiv, new byte[16], resp);
            int textLen = ((headerDec[2] & 0xFF) << 8) | (headerDec[3] & 0xFF);
            int bodyStart = 4 + textLen;
            StringBuilder out = new StringBuilder();
            out.append("dataHeader=").append(new String(headerDec, 4, Math.max(0, Math.min(textLen, headerDec.length - 4)), StandardCharsets.UTF_8)).append('\n');
            if (bodyStart < resp.length) {
                byte[] enc = new byte[resp.length - bodyStart];
                System.arraycopy(resp, bodyStart, enc, 0, enc.length);
                byte[] dec = recv.update(enc);
                out.append("socksReply[0:2]=").append(String.format("%02x %02x", dec[0], dec[1])).append('\n');
            }

            byte[] httpEnc = send.update(httpRequest.getBytes(StandardCharsets.UTF_8));
            os.write(httpEnc);
            os.flush();

            ByteArrayOutputStream acc = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[8192];
                while (acc.size() < 65536) {
                    int r = is.read(buf);
                    if (r <= 0) {
                        break;
                    }
                    acc.write(recv.update(buf, 0, r));
                }
            } catch (SocketTimeoutException ignored) {
            }
            out.append("httpResponse=\n").append(new String(acc.toByteArray(), StandardCharsets.UTF_8));
            return out.toString();
        }
    }

    public FvStream openStream(String host, int port, int timeoutMs) throws Exception {
        byte[] div = new byte[16];
        byte[] hiv = new byte[16];
        random.nextBytes(div);
        random.nextBytes(hiv);
        String text = "s=" + sid + "\nn=" + hex(div) + "\nk=\n_=" + randomPad();
        byte[] frame = buildFrame(hiv, text);
        Cipher send = ctrCipher(sessionKey, div);
        Cipher recv = ctrCipher(sessionKey, div);

        byte[] hostBytes = host.getBytes(StandardCharsets.UTF_8);
        byte[] socks = new byte[5 + hostBytes.length + 2];
        socks[0] = 5;
        socks[1] = 1;
        socks[2] = 0;
        socks[3] = 3;
        socks[4] = (byte) hostBytes.length;
        System.arraycopy(hostBytes, 0, socks, 5, hostBytes.length);
        socks[5 + hostBytes.length] = (byte) (port >> 8);
        socks[6 + hostBytes.length] = (byte) port;
        byte[] socksEnc = send.update(socks);

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(serverIp, serverPort), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        OutputStream os = socket.getOutputStream();
        os.write(concat(frame, socksEnc));
        os.flush();

        InputStream in = new java.io.BufferedInputStream(socket.getInputStream());
        Cipher headerCipher = ctrCipher(hiv, new byte[16]);
        byte[] head4 = readN(in, 4);
        byte[] head4Dec = headerCipher.update(head4);
        int textLen = ((head4Dec[2] & 0xFF) << 8) | (head4Dec[3] & 0xFF);
        String headerText = new String(headerCipher.update(readN(in, textLen)), StandardCharsets.UTF_8);
        if (!headerText.contains("code=200")) {
            socket.close();
            throw new java.io.IOException("data channel rejected: " + headerText.trim());
        }
        byte[] reply = recv.update(readN(in, 10));
        if (reply[0] != 5 || reply[1] != 0) {
            socket.close();
            throw new java.io.IOException("socks connect failed: rep=" + (reply[1] & 0xFF));
        }
        socket.setSoTimeout(0);
        return new FvStream(socket, in, os, send, recv);
    }

    private static byte[] readN(InputStream in, int n) throws Exception {
        byte[] buffer = new byte[n];
        int offset = 0;
        while (offset < n) {
            int read = in.read(buffer, offset, n - offset);
            if (read < 0) {
                throw new java.io.EOFException();
            }
            offset += read;
        }
        return buffer;
    }

    private Map<String, String> decodeResponseFields(byte[] response, byte[] hiv, byte[] ks, byte[] biv) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (response.length < 4) {
            return fields;
        }
        byte[] dec = aesCtr(hiv, new byte[16], response);
        int textLen = ((dec[2] & 0xFF) << 8) | (dec[3] & 0xFF);
        int bodyStart = 4 + textLen;
        if (bodyStart < response.length) {
            byte[] enc = new byte[response.length - bodyStart];
            System.arraycopy(response, bodyStart, enc, 0, enc.length);
            byte[] bodyPlain = aesCtr(ks, biv, enc);
            for (String line : new String(bodyPlain, StandardCharsets.UTF_8).split("\n")) {
                int eq = line.indexOf('=');
                if (eq > 0) {
                    fields.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        }
        return fields;
    }

    private byte[] buildFrame(byte[] hiv, String text) {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] plain = new byte[4 + textBytes.length];
        plain[2] = (byte) (textBytes.length >> 8);
        plain[3] = (byte) textBytes.length;
        System.arraycopy(textBytes, 0, plain, 4, textBytes.length);
        byte[] enc = aesCtr(hiv, new byte[16], plain);
        return concat(hiv, enc);
    }

    private byte[] oaepBlock(byte[] key) {
        byte[] ct = FvCrypto.rsaOaepEncrypt(key);
        byte[] block = new byte[4 + ct.length];
        block[0] = 'O';
        block[1] = 'A';
        block[2] = 'E';
        block[3] = 'P';
        System.arraycopy(ct, 0, block, 4, ct.length);
        return block;
    }

    private static byte[] readAvailable(InputStream is, int timeoutMs) throws Exception {
        ByteArrayOutputStream acc = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            while (acc.size() < 65536) {
                int r = is.read(buf);
                if (r <= 0) {
                    break;
                }
                acc.write(buf, 0, r);
                if (is.available() == 0) {
                    Thread.sleep(150);
                    if (is.available() == 0) {
                        break;
                    }
                }
            }
        } catch (SocketTimeoutException ignored) {
        }
        return acc.toByteArray();
    }

    private static Cipher ctrCipher(byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher;
    }

    private static byte[] aesCtr(byte[] key, byte[] iv, byte[] data) {
        return FvCrypto.aesCtr(key, iv, data);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private String randomPad() {
        int count = random.nextInt(200);
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    public String getSid() {
        return sid;
    }

    public void close() throws Exception {
        if (controlSocket != null) {
            controlSocket.close();
        }
    }

    private String base64(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    private String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
