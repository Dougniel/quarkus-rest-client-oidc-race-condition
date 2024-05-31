package org.acme;

import java.util.stream.IntStream;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class GreetingResource {

    @RestClient
    SecuredRestClient client;

    @GET
    @Path("hello")
    public Uni<String> hello() {
        // simulate internal concurrent calls
        return Uni.join()
                .all(IntStream.range(0, 4).mapToObj(i -> client.callSecuredEndpoint()).toList())
                .andFailFast().map(l -> "Hello " + l.size() + " times");
    }
}
