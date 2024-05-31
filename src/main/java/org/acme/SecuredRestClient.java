package org.acme;

import org.acme.runtime.PatchedOidcClientRequestReactiveFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@RegisterProvider(PatchedOidcClientRequestReactiveFilter.class)
@RegisterRestClient(configKey = "secured")
public interface SecuredRestClient {
    @GET
    @Path("/secured-endpoint")
    Uni<Void> callSecuredEndpoint();
}
