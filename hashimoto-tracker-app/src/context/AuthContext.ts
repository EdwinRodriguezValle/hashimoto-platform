import { createContext } from 'react';

export interface AuthContextType {
    authenticated: boolean;
    username: string | undefined;
    canCreatePatients: boolean;
    login: () => void;
    register: () => void;
    logout: () => void;
    getAccessToken: () => Promise<string | undefined>;
}

export const AuthContext = createContext<AuthContextType | null>(null);
