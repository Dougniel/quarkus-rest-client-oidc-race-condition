package org.acme;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.stream.IntStream;

import org.acme.WireMockResource.WiremockInject;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class GreetingResourceTest {

    @WiremockInject
    WireMockServer wiremock;

    @Test
    void test() throws InterruptedException {
        // mock authentification (Oauth2)
        wiremock.stubFor(post("/oauth2/token")
                .withBasicAuth("client-id", "client-secret")
                .withFormParam("grant_type", equalTo("client_credentials"))
                .willReturn(ok("{\"token_type\":\"Bearer\",\"expires_in\":1,\"access_token\":\"TOKEN-1\"}")
                        .withFixedDelay(50)));

        wiremock.stubFor(get("/secured-endpoint")
                .withHeader("Authorization", equalTo("Bearer TOKEN-1"))
                .willReturn(ok()));

        IntStream.range(0, 3).parallel().forEach(i -> given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello 4 times")));

        wiremock.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")));

        wiremock.resetAll(); // reset mocks
        wiremock.resetRequests(); // reset counters

        Thread.sleep(2000); // wait for token expiration

        wiremock.stubFor(post("/oauth2/token")
                .withBasicAuth("client-id", "client-secret")
                .withFormParam("grant_type", equalTo("client_credentials"))
                .willReturn(ok("{\"token_type\":\"Bearer\",\"expires_in\":1,\"access_token\":\"TOKEN-2\"}")
                        .withFixedDelay(50)));

        wiremock.stubFor(get("/secured-endpoint")
                .withHeader("Authorization", equalTo("Bearer TOKEN-1"))
                .willReturn(unauthorized().withBody("expired token")));

        wiremock.stubFor(get("/secured-endpoint")
                .withHeader("Authorization", equalTo("Bearer TOKEN-2"))
                .willReturn(ok()));

        IntStream.range(0, 3).parallel().forEach(i -> given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello 4 times")));

        wiremock.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")));
    }
}
