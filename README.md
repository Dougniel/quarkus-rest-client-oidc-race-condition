# 🧨 quarkus-rest-client-oidc-race-condition

The purpose is to give evidence of a problem in Quarkus when using the [Rest Reactive Client](https://quarkus.io/guides/rest-client-reactive) with [OidcClient Reactive ClientFilter](https://quarkus.io/guides/security-openid-connect-client-reference#rest-client-oidc-filter) : within concurrent calls condition, the token is retrieved several times 🐛 

🤕 A patch is provided in `patch` branch
