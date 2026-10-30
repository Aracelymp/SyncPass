import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Events.css';

type EventStatus = 'PROGRAMADO' | 'EN_CURSO' | 'FINALIZADO' | 'CANCELADO';

interface Event {
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
  status: EventStatus;
  enrolledCount: number;
  attendedCount: number;
  averageRating: number | null;
}

interface Participant {
  userId: number | null;
  fullName: string;
  username: string | null;
  email: string | null;
  enrolled: boolean;
  attended: boolean;
  method: string | null;
  registeredAt: string | null;
}

interface EventDetail {
  event: Event;
  participants: Participant[];
}

interface ProjectOption {
  id: number;
  code: string;
  name: string;
}

interface EventFormState {
  id: number | null;
  projectId: string;
  title: string;
  description: string;
  location: string;
  startsAt: string;
  endsAt: string;
  capacity: string;
  qrToken: string;
  status: EventStatus;
}

const STATUS_OPTIONS: { value: EventStatus; label: string }[] = [
  { value: 'PROGRAMADO', label: 'Programado' },
  { value: 'EN_CURSO', label: 'En Curso' },
  { value: 'FINALIZADO', label: 'Finalizado' },
  { value: 'CANCELADO', label: 'Cancelado' },
];

const METHOD_LABELS: Record<string, string> = {
  QR: 'QR',
  MANUAL: 'Manual',
  NO_INSCRITO: 'No inscrito',
};

const EMPTY_FORM: EventFormState = {
  id: null,
  projectId: '',
  title: '',
  description: '',
  location: '',
  startsAt: '',
  endsAt: '',
  capacity: '',
  qrToken: '',
  status: 'PROGRAMADO',
};

const getStatusLabel = (status: EventStatus): string =>
  STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status;

const statusClass = (status: EventStatus): string =>
  `status-${status.toLowerCase().replace(/_/g, '-')}`;

/** "2026-10-28T09:00:00" -> "2026-10-28 09:00" */
const formatDateTime = (value: string): string => {
  if (!value) return '';
  if (value.includes('T')) {
    return `${value.slice(0, 10)} ${value.slice(11, 16)}`;
  }
  return value.slice(0, 16);
};

/** "2026-10-28T09:00:00" -> "2026-10-28T09:00" (valor de <input type="datetime-local">) */
const toInputDateTime = (value: string): string => {
  if (!value) return '';
  return value.slice(0, 16);
};

const describeError = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'Datos inválidos: revisa los campos y que la fecha de término sea posterior al inicio.';
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para esta acción (solo Docente · Coordinador).';
      case 404:
        return 'El evento o el proyecto ya no existe.';
      case 409:
        return 'El código QR ya está en uso. Usa otro o déjalo vacío para que se genere automáticamente.';
      case 0:
        return 'No se pudo conectar con el servidor.';
      default:
        return `No se pudo completar la operación (error ${error.status}).`;
    }
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Ocurrió un error inesperado.';
};

export const Events: React.FC = () => {
  const { user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const [events, setEvents] = useState<Event[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [projects, setProjects] = useState<ProjectOption[]>([]);
  const [projectsError, setProjectsError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [projectFilter, setProjectFilter] = useState<string>('ALL');

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<EventDetail | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [formMode, setFormMode] = useState<'create' | 'edit' | null>(null);
  const [form, setForm] = useState<EventFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadEvents = useCallback(async (filter: string): Promise<void> => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const query = filter === 'ALL' ? '' : `?projectId=${encodeURIComponent(filter)}`;
      const data = await apiRequest<Event[]>(`/api/events${query}`);
      setEvents(data);
    } catch (error) {
      setEvents([]);
      setLoadError(describeError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadProjects = useCallback(async (): Promise<void> => {
    setProjectsError(null);
    try {
      const data = await apiRequest<ProjectOption[]>('/api/projects');
      setProjects(data.map((project) => ({ id: project.id, code: project.code, name: project.name })));
    } catch (error) {
      setProjects([]);
      setProjectsError(`No se pudieron cargar los proyectos: ${describeError(error)}`);
    }
  }, []);

  useEffect(() => {
    void loadProjects();
  }, [loadProjects]);

  useEffect(() => {
    void loadEvents(projectFilter);
  }, [projectFilter, loadEvents]);

  const projectCodeById = new Map<number, string>(projects.map((project) => [project.id, project.code]));

  const openEvent = async (id: number): Promise<void> => {
    setSelectedId(id);
    setFormMode(null);
    setFormError(null);
    setDetailError(null);
    setNotice(null);
    setIsDetailLoading(true);
    try {
      const result = await apiRequest<EventDetail>(`/api/events/${id}/detail`);
      setDetail(result);
    } catch (error) {
      setDetail(null);
      setDetailError(describeError(error));
    } finally {
      setIsDetailLoading(false);
    }
  };

  const startCreate = (): void => {
    setForm({
      ...EMPTY_FORM,
      projectId: projectFilter !== 'ALL' ? projectFilter : projects[0] ? String(projects[0].id) : '',
    });
    setFormError(null);
    setNotice(null);
    setFormMode('create');
  };

  const startEdit = (): void => {
    if (!detail) return;
    const event = detail.event;
    setForm({
      id: event.id,
      projectId: String(event.projectId),
      title: event.title,
      description: event.description,
      location: event.location,
      startsAt: toInputDateTime(event.startsAt),
      endsAt: toInputDateTime(event.endsAt),
      capacity: String(event.capacity),
      qrToken: event.qrToken,
      status: event.status,
    });
    setFormError(null);
    setNotice(null);
    setFormMode('edit');
  };

  const cancelForm = (): void => {
    setFormMode(null);
    setFormError(null);
    setForm(EMPTY_FORM);
  };

  const validate = (): string | null => {
    if (!form.projectId) return 'Selecciona un proyecto.';
    if (!form.title.trim()) return 'El título es obligatorio.';
    if (form.title.trim().length > 180) return 'El título no puede superar 180 caracteres.';
    if (!form.description.trim()) return 'La descripción es obligatoria.';
    if (form.description.trim().length > 1200) return 'La descripción no puede superar 1200 caracteres.';
    if (!form.location.trim()) return 'La ubicación es obligatoria.';
    if (form.location.trim().length > 200) return 'La ubicación no puede superar 200 caracteres.';
    if (!form.startsAt) return 'La fecha de inicio es obligatoria.';
    if (!form.endsAt) return 'La fecha de término es obligatoria.';
    const start = new Date(form.startsAt);
    const end = new Date(form.endsAt);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return 'Las fechas no son válidas.';
    if (end.getTime() <= start.getTime()) return 'La fecha de término debe ser posterior al inicio.';
    const capacity = Number(form.capacity);
    if (!Number.isInteger(capacity) || capacity <= 0) return 'El cupo debe ser un número entero mayor a cero.';
    if (form.qrToken.trim().length > 120) return 'El código QR no puede superar 120 caracteres.';
    return null;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (isSaving) return;

    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      return;
    }

    const payload = {
      projectId: Number(form.projectId),
      title: form.title.trim(),
      description: form.description.trim(),
      location: form.location.trim(),
      startsAt: form.startsAt,
      endsAt: form.endsAt,
      capacity: Number(form.capacity),
      qrToken: form.qrToken.trim() ? form.qrToken.trim() : null,
      status: form.status,
    };

    setIsSaving(true);
    setFormError(null);
    try {
      let saved: Event;
      if (formMode === 'create') {
        saved = await apiRequest<Event>('/api/events', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
      } else if (formMode === 'edit' && form.id !== null) {
        saved = await apiRequest<Event>(`/api/events/${form.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        });
      } else {
        setFormError('No se pudo determinar la operación a realizar.');
        return;
      }
      setFormMode(null);
      setForm(EMPTY_FORM);
      await loadEvents(projectFilter);
      await openEvent(saved.id);
      setNotice(
        formMode === 'create'
          ? `Evento "${saved.title}" creado correctamente.`
          : `Evento "${saved.title}" actualizado correctamente.`,
      );
    } catch (error) {
      setFormError(describeError(error));
    } finally {
      setIsSaving(false);
    }
  };

  const getAvailability = (capacity: number, enrolled: number) => {
    const available = capacity - enrolled;
    if (available <= 0) return { text: 'Completo', className: 'full' };
    if (available <= 3) return { text: `${available} cupos`, className: 'limited' };
    return { text: `${available} cupos`, className: 'available' };
  };

  const filteredEvents = events.filter((item) => {
    const term = searchTerm.toLowerCase();
    const matchesSearch =
      item.title.toLowerCase().includes(term) ||
      item.projectName.toLowerCase().includes(term) ||
      item.location.toLowerCase().includes(term);
    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <AppLayout>
      <div className="events-page">
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">Eventos y Actividades</h1>
            <p className="page-subtitle">Datos en tiempo real desde la API de SyncPass</p>
          </div>
          {isCoordinator && (
            <Button
              type="button"
              variant="primary"
              onClick={startCreate}
              disabled={formMode === 'create' || projects.length === 0}
            >
              Nuevo Evento
            </Button>
          )}
        </div>

        <Card className="filters-card">
          <div className="filters-container">
            <div className="filter-group">
              <Input
                placeholder="Buscar por título, proyecto o ubicación..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="filter-group filter-select">
              <select
                className="status-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                aria-label="Filtrar por estado"
              >
                <option value="ALL">Todos los estados</option>
                {STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="filter-group filter-select">
              <select
                className="status-select"
                value={projectFilter}
                onChange={(e) => setProjectFilter(e.target.value)}
                aria-label="Filtrar por proyecto"
                disabled={projects.length === 0}
              >
                <option value="ALL">Todos los proyectos</option>
                {projects.map((project) => (
                  <option key={project.id} value={String(project.id)}>
                    {project.code} · {project.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </Card>

        {projectsError && (
          <div className="banner banner-error" role="alert">
            <span>{projectsError}</span>
            <Button type="button" variant="outline" onClick={() => void loadProjects()}>
              Reintentar
            </Button>
          </div>
        )}
        {notice && (
          <div className="banner banner-success" role="status" aria-live="polite">
            {notice}
          </div>
        )}
        {loadError && (
          <div className="banner banner-error" role="alert">
            <span>{loadError}</span>
            <Button type="button" variant="outline" onClick={() => void loadEvents(projectFilter)}>
              Reintentar
            </Button>
          </div>
        )}

        <div className="events-grid">
          <div className="events-list-section">
            <h2 className="section-title">Listado de Eventos ({filteredEvents.length})</h2>
            <div className="events-list">
              {isLoading ? (
                <p className="no-selection">Cargando eventos...</p>
              ) : filteredEvents.length === 0 ? (
                <p className="no-selection">
                  {events.length === 0
                    ? 'Todavía no hay eventos registrados para este filtro.'
                    : 'Ningún evento coincide con la búsqueda.'}
                </p>
              ) : (
                filteredEvents.map((item) => {
                  const availability = getAvailability(item.capacity, item.enrolledCount);
                  const projectCode = projectCodeById.get(item.projectId);
                  return (
                    <Card
                      key={item.id}
                      className={`event-item-card ${selectedId === item.id ? 'selected' : ''}`}
                    >
                      <div
                        className="event-item-content"
                        onClick={() => void openEvent(item.id)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            void openEvent(item.id);
                          }
                        }}
                      >
                        <div className="event-item-header">
                          <div>
                            {projectCode && <span className="project-badge">{projectCode}</span>}
                            <h3 className="event-item-title">{item.title}</h3>
                            <p className="event-project-name">{item.projectName}</p>
                          </div>
                          <span className={`event-status ${statusClass(item.status)}`}>
                            {getStatusLabel(item.status)}
                          </span>
                        </div>
                        <div className="event-item-info">
                          <p className="event-datetime">
                            📅 {formatDateTime(item.startsAt)} - {formatDateTime(item.endsAt)}
                          </p>
                          <p className="event-location">📍 {item.location}</p>
                          <div className="event-capacity-row">
                            <span className="event-capacity">
                              👥 {item.enrolledCount}/{item.capacity} inscritos
                            </span>
                            <span className={`availability-badge availability-${availability.className}`}>
                              {availability.text}
                            </span>
                          </div>
                        </div>
                      </div>
                    </Card>
                  );
                })
              )}
            </div>
          </div>

          <div className="event-detail-section">
            <h2 className="section-title">
              {formMode === 'create'
                ? 'Nuevo Evento'
                : formMode === 'edit'
                  ? 'Editar Evento'
                  : 'Detalle del Evento'}
            </h2>

            {formMode ? (
              <Card>
                <form className="event-form" onSubmit={handleSubmit} noValidate>
                  {formError && (
                    <div className="banner banner-error" role="alert">
                      {formError}
                    </div>
                  )}
                  <div className="input-group">
                    <label className="input-label" htmlFor="event-project">
                      Proyecto
                    </label>
                    <select
                      id="event-project"
                      className="input-field"
                      value={form.projectId}
                      disabled={isSaving || projects.length === 0}
                      onChange={(e) => setForm({ ...form, projectId: e.target.value })}
                    >
                      <option value="">Selecciona un proyecto</option>
                      {projects.map((project) => (
                        <option key={project.id} value={String(project.id)}>
                          {project.code} · {project.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <Input
                    label="Título"
                    value={form.title}
                    maxLength={180}
                    disabled={isSaving}
                    onChange={(e) => setForm({ ...form, title: e.target.value })}
                  />
                  <div className="input-group">
                    <label className="input-label" htmlFor="event-description">
                      Descripción
                    </label>
                    <textarea
                      id="event-description"
                      className="input-field form-textarea"
                      value={form.description}
                      maxLength={1200}
                      disabled={isSaving}
                      onChange={(e) => setForm({ ...form, description: e.target.value })}
                    />
                  </div>
                  <Input
                    label="Ubicación"
                    value={form.location}
                    maxLength={200}
                    disabled={isSaving}
                    onChange={(e) => setForm({ ...form, location: e.target.value })}
                  />
                  <div className="form-grid">
                    <Input
                      label="Inicio"
                      type="datetime-local"
                      value={form.startsAt}
                      disabled={isSaving}
                      onChange={(e) => setForm({ ...form, startsAt: e.target.value })}
                    />
                    <Input
                      label="Término"
                      type="datetime-local"
                      value={form.endsAt}
                      disabled={isSaving}
                      onChange={(e) => setForm({ ...form, endsAt: e.target.value })}
                    />
                  </div>
                  <div className="form-grid">
                    <Input
                      label="Cupo"
                      type="number"
                      min={1}
                      step={1}
                      value={form.capacity}
                      disabled={isSaving}
                      onChange={(e) => setForm({ ...form, capacity: e.target.value })}
                    />
                    <div className="input-group">
                      <label className="input-label" htmlFor="event-status">
                        Estado
                      </label>
                      <select
                        id="event-status"
                        className="input-field"
                        value={form.status}
                        disabled={isSaving}
                        onChange={(e) => setForm({ ...form, status: e.target.value as EventStatus })}
                      >
                        {STATUS_OPTIONS.map((option) => (
                          <option key={option.value} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                  <Input
                    label="Código QR (opcional)"
                    value={form.qrToken}
                    maxLength={120}
                    disabled={isSaving}
                    placeholder={
                      formMode === 'create'
                        ? 'Vacío para generar uno automáticamente'
                        : 'Vacío para conservar el actual'
                    }
                    onChange={(e) => setForm({ ...form, qrToken: e.target.value })}
                  />
                  <div className="form-actions">
                    <Button type="button" variant="outline" onClick={cancelForm} disabled={isSaving}>
                      Cancelar
                    </Button>
                    <Button type="submit" variant="primary" disabled={isSaving}>
                      {isSaving ? 'Guardando...' : formMode === 'create' ? 'Crear Evento' : 'Guardar Cambios'}
                    </Button>
                  </div>
                </form>
              </Card>
            ) : isDetailLoading ? (
              <Card>
                <p className="no-selection">Cargando detalle...</p>
              </Card>
            ) : detailError ? (
              <Card>
                <div className="banner banner-error" role="alert">
                  {detailError}
                </div>
              </Card>
            ) : detail ? (
              <Card>
                <div className="event-detail">
                  <div className="detail-header">
                    <div className="detail-badges">
                      {projectCodeById.get(detail.event.projectId) && (
                        <span className="project-badge">
                          {projectCodeById.get(detail.event.projectId)}
                        </span>
                      )}
                      <span className={`event-status ${statusClass(detail.event.status)}`}>
                        {getStatusLabel(detail.event.status)}
                      </span>
                    </div>
                    <h3 className="detail-title">{detail.event.title}</h3>
                    <p className="detail-project">{detail.event.projectName}</p>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Descripción</h4>
                    <p className="detail-text">{detail.event.description}</p>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Ubicación</h4>
                    <p className="detail-text">📍 {detail.event.location}</p>
                  </div>
                  <div className="detail-row">
                    <div className="detail-column">
                      <h4 className="detail-section-title">Inicio</h4>
                      <p className="detail-text">{formatDateTime(detail.event.startsAt)}</p>
                    </div>
                    <div className="detail-column">
                      <h4 className="detail-section-title">Término</h4>
                      <p className="detail-text">{formatDateTime(detail.event.endsAt)}</p>
                    </div>
                  </div>
                  <div className="detail-row">
                    <div className="detail-column">
                      <h4 className="detail-section-title">Capacidad</h4>
                      <p className="detail-text">{detail.event.capacity} personas</p>
                    </div>
                    <div className="detail-column">
                      <h4 className="detail-section-title">Inscritos</h4>
                      <p className="detail-text">
                        {detail.event.enrolledCount} personas · {detail.event.attendedCount} asistieron
                      </p>
                    </div>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Disponibilidad</h4>
                    <p className="detail-text">
                      {Math.max(detail.event.capacity - detail.event.enrolledCount, 0)} cupos disponibles
                    </p>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">
                      Participantes ({detail.participants.length})
                    </h4>
                    {detail.participants.length === 0 ? (
                      <p className="detail-text">
                        El servidor no devolvió participantes para este evento (la lista completa solo está
                        disponible para Docente · Coordinador).
                      </p>
                    ) : (
                      <ul className="participants-list">
                        {detail.participants.map((participant, index) => (
                          <li
                            className="participant-item"
                            key={`${participant.userId ?? 'anon'}-${index}`}
                          >
                            <div className="participant-main">
                              <span className="participant-name">{participant.fullName}</span>
                              <span className="participant-contact">
                                {participant.email || participant.username || 'Sin contacto'}
                              </span>
                            </div>
                            <div className="participant-tags">
                              {participant.enrolled && (
                                <span className="participant-tag tag-enrolled">Inscrito</span>
                              )}
                              {participant.attended && (
                                <span className="participant-tag tag-attended">
                                  Asistió{participant.method ? ` · ${METHOD_LABELS[participant.method] ?? participant.method}` : ''}
                                </span>
                              )}
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                  {isCoordinator && (
                    <div className="form-actions">
                      <Button type="button" variant="secondary" onClick={startEdit}>
                        Editar Evento
                      </Button>
                    </div>
                  )}
                </div>
              </Card>
            ) : (
              <Card>
                <p className="no-selection">Seleccione un evento para ver sus detalles</p>
              </Card>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  );
};
