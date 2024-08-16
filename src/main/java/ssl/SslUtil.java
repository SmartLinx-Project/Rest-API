package ssl;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

public class SslUtil {
    public static SocketFactory getTruststoreFactory(String keystorePath, String keystorePassword) throws Exception {

        KeyStore trustStore = KeyStore.getInstance("JKS");
        InputStream in = new FileInputStream(keystorePath);
        trustStore.load(in, keystorePassword.toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(null, tmf.getTrustManagers(), null);
        return sslCtx.getSocketFactory();
    }
}
