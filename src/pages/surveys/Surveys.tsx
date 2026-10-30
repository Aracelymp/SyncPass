import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Surveys.css';

interface EventOption {
  id: number;
  title: string;
  projectName: string;
  startsAt: string;
}

interface SurveySummary {
  eventId: number;
  responseCount: number;
  averageRating: number;
  averageOrganization: number;
  averageImpact: number;
  recommendRate: number;
  comments: string[];
}

interface Survey {
  id: number;
  eventId: number;
  userId: number | null;
  participantName: string;
  rating: number;
  organizationRating: number;
  impactRating: number;
  comment: string | null;
  wouldRecommend: boolean;
  submittedAt: string;
  clientId: string;
}

interface SurveyFormState {
  rating: number | null;
  organizationRating: number | null;
  impactRating: number | null;
  wouldRecommend: boolean | null;
  comment: string;
}

const EMPTY_FORM: SurveyFormState = {
  rating: null,
  organizationRating: null,
  impactRating: null,
  wouldRecommend: null,
  comment: '',
};

const RATING_LABELS = ['Muy deficiente', 'Deficiente', 'Aceptable', 'Muy bueno', 'Excelente'];
const IMPACT_LABELS = ['Sin impacto', 'Bajo impacto', 'Impacto medio', 'Buen impacto', 'Alto impacto'];

/** "2026-10-28T09:00:00Z" -> "2026-10-28 09:00" */
const formatDateTime = (value: string | null): string => {
  if (!value) return '—';
  if (value.includes('T')) {
    return `${value.slice(0, 10)} ${value.slice(11, 16)}`;
  }
  return value.slice(0, 16);
};

const describeError = (error: unknown): string => {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 400:
        return 'Datos inválidos: las tres calificaciones son obligatorias y el comentario no puede superar 1000 caracteres.';
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para esta acción.';
      case 404:
        return 'El evento no existe o ya no está disponible.';
      case 409:
        return 'El servidor indica que ya enviaste la encuesta de este evento.';
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

export const Surveys: React.FC = () => {
  const { user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const [events, setEvents] = useState<EventOption[]>([]);
  const [isLoadingEvents, setIsLoadingEvents] = useState(true);
  const [eventsError, setEventsError] = useState<string | null>(null);
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);

  const [summary, setSummary] = useState<SurveySummary | null>(null);
  const [isLoadingSummary, setIsLoadingSummary] = useState(false);
  const [summaryError, setSummaryError] = useState<string | null>(null);

  const [surveyList, setSurveyList] = useState<Survey[]>([]);
  const [isLoadingList, setIsLoadingList] = useState(false);
  const [listError, setListError] = useState<string | null>(null);

  const [form, setForm] = useState<SurveyFormState>(EMPTY_FORM);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState<Survey | null>(null);

  const loadEvents = useCallback(async (): Promise<void> => {
    setIsLoadingEvents(true);
    setEventsError(null);
    try {
      const data = await apiRequest<EventOption[]>('/api/events');
      setEvents(data);
      setSelectedEventId((current) => {
        if (current !== null && data.some((event) => event.id === current)) {
          return current;
        }
        return data.length > 0 ? data[0].id : null;
      });
    } catch (error) {
      setEvents([]);
      setEventsError(describeError(error));
    } finally {
      setIsLoadingEvents(false);
    }
  }, []);

  const loadSummary = useCallback(async (id: number, silent = false): Promise<void> => {
    if (!silent) {
      setIsLoadingSummary(true);
      setSummary(null);
    }
    setSummaryError(null);
    try {
      const data = await apiRequest<SurveySummary>(`/api/events/${id}/survey/summary`);
      setSummary(data);
    } catch (error) {
      setSummary(null);
      setSummaryError(describeError(error));
    } finally {
      if (!silent) {
        setIsLoadingSummary(false);
      }
    }
  }, []);

  const loadSurveyList = useCallback(
    async (id: number, silent = false): Promise<void> => {
      if (!isCoordinator) {
        setSurveyList([]);
        setListError(null);
        return;
      }
      if (!silent) {
        setIsLoadingList(true);
      }
      setListError(null);
      try {
        const data = await apiRequest<Survey[]>(`/api/events/${id}/surveys`);
        setSurveyList(data);
      } catch (error) {
        setSurveyList([]);
        setListError(describeError(error));
      } finally {
        if (!silent) {
          setIsLoadingList(false);
        }
      }
    },
    [isCoordinator],
  );

  useEffect(() => {
    void loadEvents();
  }, [loadEvents]);

  useEffect(() => {
    if (selectedEventId === null) {
      setSummary(null);
      setSurveyList([]);
      return;
    }
    void loadSummary(selectedEventId);
    void loadSurveyList(selectedEventId);
  }, [selectedEventId, loadSummary, loadSurveyList]);

  const selectedEvent = events.find((event) => event.id === selectedEventId) ?? null;

  const isComplete =
    form.rating !== null &&
    form.organizationRating !== null &&
    form.impactRating !== null &&
    form.wouldRecommend !== null;

  const selectEvent = (value: string): void => {
    setSelectedEventId(value === '' ? null : Number(value));
    setForm(EMPTY_FORM);
    setFormError(null);
    setSubmitted(null);
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (isSubmitting || selectedEventId === null) return;

    if (
      form.rating === null ||
      form.organizationRating === null ||
      form.impactRating === null ||
      form.wouldRecommend === null
    ) {
      setFormError('Responde las cuatro preguntas antes de enviar.');
      return;
    }
    if (form.comment.trim().length > 1000) {
      setFormError('El comentario no puede superar 1000 caracteres.');
      return;
    }

    setIsSubmitting(true);
    setFormError(null);
    try {
      const created = await apiRequest<Survey>(`/api/events/${selectedEventId}/survey`, {
        method: 'POST',
        body: JSON.stringify({
          rating: form.rating,
          organizationRating: form.organizationRating,
          impactRating: form.impactRating,
          comment: form.comment.trim() ? form.comment.trim() : null,
          wouldRecommend: form.wouldRecommend,
        }),
      });
      setSubmitted(created);
      setForm(EMPTY_FORM);
      await loadSummary(selectedEventId, true);
      await loadSurveyList(selectedEventId, true);
    } catch (error) {
      setFormError(describeError(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderRatingGroup = (
    name: string,
    labels: string[],
    value: number | null,
    onSelect: (score: number) => void,
  ): React.ReactNode => (
    <div className="options-group">
      {labels.map((label, index) => {
        const score = index + 1;
        return (
          <label key={score} className="option-label">
            <input
              type="radio"
              name={name}
              value={String(score)}
              checked={value === score}
              onChange={() => onSelect(score)}
              disabled={isSubmitting}
              className="option-input"
            />
            <span className="option-text">
              {score} - {label}
            </span>
          </label>
        );
      })}
    </div>
  );

  return (
    <AppLayout>
      <div className="surveys-page">
        <div className="page-header">
          <h1 className="page-title">Encuesta de Satisfacción</h1>
          <p className="page-subtitle">Respuestas reales registradas en la API de SyncPass</p>
        </div>

        {eventsError && (
          <div className="banner banner-error" role="alert">
            <span>{eventsError}</span>
            <Button type="button" variant="outline" onClick={() => void loadEvents()}>
              Reintentar
            </Button>
          </div>
        )}

        <Card className="event-picker-card">
          <div className="event-picker">
            <label className="input-label" htmlFor="survey-event">
              Evento
            </label>
            <select
              id="survey-event"
              className="event-select"
              value={selectedEventId === null ? '' : String(selectedEventId)}
              onChange={(e) => selectEvent(e.target.value)}
              disabled={isLoadingEvents || events.length === 0}
            >
              {events.length === 0 && <option value="">Sin eventos disponibles</option>}
              {events.map((event) => (
                <option key={event.id} value={String(event.id)}>
                  {formatDateTime(event.startsAt)} · {event.title}
                </option>
              ))}
            </select>
            {isLoadingEvents && <p className="survey-note">Cargando eventos...</p>}
            {!isLoadingEvents && events.length === 0 && (
              <p className="survey-note">
                No hay eventos registrados. Crea uno en el módulo Eventos para habilitar la encuesta.
              </p>
            )}
          </div>
        </Card>

        {selectedEventId !== null && (
          <>
            {submitted ? (
              <Card>
                <div className="survey-success">
                  <div className="success-icon">✅</div>
                  <h2 className="success-title">Encuesta registrada</h2>
                  <p className="success-message">
                    El servidor guardó tu encuesta para “{selectedEvent?.title ?? 'el evento'}”.
                  </p>
                  <p className="success-note">
                    General {submitted.rating}/5 · Organización {submitted.organizationRating}/5 · Impacto{' '}
                    {submitted.impactRating}/5 · {submitted.wouldRecommend ? 'Recomienda' : 'No recomienda'} ·{' '}
                    {formatDateTime(submitted.submittedAt)}
                  </p>
                </div>
              </Card>
            ) : (
              <Card>
                <div className="survey-header">
                  <h2 className="survey-event-title">
                    Evento: {selectedEvent?.title ?? '—'}
                  </h2>
                  <p className="survey-project">
                    Proyecto: {selectedEvent?.projectName ?? '—'}
                  </p>
                  <p className="survey-note">Por favor, califique cada aspecto del evento</p>
                </div>

                <form onSubmit={handleSubmit} className="survey-form" noValidate>
                  {formError && (
                    <div className="banner banner-error" role="alert">
                      {formError}
                    </div>
                  )}

                  <div className="question-block">
                    <h3 className="question-number">Pregunta 1</h3>
                    <p className="question-text">¿Cómo calificaría el evento en general?</p>
                    {renderRatingGroup('rating', RATING_LABELS, form.rating, (score) =>
                      setForm({ ...form, rating: score }),
                    )}
                  </div>

                  <div className="question-block">
                    <h3 className="question-number">Pregunta 2</h3>
                    <p className="question-text">¿Cómo calificaría la organización del evento?</p>
                    {renderRatingGroup(
                      'organizationRating',
                      RATING_LABELS,
                      form.organizationRating,
                      (score) => setForm({ ...form, organizationRating: score }),
                    )}
                  </div>

                  <div className="question-block">
                    <h3 className="question-number">Pregunta 3</h3>
                    <p className="question-text">¿Cómo evalúa el impacto de la actividad en la comunidad?</p>
                    {renderRatingGroup('impactRating', IMPACT_LABELS, form.impactRating, (score) =>
                      setForm({ ...form, impactRating: score }),
                    )}
                  </div>

                  <div className="question-block">
                    <h3 className="question-number">Pregunta 4</h3>
                    <p className="question-text">¿Recomendaría este tipo de actividades?</p>
                    <div className="options-group">
                      <label className="option-label">
                        <input
                          type="radio"
                          name="wouldRecommend"
                          value="yes"
                          checked={form.wouldRecommend === true}
                          onChange={() => setForm({ ...form, wouldRecommend: true })}
                          disabled={isSubmitting}
                          className="option-input"
                        />
                        <span className="option-text">Sí, la recomiendo</span>
                      </label>
                      <label className="option-label">
                        <input
                          type="radio"
                          name="wouldRecommend"
                          value="no"
                          checked={form.wouldRecommend === false}
                          onChange={() => setForm({ ...form, wouldRecommend: false })}
                          disabled={isSubmitting}
                          className="option-input"
                        />
                        <span className="option-text">No la recomiendo</span>
                      </label>
                    </div>
                  </div>

                  <div className="question-block">
                    <h3 className="question-number">Comentario (opcional)</h3>
                    <label className="input-label" htmlFor="survey-comment">
                      Comentario
                    </label>
                    <textarea
                      id="survey-comment"
                      className="input-field comment-field"
                      value={form.comment}
                      maxLength={1000}
                      disabled={isSubmitting}
                      onChange={(e) => setForm({ ...form, comment: e.target.value })}
                      placeholder="Máximo 1000 caracteres"
                    />
                  </div>

                  <div className="survey-footer">
                    <Button
                      type="submit"
                      variant="primary"
                      disabled={!isComplete || isSubmitting}
                      className="submit-survey-btn"
                    >
                      {isSubmitting
                        ? 'Enviando...'
                        : isComplete
                          ? 'Enviar Encuesta'
                          : 'Complete todas las preguntas'}
                    </Button>
                  </div>
                </form>
              </Card>
            )}

            <Card title="Resumen de encuestas">
              {isLoadingSummary ? (
                <p className="no-selection">Cargando resumen...</p>
              ) : summaryError ? (
                <div className="banner banner-error" role="alert">
                  <span>{summaryError}</span>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => void loadSummary(selectedEventId)}
                  >
                    Reintentar
                  </Button>
                </div>
              ) : summary ? (
                <>
                  <div className="survey-stats">
                    <div className="survey-stat">
                      <span className="survey-stat-label">Respuestas</span>
                      <span className="survey-stat-value">{summary.responseCount}</span>
                    </div>
                    <div className="survey-stat">
                      <span className="survey-stat-label">Promedio general</span>
                      <span className="survey-stat-value">
                        {summary.responseCount > 0 ? `${summary.averageRating.toFixed(1)}/5` : '—'}
                      </span>
                    </div>
                    <div className="survey-stat">
                      <span className="survey-stat-label">Organización</span>
                      <span className="survey-stat-value">
                        {summary.responseCount > 0 ? `${summary.averageOrganization.toFixed(1)}/5` : '—'}
                      </span>
                    </div>
                    <div className="survey-stat">
                      <span className="survey-stat-label">Impacto</span>
                      <span className="survey-stat-value">
                        {summary.responseCount > 0 ? `${summary.averageImpact.toFixed(1)}/5` : '—'}
                      </span>
                    </div>
                    <div className="survey-stat">
                      <span className="survey-stat-label">Recomendación</span>
                      <span className="survey-stat-value">
                        {summary.responseCount > 0 ? `${summary.recommendRate.toFixed(1)}%` : '—'}
                      </span>
                    </div>
                  </div>

                  {summary.responseCount === 0 ? (
                    <p className="no-selection">Todavía no hay encuestas registradas para este evento.</p>
                  ) : (
                    <div className="comments-block">
                      <h4 className="comments-title">Comentarios</h4>
                      {summary.comments.length === 0 ? (
                        <p className="comments-empty">Sin comentarios registrados.</p>
                      ) : (
                        <ul className="comments-list">
                          {summary.comments.map((comment, index) => (
                            <li key={index} className="comment-item">
                              {comment}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </>
              ) : (
                <p className="no-selection">Selecciona un evento para ver el resumen.</p>
              )}
            </Card>

            {isCoordinator && (
              <Card title="Encuestas recibidas">
                {isLoadingList ? (
                  <p className="no-selection">Cargando encuestas...</p>
                ) : listError ? (
                  <div className="banner banner-error" role="alert">
                    <span>{listError}</span>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => void loadSurveyList(selectedEventId)}
                    >
                      Reintentar
                    </Button>
                  </div>
                ) : surveyList.length === 0 ? (
                  <p className="no-selection">Sin encuestas registradas para este evento.</p>
                ) : (
                  <div className="responses-table-container">
                    <table className="responses-table">
                      <thead>
                        <tr>
                          <th>Participante</th>
                          <th>General</th>
                          <th>Organización</th>
                          <th>Impacto</th>
                          <th>Recomienda</th>
                          <th>Comentario</th>
                          <th>Enviada</th>
                        </tr>
                      </thead>
                      <tbody>
                        {surveyList.map((survey) => (
                          <tr key={survey.id}>
                            <td>{survey.participantName}</td>
                            <td className="center-cell">{survey.rating}/5</td>
                            <td className="center-cell">{survey.organizationRating}/5</td>
                            <td className="center-cell">{survey.impactRating}/5</td>
                            <td className="center-cell">{survey.wouldRecommend ? 'Sí' : 'No'}</td>
                            <td className="comment-cell">{survey.comment || '—'}</td>
                            <td className="center-cell">{formatDateTime(survey.submittedAt)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Card>
            )}
          </>
        )}
      </div>
    </AppLayout>
  );
};
