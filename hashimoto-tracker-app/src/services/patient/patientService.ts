
interface PatientData {
    firstName: string;
    lastName: string;
    gender: string;
    dni: string;
    email: string;
    age: number;
}

export const createPatientProfile = async (data: PatientData, token: string) => {
    // El prefijo '/api' activará el Proxy de vite.config.ts hacia el puerto 8081
    const response = await fetch('/api/patients', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // INYECTAMOS EL TOKEN DE KEYCLOAK DE FORMA OBLIGATORIA
            'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(data),
    });

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    // 💡 SOLUCIÓN: Verificamos si la respuesta es texto plano (un ID de Axon) en lugar de un JSON
    const contentType = response.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
        return await response.json();
    } else {
        // Si Axon devuelve el UUID en texto crudo, lo capturamos limpiamente como string
        const textId = await response.text();
        return { success: true, id: textId };
    }
};