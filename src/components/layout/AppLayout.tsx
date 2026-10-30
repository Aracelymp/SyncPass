import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import './AppLayout.css';

interface AppLayoutProps {
  children: React.ReactNode;
}

export const AppLayout: React.FC<AppLayoutProps> = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const { logout, user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const isActive = (path: string) => location.pathname === path;

  const handleLogout = async (): Promise<void> => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="app-layout">
      <header className="app-header">
        <div className="header-container">
          <div className="header-brand">
            <h1 className="brand-logo">SyncPass</h1>
            <span className="brand-subtitle">Gestión Comunitaria</span>
          </div>
          <nav className="header-nav">
            <Link 
              to="/dashboard" 
              className={`nav-link ${isActive('/dashboard') ? 'active' : ''}`}
            >
              Dashboard
            </Link>
            <Link 
              to="/projects" 
              className={`nav-link ${isActive('/projects') ? 'active' : ''}`}
            >
              Proyectos
            </Link>
            <Link 
              to="/events" 
              className={`nav-link ${isActive('/events') ? 'active' : ''}`}
            >
              Eventos
            </Link>
            <Link 
              to="/attendance" 
              className={`nav-link ${isActive('/attendance') ? 'active' : ''}`}
            >
              Asistencia
            </Link>
            <Link 
              to="/surveys" 
              className={`nav-link ${isActive('/surveys') ? 'active' : ''}`}
            >
              Encuestas
            </Link>
            {isCoordinator && (
              <Link 
                to="/reports" 
                className={`nav-link ${isActive('/reports') ? 'active' : ''}`}
              >
                Reportes
              </Link>
            )}
            <button
              type="button"
              className="nav-link nav-link-logout"
              onClick={handleLogout}
              style={{ border: 'none', font: 'inherit', cursor: 'pointer' }}
            >
              Cerrar Sesión
            </button>
          </nav>
        </div>
      </header>
      <main className="app-main">
        <div className="main-container">
          {children}
        </div>
      </main>
    </div>
  );
};
