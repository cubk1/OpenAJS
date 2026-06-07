package us.cubk.openajs.api.net;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class InsecureTLS {

    private static boolean installed = false;

    private InsecureTLS() {
    }

    public static synchronized void enableGlobal() {
        if (installed) {
            return;
        }
        try {
            TrustManager[] trustAll = {new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAll, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
            HostnameVerifier allowAll = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allowAll);
            installed = true;
        } catch (Exception e) {
            throw new IllegalStateException("failed to install permissive TLS", e);
        }
    }
}
