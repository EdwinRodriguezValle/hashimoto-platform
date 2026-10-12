import React, { useEffect, useState } from 'react';
import keycloak from '../config/keycloak';
import { AuthContext } from './AuthContext';
let initialization: Promise<boolean> | undefined;

const initializeKeycloak = () => {
    initialization ??= keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256',
    });
    return initialization;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isReady, setIsReady] = useState(false);
    const [authInfo, setAuthInfo] = useState({
        authenticated: false,
        username: undefined as string | undefined,
        canCreatePatients: false,
    });

    useEffect(() => {
        initializeKeycloak()
            .then((authenticated) => {
                setAuthInfo({
                    authenticated,
                    username: keycloak.tokenParsed?.preferred_username,
                    canCreatePatients: keycloak.hasRealmRole('patient-write'),
                });
            })
            .catch((error: unknown) => {
                console.error('Failed to initialize Keycloak:', error);
            })
            .finally(() => setIsReady(true));
    }, []);

    const login = () => {
        void keycloak.login({ redirectUri: window.location.origin });
    };

    const register = () => {
        void keycloak.register({ redirectUri: window.location.origin });
    };

    const logout = () => {
        void keycloak.logout({ redirectUri: window.location.origin });
    };

    const getAccessToken = async () => {
        if (!keycloak.authenticated) return undefined;

        try {
            await keycloak.updateToken(30);
            return keycloak.token;
        } catch (error) {
            console.error('Unable to refresh the Keycloak access token:', error);
            return undefined;
        }
    };

    if (!isReady) {
        return <main className="auth-loading" aria-live="polite">Conectando con Hashimoto Identity…</main>;
    }

    return (
        <AuthContext.Provider value={{ ...authInfo, login, register, logout, getAccessToken }}>
            {children}
        </AuthContext.Provider>
    );
};
