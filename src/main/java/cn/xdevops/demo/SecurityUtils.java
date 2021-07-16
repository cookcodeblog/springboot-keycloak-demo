package cn.xdevops.demo;

import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;

import javax.servlet.http.HttpServletRequest;

public final class SecurityUtils {

    private SecurityUtils() {

    }

    public static KeycloakSecurityContext getKeycloakSecurityContext(HttpServletRequest request) {
        return (KeycloakSecurityContext) request.getAttribute(KeycloakSecurityContext.class.getName());
    }

    public static IDToken getIDToken(HttpServletRequest request) {
        return SecurityUtils.getKeycloakSecurityContext(request).getIdToken();
    }
}
