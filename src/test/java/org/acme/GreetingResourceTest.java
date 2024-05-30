package org.acme;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

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
    void test() {
        // mock authentification (Oauth2)
        wiremock.stubFor(post("/oauth2/token")
                .withBasicAuth("client-id", "client-secret")
                .withFormParam("grant_type", equalTo("client_credentials"))
                .willReturn(ok("{\"token_type\":\"Bearer\",\"expires_in\":300,\"access_token\":\"mock\"}")));

        wiremock.stubFor(get("/secured-endpoint")
                .withHeader("Authorization", equalTo("Bearer mock"))
                .willReturn(ok()));

        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello 10 times"));

        wiremock.verify(1, postRequestedFor(urlEqualTo("/oauth2/token")));
    }
}
