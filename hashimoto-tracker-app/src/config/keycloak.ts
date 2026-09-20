import Keycloak from 'keycloak-js';

// Configuración del servidor de identidad corporativo
const keycloakConfig = {
    url: 'http://localhost:8082', // Puerto externo de Keycloak en tu máquina física
    realm: 'hashimoto-realm',
    clientId: 'hashimoto-tracker-web',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;
