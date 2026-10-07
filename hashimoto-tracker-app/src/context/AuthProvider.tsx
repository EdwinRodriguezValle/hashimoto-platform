// src/context/AuthProvider.tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import keycloak from '../config/keycloak';

// Definimos qué información compartirá este contexto con toda la app
interface AuthContextType {
    authenticated: boolean;
    username: string | undefined;
    token: string | undefined;
    logout: () => void;
}
let isInitialized = false;

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isReady, setIsReady] = useState(false);
    const [authInfo, setAuthInfo] = useState<Omit<AuthContextType, 'logout'>>({
        authenticated: false,
        username: undefined,
        token: undefined,
    });

    useEffect(() => {
        // 💡 Si ya se inició la inicialización, nos saltamos el doble disparo de StrictMode
        if (isInitialized) return;
        isInitialized = true;

        const initializeKeycloak = async (attempt = 0) => {
            const maxAttempts = 6;
            try {
                const authenticated = await keycloak.init({
                    onLoad: 'login-required',
                    checkLoginIframe: false,
                    pkceMethod: 'S256',
                    silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
                });

                setAuthInfo({
                    authenticated,
                    username: keycloak.tokenParsed?.preferred_username,
                    token: keycloak.token,
                });

                if (!authenticated) {
                    console.info('Keycloak initialized, user not currently authenticated. Redirecting to login...');
                    keycloak.login({ redirectUri: window.location.origin });
                }

                setIsReady(true);
            } catch (err) {
                if (attempt < maxAttempts) {
                    const delayMs = 1000 * (attempt + 1);
                    console.warn(`Keycloak not ready yet, retrying in ${delayMs}ms (attempt ${attempt + 1}/${maxAttempts})`, err);
                    window.setTimeout(() => initializeKeycloak(attempt + 1), delayMs);
                    return;
                }

                console.error('❌ Failed to initialize Keycloak after retries:', err);
                isInitialized = false;
                setAuthInfo({
                    authenticated: false,
                    username: undefined,
                    token: undefined,
                });
                setIsReady(true);
            }
        };

        initializeKeycloak();
    }, []);

    const logout = () => {
        keycloak.logout({ redirectUri: window.location.origin });
    };

    // Mientras Keycloak redirige o valida, mostramos un estado de carga limpio
    if (!isReady) {
        return (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', fontFamily: 'sans-serif' }}>
        <h2>Loading Hashimoto Platform Security...</h2>
        </div>
    );
    }

    return (
        <AuthContext.Provider value={{ ...authInfo, logout }}>
    {children}
    </AuthContext.Provider>
);
};

// Hook personalizado para usar la autenticación en cualquier componente con una sola línea
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};