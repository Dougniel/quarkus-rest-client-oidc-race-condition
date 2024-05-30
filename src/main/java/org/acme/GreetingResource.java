package org.acme;

import java.util.stream.IntStream;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class GreetingResource {

    @RestClient
    SecuredRestClient restClient;

    @GET
    @Path("hello")
    public Uni<String> hello() {
        return Uni.join()
                .all(IntStream.range(0, 10).mapToObj(i -> restClient.callSecuredEndpoint()).toList())
                .andFailFast()
                .map(l -> "Hello " + l.size() + " times");
    }
}
