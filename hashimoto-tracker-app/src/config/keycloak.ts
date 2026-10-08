import Keycloak from 'keycloak-js';

// Hardcode para K8s en esta rama: el navegador debe usar la misma origin + /auth
// para que nginx haga el proxy a Keycloak y se evite el CORS.
const keycloakUrl = import.meta.env.VITE_KEYCLOAK_URL || `${window.location.origin}/auth`;

const keycloakConfig = {
    url: keycloakUrl,
    realm: 'hashimoto-realm',
    clientId: 'hashimoto-tracker-web',
};

const keycloak = new Keycloak(keycloakConfig);

export default keycloak;

// import Keycloak from 'keycloak-js';
//
// // Configuración del servidor de identidad corporativo
// const keycloakConfig = {
//
//     url: 'http://localhost:8082', // Puerto externo de Keycloak en tu máquina física
//     realm: 'hashimoto-realm',
//     clientId: 'hashimoto-tracker-web',
// };
//
// const keycloak = new Keycloak(keycloakConfig);
//
// export default keycloak;
