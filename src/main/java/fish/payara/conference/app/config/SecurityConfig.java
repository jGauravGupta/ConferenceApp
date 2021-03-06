package fish.payara.conference.app.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SecurityConfig {

    @Inject
    @ConfigProperty(name = "security.authentication.jwt.secret")
    private String secret;

    @Inject
    @ConfigProperty(name = "security.authentication.jwt.tokenValidityInSeconds")
    private long tokenValidityInSeconds;

    @Inject
    @ConfigProperty(name = "security.authentication.jwt.tokenValidityInSecondsForRememberMe")
    private long tokenValidityInSecondsForRememberMe;

    /**
     * @return the secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @return the tokenValidityInSeconds
     */
    public long getTokenValidityInSeconds() {
        return tokenValidityInSeconds;
    }

    /**
     * @return the tokenValidityInSecondsForRememberMe
     */
    public long getTokenValidityInSecondsForRememberMe() {
        return tokenValidityInSecondsForRememberMe;
    }

}
