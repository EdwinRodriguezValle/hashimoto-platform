// src/components/PatientForm.tsx
import React, { useState } from 'react';
import { useAuth } from '../context/AuthProvider';
import { createPatientProfile } from '../services/patient/patientService.ts';


export const PatientForm: React.FC = () => {
    const { token } = useAuth();
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');

    // Estado inicial alineado con tu interfaz exacta del Backend
    const [formData, setFormData] = useState({
        firstName: '',
        lastName: '',
        gender: 'MALE', // Valor por defecto común para enums de backend
        dni: '',
        email: '',
        age: ''
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!token) return;

        setLoading(true);
        setMessage('');

        try {
            const payload = {
                firstName: formData.firstName,
                lastName: formData.lastName,
                gender: formData.gender,
                dni: formData.dni,
                email: formData.email,
                age: Number(formData.age) // Convertimos el string del input a un tipo number de TS
            };

            await createPatientProfile(payload, token);
            setMessage('✅ Patient profile registered and broadcasted to Kafka successfully!');

            // Limpiamos el formulario tras el éxito
            setFormData({ firstName: '', lastName: '', gender: 'MALE', dni: '', email: '', age: '' });
        } catch (error) {
            console.error(error);
            setMessage('❌ Failed to register profile. Check API Gateway or Security authorization.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '500px', marginTop: '30px', padding: '25px', border: '1px solid #e0e0e0', borderRadius: '8px', backgroundColor: '#ffffff', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
            <h3 style={{ marginTop: 0, color: '#333' }}>📝 Patient Admission Form (MVP)</h3>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
                <div style={{ display: 'flex', gap: '15px' }}>
                    <label style={{ flex: 1 }}>
                        <strong>First Name:</strong>
                        <input
                            type="text"
                            required
                            value={formData.firstName}
                            onChange={e => setFormData({...formData, firstName: e.target.value})}
                            style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box' }}
                        />
                    </label>

                    <label style={{ flex: 1 }}>
                        <strong>Last Name:</strong>
                        <input
                            type="text"
                            required
                            value={formData.lastName}
                            onChange={e => setFormData({...formData, lastName: e.target.value})}
                            style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box' }}
                        />
                    </label>
                </div>

                <div style={{ display: 'flex', gap: '15px' }}>
                    <label style={{ flex: 1 }}>
                        <strong>Gender:</strong>
                        <select
                            value={formData.gender}
                            onChange={e => setFormData({...formData, gender: e.target.value})}
                            style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box', height: '34px' }}
                        >
                            <option value="MALE">Male</option>
                            <option value="FEMALE">Female</option>
                            <option value="OTHER">Other</option>
                        </select>
                    </label>

                    <label style={{ flex: 1 }}>
                        <strong>Age:</strong>
                        <input
                            type="number"
                            required
                            min="0"
                            max="120"
                            value={formData.age}
                            onChange={e => setFormData({...formData, age: e.target.value})}
                            style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box' }}
                        />
                    </label>
                </div>

                <label>
                    <strong>DNI / Identification Number:</strong>
                    <input
                        type="text"
                        required
                        value={formData.dni}
                        onChange={e => setFormData({...formData, dni: e.target.value})}
                        style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box' }}
                    />
                </label>

                <label>
                    <strong>Email Address:</strong>
                    <input
                        type="email"
                        required
                        value={formData.email}
                        onChange={e => setFormData({...formData, email: e.target.value})}
                        style={{ width: '100%', padding: '8px', marginTop: '5px', boxSizing: 'border-box' }}
                    />
                </label>

                <button
                    type="submit"
                    disabled={loading}
                    style={{ padding: '10px', backgroundColor: '#0288d1', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', fontWeight: 'bold', marginTop: '10px' }}
                >
                    {loading ? 'Transmitting Data...' : 'Register Patient'}
                </button>
            </form>

            {message && (
                <div style={{ marginTop: '15px', padding: '10px', borderRadius: '4px', backgroundColor: message.includes('✅') ? '#e8f5e9' : '#ffebee', color: message.includes('✅') ? '#2e7d32' : '#c62828', fontSize: '14px' }}>
                    {message}
                </div>
            )}
        </div>
    );
};