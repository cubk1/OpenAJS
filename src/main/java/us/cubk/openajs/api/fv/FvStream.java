package us.cubk.openajs.api.fv;

import javax.crypto.Cipher;
import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FvStream implements Closeable {

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Cipher send;
    private final Cipher recv;

    public FvStream(Socket socket, InputStream in, OutputStream out, Cipher send, Cipher recv) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.send = send;
        this.recv = recv;
    }

    public void write(byte[] data, int length) throws Exception {
        out.write(send.update(data, 0, length));
        out.flush();
    }

    public int read(byte[] buffer) throws Exception {
        int read = in.read(buffer);
        if (read <= 0) {
            return read;
        }
        byte[] decrypted = recv.update(buffer, 0, read);
        System.arraycopy(decrypted, 0, buffer, 0, decrypted.length);
        return decrypted.length;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}
