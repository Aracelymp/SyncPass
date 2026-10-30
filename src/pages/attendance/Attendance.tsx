import React, { useCallback, useEffect, useState } from 'react';
import { AppLayout } from '../../components/layout/AppLayout';
import { Card } from '../../components/ui/Card';
import { Input } from '../../components/ui/Input';
import { Button } from '../../components/ui/Button';
import { useAuth } from '../../context/AuthContext';
import { ApiError, apiRequest } from '../../services/apiClient';
import './Attendance.css';

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

interface EventSummary {
  id: number;
  projectName: string;
  title: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
  enrolledCount: number;
  attendedCount: number;
}

interface EventDetail {
  event: EventSummary;
  participants: Participant[];
}

interface EventOption {
  id: number;
  title: string;
  projectName: string;
  startsAt: string;
}

interface AttendanceRecord {
  id: number;
  eventId: number;
  eventTitle: string;
  participantName: string;
  method: string;
  registeredAt: string;
}

interface WalkInFormState {
  participantName: string;
  participantEmail: string;
  participantDocument: string;
}

const EMPTY_WALK_IN: WalkInFormState = {
  participantName: '',
  participantEmail: '',
  participantDocument: '',
};

const METHOD_LABELS: Record<string, string> = {
  QR: 'QR',
  MANUAL: 'Manual',
  NO_INSCRITO: 'Walk-in',
};

const getMethodLabel = (method: string | null): string => {
  if (!method) return '—';
  return METHOD_LABELS[method] ?? method;
};

/** "2026-10-28T09:00:00" -> "2026-10-28 09:00" */
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
        return 'Datos inválidos: revisa el nombre, el correo, el documento o el código QR.';
      case 401:
        return 'Tu sesión expiró. Vuelve a iniciar sesión.';
      case 403:
        return 'No tienes permisos para esta acción (solo Docente · Coordinador).';
      case 404:
        return 'No se encontró el evento o el código QR no es reconocido.';
      case 409:
        return 'El servidor rechazó el registro por un conflicto (por ejemplo, cupo lleno o código QR en uso).';
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

export const Attendance: React.FC = () => {
  const { user } = useAuth();
  const isCoordinator = user?.role === 'DOCENTE_COORDINADOR';

  const [events, setEvents] = useState<EventOption[]>([]);
  const [isLoadingEvents, setIsLoadingEvents] = useState(true);
  const [eventsError, setEventsError] = useState<string | null>(null);

  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
  const [detail, setDetail] = useState<EventDetail | null>(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');

  const [qrCode, setQrCode] = useState('');
  const [isScanning, setIsScanning] = useState(false);
  const [scanError, setScanError] = useState<string | null>(null);

  const [walkIn, setWalkIn] = useState<WalkInFormState>(EMPTY_WALK_IN);
  const [isRegistering, setIsRegistering] = useState(false);
  const [walkInError, setWalkInError] = useState<string | null>(null);

  const [notice, setNotice] = useState<string | null>(null);
  const [warning, setWarning] = useState<string | null>(null);

  const loadDetail = useCallback(async (id: number, silent = false): Promise<void> => {
    if (!silent) {
      setIsLoadingDetail(true);
      setDetail(null);
      setNotice(null);
      setWarning(null);
    }
    setDetailError(null);
    try {
      const result = await apiRequest<EventDetail>(`/api/events/${id}/detail`);
      setDetail(result);
    } catch (error) {
      setDetail(null);
      setDetailError(describeError(error));
    } finally {
      if (!silent) {
        setIsLoadingDetail(false);
      }
    }
  }, []);

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

  useEffect(() => {
    void loadEvents();
  }, [loadEvents]);

  useEffect(() => {
    if (selectedEventId === null) {
      setDetail(null);
      return;
    }
    void loadDetail(selectedEventId);
  }, [selectedEventId, loadDetail]);

  const participants = detail?.participants ?? [];

  const filteredParticipants = participants.filter((participant) => {
    const term = searchTerm.toLowerCase();
    return (
      participant.fullName.toLowerCase().includes(term) ||
      (participant.email ?? '').toLowerCase().includes(term) ||
      (participant.username ?? '').toLowerCase().includes(term)
    );
  });

  const attendedCount = participants.filter((participant) => participant.attended).length;
  const pendingCount = participants.filter(
    (participant) => participant.enrolled && !participant.attended,
  ).length;
  const isCurrentUser = (participant: Participant): boolean =>
    participant.userId !== null && user?.id !== undefined && String(participant.userId) === String(user.id);

  const handleScan = async (event: React.FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (isScanning || selectedEventId === null) return;

    const code = qrCode.trim();
    if (!code) {
      setScanError('Introduce el código QR del evento.');
      return;
    }
    if (code.length > 500) {
      setScanError('El código QR introducido es demasiado largo.');
      return;
    }
    if (code.startsWith('SYNCPASS|EVENT|')) {
      const parts = code.split('|');
      if (parts.length < 4) {
        setScanError('El código QR no tiene el formato esperado (SYNCPASS|EVENT|id|token).');
        return;
      }
      if (parts[2] !== String(selectedEventId)) {
        setScanError(`El código QR pertenece al evento #${parts[2]}, no al evento seleccionado.`);
        return;
      }
    }

    setIsScanning(true);
    setScanError(null);
    setNotice(null);
    setWarning(null);
    try {
      const record = await apiRequest<AttendanceRecord>(
        `/api/events/${selectedEventId}/attendance/scan`,
        { method: 'POST', body: JSON.stringify({ qrCode: code }) },
      );
      setQrCode('');
      await loadDetail(selectedEventId, true);
      if (record.eventId !== selectedEventId) {
        setWarning(
          `El servidor registró la asistencia en el evento #${record.eventId} ("${record.eventTitle}"), no en el evento seleccionado.`,
        );
      } else {
        setNotice(
          `Asistencia confirmada por el servidor: ${record.participantName} · ${formatDateTime(record.registeredAt)} · método ${getMethodLabel(record.method)}.`,
        );
      }
    } catch (error) {
      setScanError(describeError(error));
    } finally {
      setIsScanning(false);
    }
  };

  const handleWalkIn = async (event: React.FormEvent<HTMLFormElement>): Promise<void> => {
    event.preventDefault();
    if (isRegistering || selectedEventId === null) return;

    const name = walkIn.participantName.trim();
    const email = walkIn.participantEmail.trim();
    const document = walkIn.participantDocument.trim();

    if (!name) {
      setWalkInError('El nombre del participante es obligatorio.');
      return;
    }
    if (name.length > 180) {
      setWalkInError('El nombre no puede superar 180 caracteres.');
      return;
    }
    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setWalkInError('El correo no tiene un formato válido.');
      return;
    }
    if (email.length > 160) {
      setWalkInError('El correo no puede superar 160 caracteres.');
      return;
    }
    if (document.length > 60) {
      setWalkInError('El documento no puede superar 60 caracteres.');
      return;
    }

    setIsRegistering(true);
    setWalkInError(null);
    setNotice(null);
    setWarning(null);
    try {
      const record = await apiRequest<AttendanceRecord>(
        `/api/events/${selectedEventId}/attendance/walk-in`,
        {
          method: 'POST',
          body: JSON.stringify({
            participantName: name,
            participantEmail: email ? email : null,
            participantDocument: document ? document : null,
          }),
        },
      );
      setWalkIn(EMPTY_WALK_IN);
      await loadDetail(selectedEventId, true);
      if (record.eventId !== selectedEventId) {
        setWarning(
          `El servidor registró la asistencia en el evento #${record.eventId} ("${record.eventTitle}"), no en el evento seleccionado.`,
        );
      } else {
        setNotice(
          `Asistencia registrada por el servidor para ${record.participantName} · ${formatDateTime(record.registeredAt)} · método ${getMethodLabel(record.method)}.`,
        );
      }
    } catch (error) {
      setWalkInError(describeError(error));
    } finally {
      setIsRegistering(false);
    }
  };

  const selectedEvent = events.find((event) => event.id === selectedEventId) ?? null;

  return (
    <AppLayout>
      <div className="attendance-page">
        <div className="page-header">
          <h1 className="page-title">Asistencia</h1>
          <p className="page-subtitle">Registros reales desde la API de SyncPass</p>
        </div>

        {eventsError && (
          <div className="banner banner-error" role="alert">
            <span>{eventsError}</span>
            <Button type="button" variant="outline" onClick={() => void loadEvents()}>
              Reintentar
            </Button>
          </div>
        )}
        {notice && (
          <div className="banner banner-success" role="status" aria-live="polite">
            {notice}
          </div>
        )}
        {warning && (
          <div className="banner banner-warning" role="alert">
            {warning}
          </div>
        )}

        <div className="attendance-grid">
          <div className="attendance-main">
            <Card className="filters-card">
              <div className="filters-container">
                <div className="filter-group">
                  <Input
                    placeholder="Buscar por nombre, correo o usuario..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                  />
                </div>
                <div className="filter-group filter-select">
                  <select
                    className="event-select"
                    value={selectedEventId === null ? '' : String(selectedEventId)}
                    onChange={(e) => {
                      const value = e.target.value;
                      setSelectedEventId(value === '' ? null : Number(value));
                    }}
                    disabled={isLoadingEvents || events.length === 0}
                    aria-label="Seleccionar evento"
                  >
                    {events.length === 0 && <option value="">Sin eventos disponibles</option>}
                    {events.map((event) => (
                      <option key={event.id} value={String(event.id)}>
                        {formatDateTime(event.startsAt)} · {event.title}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </Card>

            <Card title="Participantes y Estado de Asistencia">
              {isLoadingEvents || isLoadingDetail ? (
                <p className="no-selection">Cargando participantes...</p>
              ) : selectedEventId === null ? (
                <p className="no-selection">
                  No hay eventos registrados. Crea un evento en el módulo Eventos para registrar asistencia.
                </p>
              ) : detailError ? (
                <div className="banner banner-error" role="alert">
                  <span>{detailError}</span>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => void loadDetail(selectedEventId)}
                  >
                    Reintentar
                  </Button>
                </div>
              ) : detail ? (
                <>
                  <p className="event-context">
                    {detail.event.projectName} · Cupo {detail.event.capacity} · {detail.event.enrolledCount}{' '}
                    inscrito(s)
                  </p>
                  <div className="participants-table-container">
                    <table className="participants-table">
                      <thead>
                        <tr>
                          <th>Participante</th>
                          <th>Inscrito</th>
                          <th>Estado</th>
                          <th>Método</th>
                          <th>Registrado</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filteredParticipants.length === 0 ? (
                          <tr>
                            <td className="empty-row" colSpan={5}>
                              {participants.length === 0
                                ? 'El servidor no devolvió participantes para este evento (la lista completa solo está disponible para Docente · Coordinador).'
                                : 'Ningún participante coincide con la búsqueda.'}
                            </td>
                          </tr>
                        ) : (
                          filteredParticipants.map((participant, index) => (
                            <tr key={`${participant.userId ?? 'anon'}-${index}`}>
                              <td>
                                <div className="participant-info">
                                  <div className="participant-name">
                                    {participant.fullName}
                                    {isCurrentUser(participant) && <span className="you-tag"> (tú)</span>}
                                  </div>
                                  <div className="participant-email">
                                    {participant.email || participant.username || 'Sin contacto'}
                                  </div>
                                </div>
                              </td>
                              <td className="center-cell">
                                <span className={`badge ${participant.enrolled ? 'badge-yes' : 'badge-no'}`}>
                                  {participant.enrolled ? 'Sí' : 'No'}
                                </span>
                              </td>
                              <td className="center-cell">
                                <span
                                  className={`status-badge status-${participant.attended ? 'attended' : 'pending'}`}
                                >
                                  {participant.attended ? 'Asistió' : 'Pendiente'}
                                </span>
                              </td>
                              <td className="center-cell">{getMethodLabel(participant.method)}</td>
                              <td className="center-cell">{formatDateTime(participant.registeredAt)}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </>
              ) : (
                <p className="no-selection">Selecciona un evento para ver sus participantes.</p>
              )}
            </Card>
          </div>

          <div className="attendance-sidebar">
            <Card title="Registro por QR (manual)">
              <form className="side-form" onSubmit={handleScan}>
                <p className="side-note">
                  Sin cámara: introduce el token del evento o el contenido del QR
                  (<code>SYNCPASS|EVENT|id|token</code>). El servidor registra la asistencia del
                  usuario autenticado.
                </p>
                {scanError && (
                  <div className="banner banner-error" role="alert">
                    {scanError}
                  </div>
                )}
                <Input
                  label="Código QR"
                  value={qrCode}
                  placeholder="SP-XXXXXXXX"
                  disabled={isScanning || selectedEventId === null}
                  onChange={(e) => setQrCode(e.target.value)}
                />
                <Button
                  type="submit"
                  variant="primary"
                  disabled={isScanning || selectedEventId === null}
                >
                  {isScanning ? 'Registrando...' : 'Registrar con QR'}
                </Button>
              </form>
            </Card>

            <Card title="Registro Walk-in">
              {isCoordinator ? (
                <form className="side-form" onSubmit={handleWalkIn}>
                  <p className="side-note">
                    Registra a una persona que asistió sin inscripción previa.
                  </p>
                  {walkInError && (
                    <div className="banner banner-error" role="alert">
                      {walkInError}
                    </div>
                  )}
                  <Input
                    label="Nombre"
                    value={walkIn.participantName}
                    maxLength={180}
                    disabled={isRegistering || selectedEventId === null}
                    onChange={(e) => setWalkIn({ ...walkIn, participantName: e.target.value })}
                  />
                  <Input
                    label="Correo (opcional)"
                    type="email"
                    value={walkIn.participantEmail}
                    maxLength={160}
                    disabled={isRegistering || selectedEventId === null}
                    onChange={(e) => setWalkIn({ ...walkIn, participantEmail: e.target.value })}
                  />
                  <Input
                    label="Documento (opcional)"
                    value={walkIn.participantDocument}
                    maxLength={60}
                    disabled={isRegistering || selectedEventId === null}
                    onChange={(e) => setWalkIn({ ...walkIn, participantDocument: e.target.value })}
                  />
                  <Button
                    type="submit"
                    variant="secondary"
                    disabled={isRegistering || selectedEventId === null}
                  >
                    {isRegistering ? 'Registrando...' : 'Registrar walk-in'}
                  </Button>
                </form>
              ) : (
                <p className="side-note">
                  Solo Docente · Coordinador puede registrar participantes no inscritos. Tu rol actual no
                  tiene ese permiso.
                </p>
              )}
            </Card>

            <Card title="Resumen">
              <div className="summary-stats">
                <div className="summary-item">
                  <span className="summary-label">Inscritos</span>
                  <span className="summary-value">{detail ? detail.event.enrolledCount : '—'}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Con asistencia</span>
                  <span className="summary-value attended-count">{detail ? attendedCount : '—'}</span>
                </div>
                <div className="summary-item">
                  <span className="summary-label">Pendientes</span>
                  <span className="summary-value pending-count">{detail ? pendingCount : '—'}</span>
                </div>
              </div>
              {selectedEvent && detail && (
                <p className="side-note summary-note">
                  {detail.event.title} · {formatDateTime(detail.event.startsAt)}
                </p>
              )}
            </Card>
          </div>
        </div>
      </div>
    </AppLayout>
  );
};
