package us.cubk.openajs.api.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class HttpClient {
    private final HttpURLConnection connection;

    public HttpClient(URL url) throws IOException {
        this.connection = (HttpURLConnection) url.openConnection();
        this.connection.setDoOutput(true);
        this.connection.setDoInput(true);
    }

    public HttpClient readTimeout(int timeout) {
        this.connection.setReadTimeout(timeout);
        return this;
    }

    public HttpClient connectTimeout(int timeout) {
        this.connection.setConnectTimeout(timeout);
        return this;
    }

    public void header(String key, String value) {
        this.connection.setRequestProperty(key, value);
    }

    public void post(byte[] b) throws IOException {
        this.connection.setRequestMethod("POST");

        try (OutputStream os = this.connection.getOutputStream()) {
            os.write(b);
        }
    }

    public void put(byte[] b) throws IOException {
        this.connection.setRequestMethod("PUT");

        try (OutputStream os = this.connection.getOutputStream()) {
            os.write(b);
        }
    }

    public void put(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        this.put(bytes);
    }

    public void post(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        this.post(bytes);
    }

    public void post(Map<String, String> map) throws IOException {
        StringJoiner sj = new StringJoiner("&");

        for (Entry<String, String> entry : map.entrySet()) {
            sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8"));
        }

        this.post(sj.toString());
    }

    public int responseCode() throws IOException {
        return this.connection.getResponseCode();
    }

    public String body() throws IOException {
        return new String(this.bBody(), StandardCharsets.UTF_8);
    }

    public byte[] bBody() throws IOException {
        InputStream inputStream = (responseCode() < 300) ? connection.getInputStream() : connection.getErrorStream();
        String encoding = connection.getContentEncoding();

        if (encoding != null) {
            if ("gzip".equalsIgnoreCase(encoding)) {
                inputStream = new GZIPInputStream(inputStream);
            } else if ("deflate".equalsIgnoreCase(encoding)) {
                inputStream = new InflaterInputStream(inputStream);
            }
        }


        byte[] var3;
        var3 = toByteArray(inputStream);

        return var3;
    }

    public void postAsync(String s) {
        CompletableFuture.runAsync(() -> {
            try {
                post(s);
                responseCode();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void postAsync(Map<String, String> map) {
        CompletableFuture.runAsync(() -> {
            try {
                post(map);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void disconnect() {
        this.connection.disconnect();
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy((InputStream) input, (OutputStream) output);
        return output.toByteArray();
    }

    public String finalUrl() {
        return this.connection.getURL().toString();
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        long count = copyLarge(input, output);
        return count > 2147483647L ? -1 : (int) count;
    }

    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        return copyLarge(input, output, new byte[4096]);
    }

    public static long copyLarge(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long count = 0L;

        int n;
        for (n = 0; -1 != (n = input.read(buffer)); count += (long) n) {
            output.write(buffer, 0, n);
        }

        return count;
    }

}
