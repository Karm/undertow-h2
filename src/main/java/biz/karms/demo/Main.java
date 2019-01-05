/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package biz.karms.demo;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.util.Headers;
import org.jboss.logmanager.LogManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * See: https://github.com/undertow-io/undertow/blob/master/examples/src/main/java/io/undertow/examples/http2/Http2Server.java
 *
 * @author Michal Karm Babacek <karm@redhat.com>
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger("H2");
    private static final String bindAddress = System.getProperty("bind.address", "localhost");
    private static final int bindPort = Integer.parseInt(System.getProperty("bind.port", "9443"));
    private static final char[] passphrase = System.getProperty("passphrase", "changeit").toCharArray();
    private static final String keystoreType = System.getProperty("keystore.type", "JKS");
    private static final String pathToKeystore = System.getProperty("keystore.path", "localhost.jks");
    private static final String pathToTrustStore = System.getProperty("truststore.path", "localhost.jks");

    public static void main(final String[] args) throws Exception {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        LogManager.getLogManager().readConfiguration(Main.class.getClassLoader().getResourceAsStream("logging.properties"));
        LOGGER.log(Level.INFO, "Constructing TLS context...");
        final SSLContext sslContext = createSSLContext(loadKeyStore(pathToKeystore), loadKeyStore(pathToTrustStore));
        LOGGER.log(Level.INFO, "Configuring server...");
        final Undertow server = Undertow.builder()
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .addHttpsListener(bindPort, bindAddress, sslContext)
                .setHandler(exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Hello, client!");
                }).build();
        try {
            server.start();
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, String.format("Could not start server on %s:%d", bindAddress, bindPort), e);
        }
        LOGGER.log(Level.INFO, String.format("Server started on %s:%d", bindAddress, bindPort));
    }

    private static KeyStore loadKeyStore(final String path) throws Exception {
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            final KeyStore loadedKeystore = KeyStore.getInstance(keystoreType);
            loadedKeystore.load(is, passphrase);
            return loadedKeystore;
        }
    }

    private static SSLContext createSSLContext(final KeyStore keyStore, final KeyStore trustStore) throws Exception {
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passphrase);
        final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        return sslContext;
    }
}
