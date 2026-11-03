# Historial completo de SyncPass

> Registro maestro del proyecto. Se actualiza al finalizar cada fase.
> No contiene contraseñas, tokens, secretos ni credenciales.

**Última actualización:** 2026-11-02  
**Estado general:** Fases 1-5 completadas. Integración AWS Cognito implementada y regularizada en producción para los tres perfiles de prueba. Configuración de producción documentada (Vercel, EC2, Supabase PostgreSQL). Documentación de arquitectura actualizada. Pendiente pruebas instrumentadas UI/E2E y validación manual con QR impreso.

---

## 1. Contexto original

SyncPass es una aplicación para gestionar proyectos comunitarios y sus actividades: creación de proyectos y eventos, inscripciones, asistencia mediante QR, registro de participantes no inscritos (walk-in), encuestas de satisfacción y reportes de impacto.

El caso de negocio definido en las conversaciones y en el README existente es:

- facilitar la operación de actividades comunitarias;
- permitir que coordinadores administren proyectos y eventos;
- permitir que estudiantes voluntarios/monitores registren asistencia y trabajen sin conexión;
- permitir que personas de la comunidad participen y respondan encuestas sin necesariamente tener una cuenta;
- conservar la información y sincronizarla cuando vuelva la conexión;
- producir métricas y reportes para coordinación.

### Perfiles definitivos

| Perfil | Autenticación | Representación |
| --- | --- | --- |
| Docente / Coordinador | Sí | `DOCENTE_COORDINADOR` |
| Estudiante voluntario / Monitor | Sí | `ESTUDIANTE_VOLUNTARIO_MONITOR` |
| Beneficiario comunitario / Asistente | No | Fila de asistencia del evento |

El beneficiario **no** es un tercer rol de autenticación. No tiene usuario, contraseña, JWT ni grupo de Cognito. Cuando corresponde, se identifica mediante una asistencia creada por un monitor o por el flujo de walk-in.

---

## 2. Tecnologías y arquitectura

### Android

- Kotlin.
- Jetpack Compose y Material 3.
- MVVM.
- Room para caché local, asistencias, encuestas y cola de operaciones pendientes.
- WorkManager para sincronización.
- Retrofit, OkHttp y Gson.
- CameraX y ML Kit para escaneo QR.
- DataStore para la sesión local.
- Un solo módulo `app/`.

### Backend

- Java 21.
- Spring Boot 3.5.
- Spring Web, Spring Data JPA, Spring Security, Validation.
- H2 en modo PostgreSQL, con archivo local por defecto.
- JWT propio y BCrypt.
- Módulo Gradle independiente en `backend/`.

### Estructura

```text
SyncPass/
  app/                 aplicación Android
  backend/             API REST Spring Boot
  gradle/              wrapper y catálogo de versiones
  README.md            documentación original
  HISTORIAL_COMPLETO_SYNCPASS.md
```

### Autenticación actual

- `POST /api/auth/login` valida usuario/contraseña contra BCrypt.
- El backend emite un JWT propio.
- Android guarda la sesión y permite un login local posterior usando un hash derivado de la contraseña, sin conexión.
- Cognito todavía no está integrado.

### Sincronización actual

- Las asistencias, walk-ins y encuestas se guardan primero en Room.
- Se encolan en `pending_operations`.
- El lote se envía a `POST /api/sync/batch`.
- `GET /api/sync/pull` descarga catálogos y datos del usuario.
- WorkManager sincroniza al recuperar conexión, al iniciar sesión y periódicamente.

---

## 3. Estado inicial verificado

Antes de la implementación de la Fase 1 se verificó:

- Backend con 5 pruebas de integración aprobadas.
- Android con 5 pruebas unitarias aprobadas según el README y la verificación previa del proyecto.
- Roles existentes:
  - `DOCENTE_COORDINADOR`, etiqueta “Docente · Coordinador”.
  - `ESTUDIANTE_VOLUNTARIO_MONITOR`, etiqueta “Estudiante · Monitor”.
- El backend autoriza con el rol cargado desde la base de datos, no con el claim del JWT.
- No existía endpoint para crear usuarios ni para cambiar roles.
- El walk-in estaba restringido a coordinadores en el controlador, el servicio y la sincronización.
- Las encuestas se asociaban al usuario autenticado.
- `SurveyResponse.user` ya era nullable, pero no existía relación con la asistencia ni con el monitor recopilador.
- El resumen de encuestas estaba disponible para cualquier usuario autenticado.
- El listado individual de encuestas ya estaba restringido a coordinadores.
- El QR existente identifica un evento, no a una persona.
- No existía asignación de eventos a monitores.
- La pantalla de evento mostraba resultados de encuestas a cualquier rol.
- La pantalla de encuesta siempre se presentaba como encuesta propia.

---

## 4. Decisiones y cambios solicitados

### Decisiones que prevalecen

- Mantener exactamente dos roles autenticados.
- No crear un rol `BENEFICIARIO_ASISTENTE` en backend, JWT ni Cognito.
- Permitir que el monitor registre walk-ins.
- El monitor solo puede encuestar asistencias que él registró o escaneó.
- Una sola encuesta por asistencia.
- No usar correo ni documento como criterio principal de unicidad.
- Solo el coordinador puede consultar comentarios, respuestas individuales, resultados y reportes.
- El monitor puede consultar todos los eventos disponibles; no hay asignaciones individuales en esta fase.
- No implementar QR individual de beneficiario en la primera fase.
- Mantener el QR de evento.
- Mantener el flujo de encuesta propia para usuarios autenticados.
- Mantener acceso offline después de un login online previo.
- No crear cuentas Cognito para beneficiarios.
- No habilitar registro público.
- Mantener cuentas demo durante la transición.
- No crear recursos AWS en esta etapa.
- No usar migraciones destructivas ni borrar bases de datos.
- Las respuestas locales solo se redactan después de confirmar la sincronización.

### Decisiones anteriores reemplazadas

| Decisión anterior | Decisión vigente | Motivo |
| --- | --- | --- |
| Añadir `BENEFICIARIO_ASISTENTE` como tercer rol | No añadirlo | El beneficiario no inicia sesión; es un participante |
| Asignar grupo Cognito al beneficiario | No crear grupo ni cuenta | No requiere autenticación |
| Usar QR individual para identificar beneficiarios | Diferirlo; usar asistencias y walk-in | No existe soporte actual y agrandaría el alcance |
| Usar correo/documento para unicidad | Usar asistencia | Evita bloquear personas sin correo/documento |
| Permitir que el monitor consulte resultados | Solo coordinador | Privacidad de comentarios y respuestas |
| Encuestas vinculadas al usuario autenticado | Encuestas asistidas vinculadas a la asistencia | Evita atribuir la respuesta al monitor |
| Mostrar resultados locales a cualquier rol | Ocultarlos al monitor y restringir backend | Ocultar en UI no sustituye autorización |

---

## 5. Roles y permisos

### Coordinador

- Inicia y cierra sesión.
- Crea y administra proyectos y eventos.
- Consulta participantes, inscripciones y asistencias.
- Registra walk-ins.
- Consulta encuestas, comentarios, resultados y reportes.
- Supervisa sincronización y errores.

### Monitor

- Inicia y cierra sesión.
- Consulta todos los eventos disponibles.
- Escanea el QR del evento y registra su propia asistencia.
- Registra participantes no inscritos.
- Consulta las asistencias que él registró para seleccionar a quién encuestar.
- Registra encuestas asistidas para esas asistencias.
- Trabaja offline y sincroniza después.
- No crea proyectos/eventos.
- No consulta reportes.
- No consulta comentarios, respuestas individuales ni resultados de encuestas.

### Beneficiario

- No inicia sesión.
- No tiene cuenta ni contraseña.
- Puede ser registrado como walk-in por un monitor.
- Puede responder una encuesta en el dispositivo del monitor.
- Su encuesta queda ligada a su asistencia y al evento.
- No accede a funciones administrativas.

---

## 6. Encuestas

### Flujo propio

- Se conserva para usuarios autenticados.
- Se asocia al usuario y al evento.
- La regla actual sigue siendo una encuesta por evento y usuario.
- Se añadió `collectedBy` con el mismo usuario para poder emitir recibos de sincronización.

### Flujo asistido

1. El monitor registra un walk-in o escanea una asistencia propia.
2. En el detalle del evento selecciona “Registrar encuesta”.
3. Elige una asistencia que él mismo registró.
4. La pantalla muestra el nombre del participante.
5. El beneficiario responde sin iniciar sesión.
6. La encuesta se guarda localmente y se encola.
7. Al sincronizar, el backend la vincula a la asistencia y al evento.
8. `user` queda nulo si el participante no tiene cuenta.
9. `collectedBy` queda con el monitor que operó el dispositivo.
10. El monitor no recibe el contenido de la encuesta sincronizada.

### Duplicados

- Una sola encuesta por asistencia.
- Índice único en `attendance_id`.
- `client_id` único para reintentos idempotentes.
- Si la asistencia tiene usuario y ya existe una encuesta de ese usuario para el evento, se rechaza la segunda.
- No se usa correo ni documento para decidir duplicados.
- El backend devuelve `DUPLICATE` con el identificador de la encuesta existente.

### Privacidad

- `GET /api/events/{id}/surveys`: solo coordinador.
- `GET /api/events/{id}/survey/summary`: solo coordinador desde la Fase 1.
- `GET /api/reports/*`: solo coordinador.
- `EventDetailDto.surveySummary`: nulo para monitor.
- `EventDto.averageRating`: nulo/oculto al monitor en detalle y tarjetas.
- `GET /api/sync/pull`: el monitor recibe `surveyReceipts`, no encuestas completas.
- El monitor no recibe calificaciones, comentarios ni identidad del respondiente.
- En Android el monitor solo ve estado de sincronización para sus encuestas; el contenido se redacta tras confirmación.
- El coordinador conserva resultados, comentarios y reportes.

---

## 7. QR y asistencias

- Se mantiene el QR de evento existente.
- El escaneo QR registra la asistencia del usuario autenticado que escanea.
- No hay QR individual de beneficiario en esta fase.
- Los beneficiarios sin cuenta se incorporan mediante walk-in.
- El monitor puede registrar walk-ins desde la aplicación y desde la sincronización.
- Cada asistencia tiene `clientId` estable, generado localmente cuando corresponde.
- El monitor solo puede encuestar asistencias cuyo `registeredBy` sea él mismo.
- El coordinador puede encuestar cualquier asistencia existente.

Consecuencia documentada: como no hay QR individual, la encuesta asistida se aplicará principalmente a walk-ins registrados por el monitor. Si en el futuro se necesita encuestar a beneficiarios que se registraron por su cuenta, hará falta un QR de participante o un cambio de flujo.

---

## 8. Funcionamiento offline

- La sesión local se mantiene después de un login online previo.
- Las asistencias y encuestas se guardan en Room.
- La cola de operaciones pendientes se ordena por `createdAt` y, en la implementación Android pendiente, por `id` como desempate.
- La encuesta referencia la asistencia por `attendanceClientId` y, cuando está disponible, por `attendanceServerId`.
- Si la asistencia no está sincronizada, el backend devuelve un error reintentable; no se descarta la encuesta.
- Los reintentos usan el mismo `clientId` y son idempotentes.
- Las respuestas locales de una encuesta asistida se redactarán solo después de confirmar `SYNCED` o `DUPLICATE` del servidor.
- Los datos del servidor no se modifican al redactar la copia local.
- No se almacenan secretos de AWS en Android.
- Limitación: mientras una encuesta está pendiente, su contenido existe en la base local. La protección contra un dispositivo comprometido requeriría cifrado adicional, fuera de esta fase.
- Limitación: el acceso offline no valida MFA ni cambios de rol en tiempo real.

---

## 9. Migraciones y cambios técnicos

### Fase 1: backend implementado

Entidades:

- `SurveyResponse`:
  - nueva relación `attendance_id`;
  - nueva relación `collected_by`;
  - índice único en `attendance_id`;
  - índice no único en `collected_by`.

DTOs:

- `AttendanceDto`: nuevo campo `registeredByUserId`.
- `SurveyDto`: nuevos campos `attendanceId`, `attendanceClientId`, `collectedByUserId`, `collectedByName`.
- `SurveyReceiptDto`: nuevo contrato mínimo sin contenido de encuesta.
- `SyncOperationDto`: nuevos campos `attendanceClientId` y `attendanceServerId`.
- `SyncPullDto`: nueva lista `surveyReceipts`.

Repositorios:

- `AttendanceRepository`: consultas por monitor registrador y consulta combinada de asistencias propias/registradas.
- `SurveyRepository`: búsqueda por asistencia y consulta de encuestas propias o recopiladas.

Servicios y controladores:

- `EventService.detail`: resumen de encuesta solo para coordinador.
- `EventService.walkIn`: permitido a coordinador y monitor.
- `EventService.submitSurvey`: asigna `collectedBy` al usuario.
- `SyncService.processWalkIn`: permitido a coordinador y monitor.
- `SyncService.processSurvey`: valida asistencia, propiedad del monitor, evento y duplicados; crea la asociación.
- `SyncService.pull`: coordinador recibe encuestas completas; monitor recibe recibos.
- `EventController`: walk-in para ambos roles; resumen de encuesta solo coordinador.
- `DtoMapper`: mapea nuevos campos y nombre del participante desde la asistencia.

Base de datos:

- `spring.jpa.hibernate.ddl-auto=update`.
- La migración es aditiva; no se borra la base.
- Las columnas nuevas son nullables para conservar registros existentes.
- `attendance_id` es único, pero admite múltiples nulos para encuestas propias sin asistencia.

### Compatibilidad

- Se mantiene `POST /api/events/{id}/survey`.
- Si una operación de sincronización no trae referencias de asistencia, se procesa como encuesta propia antigua.
- Se conservan los endpoints y flujos existentes de proyectos, eventos, inscripciones, QR, reportes y sincronización.

---

## 10. Pruebas y validaciones

### Antes de la Fase 1

Comando:

```powershell
cd backend
$env:JAVA_HOME="C:\Users\milla\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat test --console=plain
```

Resultado real: `BUILD SUCCESSFUL`; 5 pruebas existentes aprobadas.

### Después de la Fase 1

El mismo comando se ejecutó después de los cambios.

Resultado real: `BUILD SUCCESSFUL`; 8 pruebas de integración aprobadas.

Pruebas nuevas:

1. Un monitor registra un walk-in y recibe `registeredByUserId`.
2. Un monitor sincroniza una encuesta anónima para esa asistencia.
3. La encuesta guarda `user_id = null`, `attendance_id`, `collected_by = monitor` y el nombre del participante.
4. El monitor recibe `403` al consultar el resumen de encuestas.
5. El monitor recibe `403` al consultar el listado individual.
6. El coordinador recibe el resumen y los comentarios.
7. El detalle del evento devuelve `surveySummary = null` para el monitor.
8. El `pull` del monitor devuelve `surveyReceipts` sin calificaciones ni comentarios.
9. Un segundo intento para la misma asistencia devuelve `DUPLICATE`.
10. Un monitor no puede encuestar una asistencia registrada por el coordinador.
11. La encuesta propia antigua sigue sincronizándose y se asocia al usuario.

### Errores encontrados durante la Fase 1

- Error de compilación en una aserción de prueba por usar `List<?>` con `containsExactly`.
- Solución: tipar la lista como `List<String>` en la prueba.
- No hubo fallos de lógica detectados en la ejecución final.

### Pendiente de probar

- Android y migración Room.
- Flujo de UI asistido.
- Sincronización offline real con Room.
- Redacción local después de sincronizar.
- Cognito.

---

## 11. Cognito y AWS

### Estrategia definida

Cognito autenticará únicamente a coordinadores y monitores.

Flujo previsto:

1. Android autentica contra Cognito.
2. Android envía el token de Cognito al backend.
3. El backend valida firma, issuer, cliente, expiración y grupos.
4. El backend emite el JWT actual de SyncPass.
5. La aplicación conserva el contrato actual.

### Reglas

- Grupos previstos: `SYNCPASS_COORDINADOR` y `SYNCPASS_MONITOR`.
- No se crea grupo ni cuenta para beneficiarios.
- No se habilita registro público.
- Los roles se asignan desde administración, no desde el cliente.
- Se conservan las cuentas demo.
- Se mantiene el login offline actual.
- No se almacenan secretos AWS en Android.

### Configuración manual futura

- User Pool.
- App Client público sin secreto.
- Política de contraseñas.
- Verificación de correo.
- MFA según decisión.
- Duración de tokens.
- Grupos.
- Usuarios y asignaciones.
- Variables de entorno del backend.
- CloudWatch.
- IAM solo para administración de usuarios/grupos.

### Estado real (actualizado 2026-11-02)

**AWS Cognito está configurado y en uso en producción.** Se crearon/recuperaron los recursos necesarios: User Pool, App Clients web y Android, dominio Hosted UI. Se completó la regularización de vínculos Cognito ↔ usuarios backend para los tres perfiles de prueba. La autenticación web y Android utiliza Cognito; el backend emite el JWT propio tras validar el token y la vinculación en `user_cognito_links`.

---

## 12. Incidencias y riesgos

| Riesgo | Estado | Mitigación |
| --- | --- | --- |
| El monitor obtiene permiso de walk-in | Implementado y probado en backend | Validación de rol y registro de `registeredBy` |
| El monitor encuesta asistencias ajenas | Mitigado en backend | Validación de `registeredBy`; prueba incluida |
| Respuestas visibles al monitor | Mitigado en backend | Endpoints restringidos y pull con recibos |
| Duplicados de encuesta | Mitigado en backend | Una por asistencia; `clientId` idempotente |
| Encuesta llega antes que la asistencia | Pendiente en Android | Ordenar outbox y reintentar; `attendanceServerId` |
| Respuestas pendientes en el dispositivo | Pendiente mitigación local | Redactar solo después de sync confirmada |
| Migración Room destructiva | Pendiente | Migración aditiva y pruebas |
| QR individual no existe | Limitación aceptada | Usar walk-in y asistencias; documentado |
| Cognito offline/MFA | Pendiente de diseño final | Mantener sesión prevalidada; documentar límites |
| Tokens locales sin cifrado | Riesgo conocido | Evaluar endurecimiento posterior |

---

## 13. Historial de implementación por fases

### Fase 1 — Backend, modelo de encuestas y permisos

**Estado:** completada y probada.

**Objetivo:** vincular encuestas a asistencias, permitir walk-in al monitor, impedir encuestas ajenas y ocultar resultados al monitor.

**Cambios realizados:**

- `SurveyResponse` con `attendance` y `collectedBy`.
- Índice único de asistencia.
- DTOs de encuesta y sincronización ampliados.
- Recibo mínimo de encuesta para monitores.
- Repositorios con consultas por asistencia y monitor.
- Walk-in permitido a monitores.
- Validación de propiedad de la asistencia.
- Duplicados por asistencia.
- Resumen solo para coordinador.
- Pull sin contenido de encuestas para monitores.
- Pruebas de integración nuevas.

**Archivos modificados:**

- `backend/src/main/java/com/rojas/syncpass/backend/domain/SurveyResponse.java`
- `backend/src/main/java/com/rojas/syncpass/backend/dto/ApiDtos.java`
- `backend/src/main/java/com/rojas/syncpass/backend/repo/AttendanceRepository.java`
- `backend/src/main/java/com/rojas/syncpass/backend/repo/SurveyRepository.java`
- `backend/src/main/java/com/rojas/syncpass/backend/service/DtoMapper.java`
- `backend/src/main/java/com/rojas/syncpass/backend/service/EventService.java`
- `backend/src/main/java/com/rojas/syncpass/backend/service/SyncService.java`
- `backend/src/main/java/com/rojas/syncpass/backend/web/EventController.java`
- `backend/src/test/java/com/rojas/syncpass/backend/SyncPassApiIntegrationTest.java`
- `HISTORIAL_COMPLETO_SYNCPASS.md`

**Comandos ejecutados:**

```powershell
cd C:\Users\milla\Documents\SyncPass\backend
$env:JAVA_HOME="C:\Users\milla\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat test --console=plain
```

**Resultado:** `BUILD SUCCESSFUL`; 8 pruebas aprobadas.

### Fase 2 — Room y migración no destructiva

**Estado:** completada.

**Objetivo:** añadir campos locales para asistencia registrada por monitor, encuesta asistida, recibos, redacción y orden de sincronización.

**Cambios realizados:**
- `AttendanceEntity` con `registeredByUserId`.
- `SurveyEntity` con `attendanceClientId`, `attendanceServerId`, `collectedByUserId`, `redacted`.
- `PendingOperationEntity` con referencias de asistencia.
- `AppDatabase` versión 2 con `MIGRATION_1_2` aditiva.
- Esquema exportado `app/schemas/.../2.json`.
- DAOs ampliados para elegibilidad, recibos, orden de outbox y consultas.

**Archivos modificados:**
- `app/src/main/java/com/rojas/syncpass/data/local/Entities.kt`
- `app/src/main/java/com/rojas/syncpass/data/local/Daos.kt`
- `app/src/main/java/com/rojas/syncpass/data/local/AppDatabase.kt`
- `app/src/main/java/com/rojas/syncpass/data/local/Mappers.kt`
- `app/schemas/.../2.json`
- `app/src/main/java/com/rojas/syncpass/ui/events/EventsScreen.kt`
- `app/src/main/java/com/rojas/syncpass/ui/events/EventsViewModel.kt`

### Fase 3 — Pantallas Android

**Estado:** completada.

**Objetivo:** walk-in para monitor, selector de asistencias, encuesta asistida y ocultamiento de resultados.

**Cambios realizados:**
- `EventDetailScreen`: selector de asistencias elegibles, tarjeta de encuestas asistidas, botón de encuesta propia con `attendanceClientId`, ocultamiento de calificaciones/resultados al monitor, walk-in visible a ambos roles, promedio oculto en cabecera/tarjetas.
- `SurveyScreen`: soporte de modo asistido con nombre del participante y ocultamiento de respuestas existentes.
- `AppRoot`: navegación con parámetro `attendanceClientId` opcional.
- `EventDetailViewModel`: lista `eligibleAttendance` con regla de elegibilidad y exclusión de encuestas existentes.
- `WalkInRepository/AttendanceRepository`: permite registrar walk-in al monitor con `registeredByUserId`.

### Fase 4 — Sincronización offline

**Estado:** completada.

**Objetivo:** ordenar operaciones, resolver duplicados, reintentar y redactar tras sincronización confirmada.

**Cambios realizados:**
- `SyncRepository`: priorización asistencia→encuesta, procesamiento de `surveyReceipts`, redacción tras `SYNCED`/`DUPLICATE`, no almacena encuestas completas en monitor, actualización de promedio con valores nulos.
- `SurveyRepository`: envío asistido, validación de duplicados, referencia de asistencia en outbox.
- `AttendanceSurveyPolicy`: regla pura de elegibilidad.
- DTOs remotos ampliados para asistido/recibos.

### Fase 5 — Pruebas y documentación

**Estado:** parcial (validación completada).

**Objetivo:** pruebas Android, migración, privacidad, offline y actualización del README.

**Cambios realizados:**
- Pruebas unitarias Android: política de elegibilidad y roles (5 añadidas). Total 10/10.
- Validación manual de migración Room 1→2 contra `app/schemas/.../2.json`.
- Actualización de `README.md` (roles, walk-in/encuestas asistidas, privacidad, pruebas).
- Actualización de `HISTORIAL_COMPLETO_SYNCPASS.md`.
- Verificación completa: `EventDetailScreen` adaptado a encuestas asistidas con selector por rol; restricciones de privacidad backend y Android validadas; sincronización offline con orden asistencia→encuesta, idempotencia y redacción tras confirmación.
- **Mejora:** Creación de eventos desde la pantalla global `Eventos`. Se añade botón `+` solo para coordinador en `EventsScreen`, selector de proyecto con lista desde `ProjectsViewModel`, reutilización del formulario `EventFormDialog` (validación y llamada vía `EventRepository.create`), actualización de lista global al guardar. Se amplía `EventsViewModel` con `projectRepository` y `createEvent`. Se conserva la creación desde `ProjectDetailScreen`.

**Pendiente:** pruebas instrumentadas de UI/end-to-end y validación manual con QR impreso (creación global desde Eventos).

### Fase 6 — Cognito

**Estado:** pendiente y sin recursos AWS.

**Plan:** integrar Amazon Cognito al final mediante intercambio: Cognito autentica y el backend emite el JWT actual (mantener arquitectura, sin cambiar contratos REST). No se implementa en esta fase.

---

## 14. Estado actual

### Terminado

- Auditoría y plan definitivo.
- Fase 1 (backend): modelo asistido, permisos, privacidad y recibos. 8 pruebas integradas.
- Fase 2 (Room): entidades, DAOs, migración 1→2 y esquema. Migración aditiva validada contra `app/schemas/.../2.json`.
- Fase 3 (UI Android): walk-in para monitor, selector de asistencias, encuesta asistida, ocultamiento de resultados. `EventDetailScreen` adaptado con selector por rol.
- Fase 4 (Sync): orden asistencia→encuesta, redacción tras confirmación, recibos, privacidad de promedio.
- Fase 5 parcial: 10 pruebas unitarias Android, validación de migración, actualización README e historial.
- Verificación completa (última): `EventDetailScreen` y flujo asistido correctos; compilación Android completa (`:app:assembleDebug`) y pruebas OK (`:app:testDebugUnitTest` 10/10); backend `backend:test` 8/8; migración Room 1→2 no destructiva validada; privacidad backend/Android validada; sincronización offline con orden, idempotencia y conservación de respuestas pendientes hasta confirmación `SYNCED`/`DUPLICATE`. Sin errores bloqueantes. Cognito pospuesto, sin recursos AWS creados.
- Creación de eventos desde `Eventos` global: botón `+` solo coordinador, selector de proyecto, reutilización de `EventFormDialog`, guardado vía `EventRepository.create`. Conflada lista global tras creación. Flujo desde detalle de proyecto conservado.

### En progreso

- Ninguno.

### Pendiente

- Pruebas instrumentadas de UI / end-to-end (no ejecutadas).
- Validación manual con QR impreso (no automatizada).
- Integración Amazon Cognito (Fase 6, pospuesta; sin recursos AWS creados).
- README.
- Cognito.

### Errores conocidos

- Ninguno en backend tras la Fase 1.
- La aplicación Android todavía no conoce los nuevos campos; se actualizará en la Fase 2/3.
- El backend devuelve `surveySummary = null` para monitores; Android debe aceptar el nulo antes de usar esa respuesta.

### Próximo paso exacto

Implementar la Fase 2:

1. añadir campos a `AttendanceEntity`, `SurveyEntity` y `PendingOperationEntity`;
2. actualizar `Daos.kt`, `Mappers.kt` y `AppDatabase.kt`;
3. crear una migración Room no destructiva;
4. actualizar los DTOs Kotlin;
5. compilar y probar la migración antes de tocar las pantallas.

---

## 15. Instrucciones para retomar

Antes de continuar:

1. Leer este historial completo.
2. Confirmar que la Fase 1 está aprobada y no volver a añadir un rol de beneficiario.
3. No eliminar ni renombrar `DOCENTE_COORDINADOR` ni `ESTUDIANTE_VOLUNTARIO_MONITOR`.
4. No crear cuentas ni grupos Cognito para beneficiarios.
5. No implementar QR individual en esta fase.
6. No usar correo ni documento como unicidad principal.
7. No permitir que el monitor consulte encuestas, comentarios o reportes.
8. No redactar respuestas locales antes de recibir `SYNCED` o `DUPLICATE` del servidor.
9. No usar migraciones destructivas ni borrar la base Room.
10. No ejecutar comandos que creen recursos AWS.
11. Mantener el flujo de encuesta propia.
12. Actualizar este archivo al terminar cada fase.

### Comandos de verificación

Backend:

```powershell
cd C:\Users\milla\Documents\SyncPass\backend
$env:JAVA_HOME="C:\Users\milla\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat test --console=plain
```

Android:

```powershell
cd C:\Users\milla\Documents\SyncPass
$env:JAVA_HOME="C:\Users\milla\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

### Lagunas de información

- No se dispone de un historial de commits porque el proyecto no está confirmado como repositorio Git en esta sesión.
- Las fechas exactas de conversaciones anteriores no están confirmadas; se registra la fecha de esta actualización.
- No se han ejecutado pruebas instrumentadas de UI.

---

## 16. Fase de regularización Cognito, configuración de producción y actualización documental (2026-11-02)

### Objetivo

Poner en producción la autenticación con AWS Cognito, regularizar los vínculos de los tres perfiles de prueba, documentar la arquitectura real del sistema y dejar el proyecto preparado para mantenimiento futuro.

### Cambios realizados

- **Regularización Cognito:**
  - Se identificaron los `cognito_sub` de los tres perfiles de prueba.
  - Se crearon los usuarios backend faltantes mediante el endpoint oficial `POST /api/admin/users`.
  - Se reasignaron los vínculos `user_cognito_links` para que cada `sub` apunte al `user_id` y rol correctos.
  - Se registraron las solicitudes de vínculo correspondientes en `cognito_link_requests` con estado `APPROVED`.
  - Se preservaron los usuarios demo y sus datos históricos.

- **Configuración de producción Android:**
  - Se creó `.env` en la raíz del proyecto Gradle con `API_BASE_URL`, `COGNITO_CLIENT_ID` y `COGNITO_USER_POOL_DOMAIN`.
  - `.gitignore` ya protegía `.env`.
  - Se verificó que el App Client Android es público y permite `USER_PASSWORD_AUTH`.
  - Se compiló exitosamente `:app:assembleDebug` con los valores de producción.

- **Diagnóstico y validación:**
  - Se verificó conectividad con EC2, Vercel y Supabase PostgreSQL.
  - Se instaló `postgresql-client-17` en EC2 para coincidir con la versión del servidor.
  - Se generó respaldo persistente de `app_users`, `user_cognito_links` y `cognito_link_requests`.
  - Se ejecutó la regularización en una transacción con validaciones previas y posteriores; se confirmó con `COMMIT`.
  - Se probó login web y Android con los tres perfiles.
  - Se validó flujo de encuesta offline: registro local, sincronización, persistencia en `survey_responses` e inclusión en reportes.
  - Se diagnosticó que la web ya dispone de consulta individual de respuestas en `/surveys`; la causa de respuestas no visibles fue la selección inicial de evento.

- **Documentación:**
  - Se actualizó integralmente `ARQUITECTURA_COMPLETA_SYNCPASS.md` con el estado real de arquitectura, tecnologías, autenticación, base de datos, sincronización, infraestructura, pruebas y diagramas.
  - Se actualizó `README.md` para reflejar Cognito, PostgreSQL en producción, web en Vercel, proxy a EC2 y las funcionalidades actuales.
  - Se consolidó `backend/ARQUITECTURA_COMPLETA_SYNCPASS.md` como referencia al documento raíz.
  - Se actualizó este `HISTORIAL_COMPLETO_SYNCPASS.md` con la nueva fase.

### Estado resultante

- Autenticación Cognito operativa en web y Android.
- Backend en EC2 conectado a Supabase PostgreSQL.
- Web en Vercel con proxy `/api/*` a EC2.
- Tres perfiles regularizados con roles correctos.
- Documentación de arquitectura alineada con el código.

### Pendiente

- Pruebas instrumentadas de UI / end-to-end.
- Validación con QR impreso en dispositivo físico.
- Migración H2 → PostgreSQL (pausada; datos H2 intactos).
- Resolver UX de selección inicial de evento en `/surveys`.
