// src/App.tsx
import { useAuth } from './context/AuthProvider';
import { PatientForm } from './components/PatientForm';

function App() {
  const { username, logout } = useAuth();

  return (
      <div style={{ padding: '40px', fontFamily: 'sans-serif', backgroundColor: '#f9f9f9', minHeight: '100vh' }}>
        <h1>🚀 Hashimoto Tracker Dashboard</h1>
        <p>Welcome back, <strong>{username}</strong>!</p>

        {/* 👇 NUEVO FORMULARIO DE PRUEBA END-TO-END */}
        <PatientForm />

        <button
            onClick={logout}
            style={{ marginTop: '30px', padding: '10px 20px', backgroundColor: '#d32f2f', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
        >
          Logout
        </button>
      </div>
  );
}

export default App;