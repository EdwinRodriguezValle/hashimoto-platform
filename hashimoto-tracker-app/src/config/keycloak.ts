import Keycloak from 'keycloak-js';

// 🚀 SOLUCIÓN: Calculamos la URL estrictamente según el puerto real en el que está el navegador del usuario.
const isLocalDev = typeof window !== 'undefined' &&
    (window.location.port === '5173' || window.location.port === '4173' || window.location.port === '3000');

// Si el navegador está en el puerto 80 nativo de K8s, ignoramos cualquier .env viejo y forzamos /auth
const KEYCLOAK_URL = isLocalDev ? 'http://localhost:8082' : `${window.location.origin}/auth`;

const keycloakConfig = {
    url: KEYCLOAK_URL,
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
