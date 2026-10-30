import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Dashboard.css';

type ProjectStatus = 'PLANIFICADO' | 'EN_CURSO' | 'FINALIZADO';

interface Project {
  id: number;
  code: string;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  status: ProjectStatus;
  responsible: string;
  eventCount: number;
  createdAt: string;
}

interface DashboardEvent {
  id: number;
  projectId: number;
  projectName: string;
  title: string;
  description: string;
  location: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  qrToken: string;
  status: string;
  enrolledCount: number;
  attendedCount: number;
  averageRating: number | null;
}

const STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANIFICADO: 'Planificación',
  EN_CURSO: 'En Curso',
  FINALIZADO: 'Finalizado',
};

const describeError = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para ver estos datos.';
      case 0:
        return 'No se pudo conectar con el servidor.';
      default:
        return `No se pudo cargar el panel (error ${error.status}).`;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Ocurrió un error inesperado.';
};

const statusClass = (status: ProjectStatus): string =>
  `status-${status.toLowerCase().replace(/_/g, '-')}`;

export const Dashboard: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [events, setEvents] = useState<DashboardEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    try {
      const [projectsData, eventsData] = await Promise.all([
        apiRequest<Project[]>('/api/projects'),
        apiRequest<DashboardEvent[]>('/api/events'),
      ]);
      setProjects(projectsData);
      setEvents(eventsData);
    } catch (err) {
      setError(describeError(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const totalProjects = projects.length;
  const totalEvents = events.length;
  const totalEnrollments = events.reduce(
    (sum, event) => sum + (event.enrolledCount || 0),
    0,
  );
  const totalAttendances = events.reduce(
    (sum, event) => sum + (event.attendedCount || 0),
    0,
  );

  const recentProjects = projects.slice(0, 3);

  const stats = [
    {
      title: 'Total Proyectos',
      value: isLoading ? '—' : totalProjects.toString(),
      icon: '📁',
      color: 'primary',
    },
    {
      title: 'Total Eventos',
      value: isLoading ? '—' : totalEvents.toString(),
      icon: '📅',
      color: 'secondary',
    },
    {
      title: 'Total Inscritos',
      value: isLoading ? '—' : totalEnrollments.toString(),
      icon: '👥',
      color: 'accent',
    },
    {
      title: 'Total Asistencias',
      value: isLoading ? '—' : totalAttendances.toString(),
      icon: '✅',
      color: 'accent-light',
    },
  ];

  return (
    <AppLayout>
      <div className="dashboard">
        <div className="dashboard-header">
          <h1 className="dashboard-title">Panel de Control</h1>
          <p className="dashboard-subtitle">
            {isLoading ? 'Cargando datos reales...' : 'Resumen general de SyncPass'}
          </p>
        </div>

        {error && (
          <div className="banner banner-error" role="alert">
            <span>{error}</span>
            <Button type="button" variant="outline" onClick={() => void loadData()}>
              Reintentar
            </Button>
          </div>
        )}

        <div className="stats-grid">
          {stats.map((stat, index) => (
            <Card key={index} className="stat-card">
              <div className="stat-content">
                <div className="stat-icon">{stat.icon}</div>
                <div className="stat-info">
                  <p className="stat-value">{stat.value}</p>
                  <p className="stat-title">{stat.title}</p>
                </div>
              </div>
            </Card>
          ))}
        </div>

        <div className="dashboard-content">
          <Card title="Proyectos Recientes">
            <div className="projects-list">
              {isLoading ? (
                <p className="no-selection">Cargando proyectos...</p>
              ) : recentProjects.length === 0 ? (
                <p className="no-selection">No hay proyectos registrados.</p>
              ) : (
                recentProjects.map((project) => (
                  <div key={project.id} className="project-item">
                    <div className="project-info">
                      <h4 className="project-name">{project.name}</h4>
                      <p className="project-meta">{project.eventCount} eventos</p>
                    </div>
                    <span className={`project-status ${statusClass(project.status)}`}>
                      {STATUS_LABELS[project.status]}
                    </span>
                  </div>
                ))
              )}
            </div>
          </Card>
        </div>
      </div>
    </AppLayout>
  );
};
