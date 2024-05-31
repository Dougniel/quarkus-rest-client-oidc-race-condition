package org.acme;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.post;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ServerSocket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WireMockResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(WireMockResource.class);

    WireMockServer wireMockServer;

    private int port;

    @Override
    public Map<String, String> start() {
        // detection of an available port
        // NOTE: WireMock can do this but it loses the port when a stop()/start() is needed to test some cases
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        wireMockServer = new WireMockServer(wireMockConfig().port(port));
        wireMockServer.start();

        wireMockServer.stubFor(post("/oauth2/token")
                .withBasicAuth("client-id", "client-secret")
                .withFormParam("grant_type", equalTo("client_credentials"))
                .willReturn(null)
                .willReturn(ok("{\"token_type\":\"Bearer\",\"expires_in\":1,\"access_token\":\"TOKEN-0\"}")
                        .withFixedDelay(50)));

        var props = Map.of("service.url", wireMockServer.baseUrl());

        LOGGER.info("Wiremock started: {}", props);
        return props;
    }

    @Override
    public synchronized void stop() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(wireMockServer,
                new TestInjector.AnnotatedAndMatchesType(WiremockInject.class, WireMockServer.class));
    }

    @QuarkusTestResource(WireMockResource.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface WiremockInject {
    }
}
