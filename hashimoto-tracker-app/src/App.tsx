import { useAuth } from './context/useAuth';
import { PatientForm } from './components/PatientForm';

function App() {
  const { authenticated, username, canCreatePatients, login, register, logout } = useAuth();

  if (!authenticated) {
    return (
      <main className="auth-page">
        <section className="auth-card" aria-labelledby="welcome-title">
          <div className="brand-mark" aria-hidden="true">H</div>
          <p className="eyebrow">HASHIMOTO PLATFORM</p>
          <h1 id="welcome-title">Tu espacio de salud, conectado.</h1>
          <p className="auth-description">
            Inicia sesión o crea una cuenta. Hashimoto Identity protege tu acceso y tus datos.
          </p>
          <div className="auth-actions">
            <button className="button button-primary" onClick={login}>Iniciar sesión</button>
            <button className="button button-secondary" onClick={register}>Crear cuenta</button>
          </div>
          <p className="auth-footnote">La cuenta se administra de forma segura con Keycloak.</p>
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">HASHIMOTO PLATFORM</p>
          <h1>Panel de salud</h1>
        </div>
        <div className="account-actions">
          <span className="account-name">{username}</span>
          <button className="button button-secondary button-small" onClick={logout}>Cerrar sesión</button>
        </div>
      </header>

      {canCreatePatients ? (
        <PatientForm />
      ) : (
        <section className="permission-notice" role="status">
          <h2>Tu cuenta ya está conectada</h2>
          <p>
            Para registrar pacientes, un administrador debe asignarte el rol <code>patient-write</code> en Keycloak.
            Las cuentas nuevas no reciben permisos de escritura automáticamente.
          </p>
        </section>
      )}
    </main>
  );
}

export default App;
