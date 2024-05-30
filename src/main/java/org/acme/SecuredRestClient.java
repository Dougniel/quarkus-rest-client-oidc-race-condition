package org.acme;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterProvider(OidcClientRequestReactiveFilter.class)
@RegisterRestClient(configKey = "secured")
public interface SecuredRestClient {
    @GET
    @Path("/secured-endpoint")
    Uni<Void> callSecuredEndpoint();
}
