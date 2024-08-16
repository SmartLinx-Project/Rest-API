package rest;

import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import schedule.Schedule;

public class Main {
    static final String IP = "localhost";
    static final int PORT = 443;

    public static void main(String[] args) throws IOException {
        URI serverAddress = URI.create("https://" + IP + ":" + PORT + "/");

        // Configura il contesto SSL
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        sslContextConfig.setKeyStoreFile("resources/keystore.jks"); // Percorso del tuo keystore
        sslContextConfig.setKeyStorePass("...."); // Password del tuo keystore

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                serverAddress,
                new ResourceConfig(RestResources.class),
                true, // Abilita HTTPS
                new SSLEngineConfigurator(sslContextConfig).setClientMode(false).setNeedClientAuth(false)
        );
        server.start();
        System.out.println("Server avviato su " + serverAddress);

        Schedule.polling();
    }
}
