import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Projects.css';

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
}

interface ProjectFormState {
  id: number | null;
  code: string;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
  status: ProjectStatus;
  responsible: string;
}

const STATUS_OPTIONS: { value: ProjectStatus; label: string }[] = [
  { value: 'PLANIFICADO', label: 'Planificado' },
  { value: 'EN_CURSO', label: 'En Curso' },
  { value: 'FINALIZADO', label: 'Finalizado' },
];

const EMPTY_FORM: ProjectFormState = {
  id: null,
  code: '',
  name: '',
  description: '',
  startDate: '',
  endDate: '',
  status: 'PLANIFICADO',
  responsible: '',
};

const getStatusLabel = (status: ProjectStatus): string =>
  STATUS_OPTIONS.find((option) => option.value === status)?.label ?? status;

const describeError = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'Datos inválidos: revisa los campos y que la fecha de término no sea anterior al inicio.';
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para esta acción (solo Docente · Coordinador).';
      case 404:
        return 'El proyecto ya no existe.';
      case 409:
        return 'Ya existe un proyecto con ese código.';
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

export const Projects: React.FC = () => {
  const { user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [formMode, setFormMode] = useState<'create' | 'edit' | null>(null);
  const [form, setForm] = useState<ProjectFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const loadProjects = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setLoadError(null);
    try {
      const data = await apiRequest<Project[]>('/api/projects');
      setProjects(data);
    } catch (error) {
      setLoadError(describeError(error));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadProjects();
  }, [loadProjects]);

  const openProject = async (id: number): Promise<void> => {
    setSelectedId(id);
    setFormMode(null);
    setFormError(null);
    setDetailError(null);
    setNotice(null);
    setIsDetailLoading(true);
    try {
      const project = await apiRequest<Project>(`/api/projects/${id}`);
      setSelectedProject(project);
    } catch (error) {
      setDetailError(describeError(error));
    } finally {
      setIsDetailLoading(false);
    }
  };

  const startCreate = (): void => {
    setForm(EMPTY_FORM);
    setFormError(null);
    setNotice(null);
    setFormMode('create');
  };

  const startEdit = (): void => {
    if (!selectedProject) return;
    setForm({
      id: selectedProject.id,
      code: selectedProject.code,
      name: selectedProject.name,
      description: selectedProject.description,
      startDate: selectedProject.startDate,
      endDate: selectedProject.endDate,
      status: selectedProject.status,
      responsible: selectedProject.responsible,
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
    if (!form.code.trim()) return 'El código es obligatorio.';
    if (form.code.trim().length > 40) return 'El código no puede superar 40 caracteres.';
    if (!form.name.trim()) return 'El nombre es obligatorio.';
    if (form.name.trim().length > 160) return 'El nombre no puede superar 160 caracteres.';
    if (!form.description.trim()) return 'La descripción es obligatoria.';
    if (form.description.trim().length > 1200) return 'La descripción no puede superar 1200 caracteres.';
    if (!form.responsible.trim()) return 'El responsable es obligatorio.';
    if (form.responsible.trim().length > 160) return 'El responsable no puede superar 160 caracteres.';
    if (!form.startDate) return 'La fecha de inicio es obligatoria.';
    if (!form.endDate) return 'La fecha de término es obligatoria.';
    if (form.endDate < form.startDate) return 'La fecha de término no puede ser anterior al inicio.';
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
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
      status: form.status,
      responsible: form.responsible.trim(),
    };

    setIsSaving(true);
    setFormError(null);
    try {
      if (formMode === 'create') {
        const created = await apiRequest<Project>('/api/projects', {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        setProjects((previous) => [created, ...previous]);
        setSelectedProject(created);
        setSelectedId(created.id);
        setDetailError(null);
        setNotice(`Proyecto "${created.name}" creado correctamente.`);
      } else if (formMode === 'edit' && form.id !== null) {
        const updated = await apiRequest<Project>(`/api/projects/${form.id}`, {
          method: 'PUT',
          body: JSON.stringify(payload),
        });
        setProjects((previous) => previous.map((project) => (project.id === updated.id ? updated : project)));
        setSelectedProject(updated);
        setSelectedId(updated.id);
        setDetailError(null);
        setNotice(`Proyecto "${updated.name}" actualizado correctamente.`);
      } else {
        setFormError('No se pudo determinar la operación a realizar.');
        return;
      }
      setFormMode(null);
      setForm(EMPTY_FORM);
    } catch (error) {
      setFormError(describeError(error));
    } finally {
      setIsSaving(false);
    }
  };

  const filteredProjects = projects.filter((project) => {
    const term = searchTerm.toLowerCase();
    return (
      project.name.toLowerCase().includes(term) ||
      project.code.toLowerCase().includes(term) ||
      project.responsible.toLowerCase().includes(term)
    );
  });

  return (
    <AppLayout>
      <div className="projects-page">
        <div className="page-header-row">
          <div className="page-header">
            <h1 className="page-title">Proyectos Comunitarios</h1>
            <p className="page-subtitle">Datos en tiempo real desde la API de SyncPass</p>
          </div>
          {isCoordinator && (
            <Button type="button" variant="primary" onClick={startCreate} disabled={formMode === 'create'}>
              Nuevo Proyecto
            </Button>
          )}
        </div>

        <Card className="search-card">
          <div className="search-container">
            <Input
              placeholder="Buscar por nombre, código o responsable..."
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
          </div>
        </Card>

        {notice && (
          <div className="banner banner-success" role="status" aria-live="polite">
            {notice}
          </div>
        )}
        {loadError && (
          <div className="banner banner-error" role="alert">
            <span>{loadError}</span>
            <Button type="button" variant="outline" onClick={() => void loadProjects()}>
              Reintentar
            </Button>
          </div>
        )}

        <div className="projects-grid">
          <div className="projects-list-section">
            <h2 className="section-title">Listado de Proyectos ({filteredProjects.length})</h2>
            <div className="projects-list">
              {isLoading ? (
                <p className="no-selection">Cargando proyectos...</p>
              ) : filteredProjects.length === 0 ? (
                <p className="no-selection">
                  {projects.length === 0
                    ? 'Todavía no hay proyectos registrados.'
                    : 'Ningún proyecto coincide con la búsqueda.'}
                </p>
              ) : (
                filteredProjects.map((project) => (
                  <Card
                    key={project.id}
                    className={`project-item-card ${selectedId === project.id ? 'selected' : ''}`}
                  >
                    <div
                      className="project-item-content"
                      onClick={() => void openProject(project.id)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          void openProject(project.id);
                        }
                      }}
                    >
                      <div className="project-item-header">
                        <div>
                          <span className="project-code">{project.code}</span>
                          <h3 className="project-item-title">{project.name}</h3>
                        </div>
                        <span className={`project-status status-${project.status.toLowerCase().replace(/_/g, '-')}`}>
                          {getStatusLabel(project.status)}
                        </span>
                      </div>
                      <p className="project-responsible">Responsable: {project.responsible}</p>
                      <p className="project-events">Eventos asociados: {project.eventCount}</p>
                    </div>
                  </Card>
                ))
              )}
            </div>
          </div>

          <div className="project-detail-section">
            <h2 className="section-title">
              {formMode === 'create' ? 'Nuevo Proyecto' : formMode === 'edit' ? 'Editar Proyecto' : 'Detalle del Proyecto'}
            </h2>

            {formMode ? (
              <Card>
                <form className="project-form" onSubmit={handleSubmit} noValidate>
                  {formError && (
                    <div className="banner banner-error" role="alert">
                      {formError}
                    </div>
                  )}
                  <div className="form-grid">
                    <Input
                      label="Código"
                      value={form.code}
                      maxLength={40}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, code: event.target.value })}
                    />
                    <Input
                      label="Responsable"
                      value={form.responsible}
                      maxLength={160}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, responsible: event.target.value })}
                    />
                  </div>
                  <Input
                    label="Nombre"
                    value={form.name}
                    maxLength={160}
                    disabled={isSaving}
                    onChange={(event) => setForm({ ...form, name: event.target.value })}
                  />
                  <div className="input-group">
                    <label className="input-label" htmlFor="project-description">
                      Descripción
                    </label>
                    <textarea
                      id="project-description"
                      className="input-field form-textarea"
                      value={form.description}
                      maxLength={1200}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, description: event.target.value })}
                    />
                  </div>
                  <div className="form-grid">
                    <Input
                      label="Fecha de Inicio"
                      type="date"
                      value={form.startDate}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, startDate: event.target.value })}
                    />
                    <Input
                      label="Fecha de Término"
                      type="date"
                      value={form.endDate}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, endDate: event.target.value })}
                    />
                  </div>
                  <div className="input-group">
                    <label className="input-label" htmlFor="project-status">
                      Estado
                    </label>
                    <select
                      id="project-status"
                      className="input-field"
                      value={form.status}
                      disabled={isSaving}
                      onChange={(event) => setForm({ ...form, status: event.target.value as ProjectStatus })}
                    >
                      {STATUS_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="form-actions">
                    <Button type="button" variant="outline" onClick={cancelForm} disabled={isSaving}>
                      Cancelar
                    </Button>
                    <Button type="submit" variant="primary" disabled={isSaving}>
                      {isSaving ? 'Guardando...' : formMode === 'create' ? 'Crear Proyecto' : 'Guardar Cambios'}
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
            ) : selectedProject ? (
              <Card>
                <div className="project-detail">
                  <div className="detail-header">
                    <span className="project-code">{selectedProject.code}</span>
                    <h3 className="detail-title">{selectedProject.name}</h3>
                    <span
                      className={`project-status status-${selectedProject.status.toLowerCase().replace(/_/g, '-')}`}
                    >
                      {getStatusLabel(selectedProject.status)}
                    </span>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Descripción</h4>
                    <p className="detail-text">{selectedProject.description}</p>
                  </div>
                  <div className="detail-row">
                    <div className="detail-column">
                      <h4 className="detail-section-title">Fecha de Inicio</h4>
                      <p className="detail-text">{selectedProject.startDate}</p>
                    </div>
                    <div className="detail-column">
                      <h4 className="detail-section-title">Fecha de Término</h4>
                      <p className="detail-text">{selectedProject.endDate}</p>
                    </div>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Responsable</h4>
                    <p className="detail-text">{selectedProject.responsible}</p>
                  </div>
                  <div className="detail-section">
                    <h4 className="detail-section-title">Eventos Asociados</h4>
                    <p className="detail-text">{selectedProject.eventCount} evento(s)</p>
                  </div>
                  {isCoordinator && (
                    <div className="form-actions">
                      <Button type="button" variant="secondary" onClick={startEdit}>
                        Editar Proyecto
                      </Button>
                    </div>
                  )}
                </div>
              </Card>
            ) : (
              <Card>
                <p className="no-selection">Seleccione un proyecto para ver sus detalles</p>
              </Card>
            )}
          </div>
        </div>
      </div>
    </AppLayout>
  );
};
