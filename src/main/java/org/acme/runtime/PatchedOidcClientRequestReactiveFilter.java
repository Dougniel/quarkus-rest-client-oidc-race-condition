package org.acme.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.Tokens;
import io.quarkus.oidc.client.reactive.filter.OidcClientRequestReactiveFilter;
import io.quarkus.oidc.client.runtime.DisabledOidcClientException;
import io.quarkus.oidc.client.runtime.OidcClientsConfig;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

public class PatchedOidcClientRequestReactiveFilter extends OidcClientRequestReactiveFilter {
    private static final Logger LOG = Logger.getLogger(PatchedOidcClientRequestReactiveFilter.class);
    private static final String BEARER_SCHEME_WITH_SPACE = OidcConstants.BEARER_SCHEME + " ";

    private OidcClient oidcClient;

    @Inject
    public OidcClientsConfig oidcClientsConfig;

    final PatchedTokensHelper tokensHelper = new PatchedTokensHelper();

    @PostConstruct
    public void init() {
        Optional<OidcClient> initializedClient = client();
        if (initializedClient.isEmpty()) {
            Optional<String> clientId = Objects.requireNonNull(clientId(), "clientId must not be null");
            OidcClients oidcClients = Arc.container().instance(OidcClients.class).get();
            if (clientId.isPresent()) {
                // static named OidcClient
                oidcClient = Objects.requireNonNull(oidcClients.getClient(clientId.get()), "Unknown client");
                earlyTokenAcquisition = false;
            } else {
                // default OidcClient
                earlyTokenAcquisition = false;
                oidcClient = oidcClients.getClient();
            }
        } else {
            oidcClient = initializedClient.get();
        }
    }

    @Override
    public void filter(ResteasyReactiveClientRequestContext requestContext) {
        requestContext.suspend();

        getTokens().subscribe().with(new Consumer<>() {
            @Override
            public void accept(Tokens tokens) {
                requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                        BEARER_SCHEME_WITH_SPACE + tokens.getAccessToken());
                requestContext.resume();
            }
        }, new Consumer<>() {
            @Override
            public void accept(Throwable t) {
                if (t instanceof DisabledOidcClientException) {
                    LOG.debug("Client is disabled, acquiring and propagating the token is not necessary");
                    requestContext.resume();
                } else {
                    LOG.debugf("Access token is not available, cause: %s, aborting the request", t.getMessage());
                    requestContext.resume((t instanceof RuntimeException) ? t : new RuntimeException(t));
                }
            }
        });
    }

    @Override
    public Uni<Tokens> getTokens() {
        final boolean forceNewTokens = isForceNewTokens();
        if (forceNewTokens) {
            final Optional<String> clientId = clientId();
            LOG.debugf("%s OidcClient will discard the current access and refresh tokens", clientId);
        }
        return tokensHelper.getTokens(oidcClient, additionalParameters(), forceNewTokens);
    }
}
