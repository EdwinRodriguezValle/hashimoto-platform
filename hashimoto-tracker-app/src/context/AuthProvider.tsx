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
        // Inicialización segura con PKCE
        keycloak
            .init({
                onLoad: 'login-required',
                checkLoginIframe: false,
                pkceMethod: 'S256',
            })
            .then((authenticated) => {
                setAuthInfo({
                    authenticated,
                    username: keycloak.tokenParsed?.preferred_username,
                    token: keycloak.token,
                });
                setIsReady(true);
            })
            .catch((err) => {
                console.error('❌ Failed to initialize Keycloak:', err);
                isInitialized = false; // Permitir reintento si falla radicalmente
            });
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