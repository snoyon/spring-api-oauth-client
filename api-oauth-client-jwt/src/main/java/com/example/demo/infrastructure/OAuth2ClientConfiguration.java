package com.example.demo.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JwtBearerOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.endpoint.RestClientJwtBearerTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Configuration
public class OAuth2ClientConfiguration {

    private static final String REGISTRATION_ID = "external-person";
    private static final String CLIENT_ID = "mock-client";
    private static final String SCOPE = "person.read";
    private static final AuthorizationGrantType JWT_BEARER =
            new AuthorizationGrantType("urn:ietf:params:oauth:grant-type:jwt-bearer");

    // Test-only RSA key pair. Never store private keys this way outside a test fixture.
    private static final String PRIVATE_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDKvojWOftFi9ZMIkplhIwmzrSn+3JwHHIwhpRL9gIstVCE2fC4a56hy3RV2BsMEVzDvSwewow6wBCgPPOxOvqxm+OAb9iOikt6cWggjCr3nPxDLN+gbYcK+8AFA4Q7C8Sia+UPyf39OIaeLOk4ZUYgEcV6PhrEMLhv5lIMWC7lWvtshFAxcny0f+XT2HvhhnuQ+hI6NpmzjDOlVI35vFo9bj741V1iPr3gKqYdtMDHcJI95DWTDYDpyXl/ZZsWOR1e4CP8T5dXDaLUGenXl4C/bPDEyz2AhLcrGlDQf7/KTWnJBW6rsq5tjd8wZB8jhPruWtBvEepiRzY/3mZntwdpAgMBAAECggEAC432UHc3ecwxZ19g7A40ypVnOFedOwR0AY3576ZSk2e18gxVqrz/amsk8yuAxxxKNCGY3RvrUAHYMLmojcY1uV+QBKbbKax6Br1l2M0EOd5phg6NpB/53UdhtydvJspR54vMnyy82+yXx3X5yKwE5hj9Ly1Q6zN3n3D55hg0e/+sax/dJ0WHcwT7Bg2dx0KFkCokASEHJj8qOj3mSbn7P8O+Hc8zZZ/dTiM8X2ySUkohAnpbffVefpNdfhyQvmKHbSQxhpg54fIJM+1mn9qyiCivac3Se8SRxfbEFXs1Y4b1yWZ1OuYMTuQhWlgJTgEDbawpq4CtX3NGuok6Uart3QKBgQDvCpVto0y6nASbyphIiIvVydK3LqkcChVgY4r8c9K1wy3fPE2Uvc36zjKsTQhoDnT1XFFMJoI0nNsIMFlTcEaznxvOUYv9EwTXtABWykJ2980nDGgR3Wo92i3qYtyosuDP0pUFUrjlaEuL4EEpynZ4O65kvHrYBcRE6X70gxQgmwKBgQDZILtBM2vTd6fE7XKofgoDSgS7HvKRy01ByRQJOJSENlP8G9lVTNPO7XfE/lNCzI3+2loX8QsG8OQFDNVjeq3REA3m2d5ejhBkLfjpQkrMlSnKPCzdzYbMwyTf3ZzcpO/ndSeCaLiWtzrE69WrHYqbvKPUYeTOORmVTMe0lysOSwKBgD4qrnn5Ajd14+zwX3JdsKBFALwLMofMR4rt2HOXW7FRtVhdQn/wbOVRQvR0hD8ro1c8TxhS24H7WStkB5cfmAOW2ZBqvNFwZM4ETiJEL8zo3T3OiDI9Nygm2dIfK+vjuXvS9FaSEOv4l80k4U2RDgZu3wfrzLbpqQWTBCVrAY0JAoGAJtDXKPzMVe2aLHZWfRCrMZNV7S3HJ5E0qoIL9uoKguhDc1p3K+ykIOYK90iQEe0HpXvbh1QsKH2ABfcZXsNbt3grRA3G1xiGjaI6UVjsjk5MPj8rtis74fcw7GRt2nQR/0rOWV3nQepXs0SF2iVZ1iWPFDS79rH2hN5JBVvQ8qkCgYAOENnGot2qntx49cme4huEbPpKsbmcBUhjEWzmBHSG0ea7CnlnhrZgpvjXxdhKe93M5016HSYHHiutshYxaoKVkZO+kqh3uyA2n8dUMZYgcpvTgs4JllTXSF//QLPrjNGf3pf+IE+ZIa8vlHbMhvpU6vze5akFnRaiXiY6YjnIIA==";
    private static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyr6I1jn7RYvWTCJKZYSMJs60p/tycBxyMIaUS/YCLLVQhNnwuGueoct0VdgbDBFcw70sHsKMOsAQoDzzsTr6sZvjgG/YjopLenFoIIwq95z8QyzfoG2HCvvABQOEOwvEomvlD8n9/TiGnizpOGVGIBHFej4axDC4b+ZSDFgu5Vr7bIRQMXJ8tH/l09h74YZ7kPoSOjaZs4wzpVSN+bxaPW4++NVdYj694CqmHbTAx3CSPeQ1kw2A6cl5f2WbFjkdXuAj/E+XVw2i1Bnp15eAv2zwxMs9gIS3KxpQ0H+/yk1pyQVuq7KubY3fMGQfI4T67lrQbxHqYkc2P95mZ7cHaQIDAQAB";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${external-person.token-uri}") String tokenUri) {
        var registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .tokenUri(tokenUri)
                .clientId(CLIENT_ID)
                .clientAuthenticationMethod(
                        org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
                .authorizationGrantType(JWT_BEARER)
                .scope(SCOPE)
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    JwtEncoder jwtEncoder() {
        try {
            var keyFactory = KeyFactory.getInstance("RSA");
            var privateKey = (RSAPrivateKey) keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_KEY)));
            var publicKey = (RSAPublicKey) keyFactory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(PUBLIC_KEY)));
            return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
        }
        catch (Exception exception) {
            throw new IllegalStateException("Unable to create the test RSA key pair", exception);
        }
    }

    @Bean
    OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository registrations) {
        return new InMemoryOAuth2AuthorizedClientService(registrations);
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClientService,
            JwtEncoder jwtEncoder,
            @Value("${external-person.token-uri}") String tokenUri) {
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations, authorizedClientService);
        var provider = new JwtBearerOAuth2AuthorizedClientProvider();
        provider.setJwtAssertionResolver(context -> {
            var now = Instant.now();
            var claims = JwtClaimsSet.builder()
                    .issuer(CLIENT_ID)
                    .subject(CLIENT_ID)
                    .audience(List.of(tokenUri))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(60))
                    .id(UUID.randomUUID().toString())
                    .build();
            return jwtEncoder.encode(JwtEncoderParameters.from(claims));
        });
        var tokenResponseClient = new RestClientJwtBearerTokenResponseClient();
        tokenResponseClient.setParametersCustomizer(parameters ->
                parameters.remove("client_id"));
        provider.setAccessTokenResponseClient(tokenResponseClient);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }




    @Bean
    OAuth2ClientHttpRequestInterceptor oauth2Interceptor(
            OAuth2AuthorizedClientManager authorizedClientManager) {
        var interceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        return interceptor;
    }
}
