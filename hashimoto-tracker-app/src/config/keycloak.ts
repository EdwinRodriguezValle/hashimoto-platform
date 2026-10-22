import Keycloak from 'keycloak-js';

// Usa el origen público actual en los tres entornos: Vite/Nginx enruta /auth
// hacia Keycloak y el navegador mantiene un solo origen durante OIDC.
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || `${window.location.origin}/auth`;

const keycloakConfig = {
    url: keycloakUrl,
    realm: 'hashimoto-realm',
    clientId: 'hashimoto-tracker-web',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;