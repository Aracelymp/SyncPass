import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Reports.css';

interface EventStat {
  eventId: number;
  title: string;
  projectName: string;
  startsAt: string;
  enrolled: number;
  attended: number;
  attendanceRate: number;
  averageRating: number;
}

interface ImpactReport {
  generatedAt: string;
  totalProjects: number;
  totalEvents: number;
  totalEnrollments: number;
  totalAttendances: number;
  uniqueParticipants: number;
  attendanceRate: number;
  averageSatisfaction: number;
  recommendRate: number;
  events: EventStat[];
}

/** "2026-10-28T09:00:00" / "2026-10-28T09:00:00Z" -> "2026-10-28 09:00" */
const formatDateTime = (value: string | null): string => {
  if (!value) return '—';
  if (value.includes('T')) {
    return `${value.slice(0, 10)} ${value.slice(11, 16)}`;
  }
  return value.slice(0, 16);
};

const formatNumber = (value: number): string => value.toLocaleString('es-CL');

const formatPercent = (value: number): string => `${value.toFixed(1)}%`;

const describeError = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para ver este reporte (solo Docente · Coordinador).';
      case 404:
        return 'El reporte no está disponible.';
      case 0:
        return 'No se pudo conectar con el servidor.';
      default:
        return `No se pudo cargar el reporte (error ${error.status}).`;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Ocurrió un error inesperado.';
};

export const Reports: React.FC = () => {
  const { user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const [report, setReport] = useState<ImpactReport | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const loadReport = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const data = await apiRequest<ImpactReport>('/api/reports/impact');
      setReport(data);
    } catch (error) {
      setReport(null);
      setLoadError(describeError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isCoordinator) {
      setIsLoading(false);
      setReport(null);
      return;
    }
    void loadReport();
  }, [isCoordinator, loadReport]);

  const hasData = report
    ? report.totalProjects > 0 ||
      report.totalEvents > 0 ||
      report.totalEnrollments > 0 ||
      report.totalAttendances > 0
    : false;

  const hasSurveys = report
    ? report.averageSatisfaction > 0 || report.recommendRate > 0
    : false;

  const satisfactionPercent = report ? (report.averageSatisfaction / 5) * 100 : 0;

  return (
    <AppLayout>
      <div className="reports-page">
        <div className="page-header">
          <h1 className="page-title">Reportes e Indicadores</h1>
          <p className="page-subtitle">Datos reales calculados por la API de SyncPass</p>
        </div>

        {!isCoordinator ? (
          <Card title="Acceso restringido">
            <p className="no-selection">
              Este reporte solo está disponible para el rol Docente · Coordinador. Tu rol actual no
              tiene permiso para consultarlo.
            </p>
          </Card>
        ) : isLoading ? (
          <Card>
            <p className="no-selection">Cargando reporte...</p>
          </Card>
        ) : loadError ? (
          <Card>
            <div className="banner banner-error" role="alert">
              <span>{loadError}</span>
              <Button type="button" variant="outline" onClick={() => void loadReport()}>
                Reintentar
              </Button>
            </div>
          </Card>
        ) : !report ? (
          <Card>
            <p className="no-selection">No hay información disponible para mostrar.</p>
          </Card>
        ) : (
          <>
            {!hasData && (
              <div className="banner banner-info" role="status">
                El reporte no contiene datos registrados todavía: no hay proyectos, eventos,
                inscripciones ni asistencias.
              </div>
            )}

            <div className="stats-grid">
              <Card className="stat-card">
                <div className="stat-content">
                  <div className="stat-icon">📁</div>
                  <div className="stat-info">
                    <p className="stat-value">{formatNumber(report.totalProjects)}</p>
                    <p className="stat-title">Total Proyectos</p>
                  </div>
                </div>
              </Card>
              <Card className="stat-card">
                <div className="stat-content">
                  <div className="stat-icon">📅</div>
                  <div className="stat-info">
                    <p className="stat-value">{formatNumber(report.totalEvents)}</p>
                    <p className="stat-title">Total Eventos</p>
                  </div>
                </div>
              </Card>
              <Card className="stat-card">
                <div className="stat-content">
                  <div className="stat-icon">👥</div>
                  <div className="stat-info">
                    <p className="stat-value">{formatNumber(report.uniqueParticipants)}</p>
                    <p className="stat-title">Participantes Únicos</p>
                    <p className="stat-trend">
                      {formatNumber(report.totalAttendances)} asistencia(s) registrada(s)
                    </p>
                  </div>
                </div>
              </Card>
              <Card className="stat-card">
                <div className="stat-content">
                  <div className="stat-icon">✅</div>
                  <div className="stat-info">
                    <p className="stat-value">{formatNumber(report.totalAttendances)}</p>
                    <p className="stat-title">Asistencias Registradas</p>
                    <p className="stat-trend">
                      {formatPercent(report.attendanceRate)} de {formatNumber(report.totalEnrollments)}{' '}
                      inscripción(es)
                    </p>
                  </div>
                </div>
              </Card>
            </div>

            <div className="reports-content">
              <div className="reports-row">
                <Card title="Desempeño por Evento">
                  <div className="project-stats-table">
                    <table>
                      <thead>
                        <tr>
                          <th>Evento</th>
                          <th>Proyecto</th>
                          <th>Fecha</th>
                          <th>Inscritos</th>
                          <th>Asistentes</th>
                          <th>% Asistencia</th>
                          <th>Satisfacción</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.events.length === 0 ? (
                          <tr>
                            <td className="empty-row" colSpan={7}>
                              Sin eventos registrados.
                            </td>
                          </tr>
                        ) : (
                          report.events.map((stat) => (
                            <tr key={stat.eventId}>
                              <td className="project-name-cell">{stat.title}</td>
                              <td className="project-cell">{stat.projectName}</td>
                              <td className="center-cell">{formatDateTime(stat.startsAt)}</td>
                              <td className="center-cell">{formatNumber(stat.enrolled)}</td>
                              <td className="center-cell">{formatNumber(stat.attended)}</td>
                              <td className="center-cell">
                                <span
                                  className={`attendance-percent ${
                                    stat.attendanceRate >= 85
                                      ? 'high'
                                      : stat.attendanceRate >= 75
                                        ? 'medium'
                                        : 'low'
                                  }`}
                                >
                                  {formatPercent(stat.attendanceRate)}
                                </span>
                              </td>
                              <td className="center-cell">
                                {stat.averageRating > 0
                                  ? `${stat.averageRating.toFixed(1)} / 5`
                                  : '—'}
                              </td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </Card>

                <Card title="Satisfacción General">
                  {!hasSurveys ? (
                    <p className="no-selection">
                      Sin encuestas registradas todavía: no hay promedio de satisfacción ni tasa de
                      recomendación.
                    </p>
                  ) : (
                    <div className="satisfaction-section">
                      <div className="satisfaction-chart">
                        <div className="satisfaction-bar-container">
                          <div className="satisfaction-label-row">
                            <span className="satisfaction-label">Recomendarían el evento</span>
                            <span className="satisfaction-percentage">
                              {formatPercent(report.recommendRate)}
                            </span>
                          </div>
                          <div className="satisfaction-bar-bg">
                            <div
                              className="satisfaction-bar bar-primary"
                              style={{ width: `${Math.min(100, Math.max(0, report.recommendRate))}%` }}
                            ></div>
                          </div>
                        </div>
                        <div className="satisfaction-bar-container">
                          <div className="satisfaction-label-row">
                            <span className="satisfaction-label">Satisfacción promedio (escala 0–5)</span>
                            <span className="satisfaction-percentage">
                              {report.averageSatisfaction.toFixed(1)} / 5
                            </span>
                          </div>
                          <div className="satisfaction-bar-bg">
                            <div
                              className="satisfaction-bar bar-secondary"
                              style={{ width: `${Math.min(100, Math.max(0, satisfactionPercent))}%` }}
                            ></div>
                          </div>
                        </div>
                      </div>
                      <div className="satisfaction-summary">
                        <p className="satisfaction-average">
                          <strong>Promedio de Satisfacción:</strong>{' '}
                          {report.averageSatisfaction.toFixed(1)} / 5.0
                        </p>
                        <p className="satisfaction-note">
                          Calculado con las encuestas registradas en el sistema.
                        </p>
                      </div>
                    </div>
                  )}
                </Card>
              </div>

              <Card title="Indicadores de Impacto">
                <div className="impact-indicators">
                  <div className="impact-item">
                    <div className="impact-icon">📝</div>
                    <div className="impact-info">
                      <p className="impact-value">{formatNumber(report.totalEnrollments)}</p>
                      <p className="impact-label">Inscripciones Totales</p>
                    </div>
                  </div>
                  <div className="impact-item">
                    <div className="impact-icon">🎯</div>
                    <div className="impact-info">
                      <p className="impact-value">{formatPercent(report.attendanceRate)}</p>
                      <p className="impact-label">Tasa de Asistencia</p>
                    </div>
                  </div>
                  <div className="impact-item">
                    <div className="impact-icon">🤝</div>
                    <div className="impact-info">
                      <p className="impact-value">{formatPercent(report.recommendRate)}</p>
                      <p className="impact-label">Tasa de Recomendación</p>
                    </div>
                  </div>
                </div>
                <p className="impact-disclaimer">
                  Reporte generado por el servidor el {formatDateTime(report.generatedAt)} · Fuente:
                  GET /api/reports/impact
                </p>
              </Card>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  );
};
