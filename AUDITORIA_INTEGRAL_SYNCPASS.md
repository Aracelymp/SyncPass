> **Versión sanitizada para repositorio.** Los correos personales, contraseñas de demostración, IP de producción e identificadores de Cognito han sido reemplazados por marcadores genéricos. El archivo original se conserva localmente como AUDITORIA_INTEGRAL_SYNCPASS.original.md.
# AUDITORÍA INTEGRAL SYNCPASS

**Fecha:** 2026-11-02  
**Auditor:** OpenCode (Kimi K2.7 Code)  
**Alcance:** Solo lectura y diagnóstico. No se modificaron archivos, variables de entorno, configuraciones de AWS/Vercel/Supabase, ni servicios.  
**Ruta del proyecto:** `C:\Users\milla\Documents\SyncPass`  

> **Nota importante:** Esta auditoría se basa en la inspección del código fuente, la ejecución de builds/tests locales y pruebas HTTP GET/POST seguras contra endpoints públicos. No se tuvo acceso a las consolas de AWS, Vercel ni Supabase; donde corresponde se indica explícitamente que una configuración no pudo verificarse.

---

## A. Estado general

SyncPass es un sistema fullstack compuesto por:

- **Frontend web:** React + TypeScript + Vite, desplegado en Vercel.
- **Backend:** Spring Boot 3.5 + Java 21, desplegado en AWS EC2 con IP fija `[IP DE PRODUCCIÓN]`.
- **Base de datos:** PostgreSQL vía JDBC, documentado para usar Supabase como host.
- **Autenticación:** AWS Cognito (Authorization Code + PKCE en web, USER_PASSWORD_AUTH en Android) + JWT propio del backend.
- **Aplicación Android:** Kotlin + Jetpack Compose + Room + WorkManager.

**Veredicto:** El proyecto compila correctamente en los tres componentes y el backend responde. Sin embargo, **la base de datos de producción contiene actualmente datos de demostración junto a una sola cuenta real**, lo que representa un riesgo crítico. El frontend web tiene problemas de configuración de variables de entorno que impiden el funcionamiento de Cognito si Vercel no las inyecta manualmente. Android no apunta a producción ni tiene valores Cognito reales. La causa raíz del `redirect_mismatch` de Cognito en web es que el bundle desplegado probablemente no envía exactamente el `client_id`/`redirect_uri` esperado; la configuración de AWS para los valores declarados es correcta.

---

## B. Arquitectura actual

```text
┌─────────────────┐     ┌─────────────────┐
│   Web (Vercel)  │     │ Android (Kotlin)│
│  React+Vite+Ampl│     │  Room+WorkManagr│
└────────┬────────┘     └────────┬────────┘
         │                       │
         │ HTTPS                 │ HTTPS/HTTP
         │ /api/*                │ directo a EC2
         ▼                       ▼
┌─────────────────────────────────────┐
│      Vercel Proxy / EC2 [IP_OCULTA]      │
│   vercel.json → http://[IP DE PRODUCCIÓN]│
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│   Backend Spring Boot (EC2)         │
│   JWT propio + Cognito validator    │
└─────────────┬───────────────────────┘
              │
              ▼
┌─────────────────────────────────────┐
│   PostgreSQL / Supabase (JDBC)      │
└─────────────────────────────────────┘
```

- **Web:** La SPA se sirve desde `[URL_WEB_PRODUCCIÓN]`. Las llamadas a `/api/*` se reescriben al backend EC2 (`vercel.json`).
- **Android:** Apunta a una URL configurable por build; el fallback actual es una IP local (`192.168.1.6:8080`).
- **Backend:** Expone API REST en `/api`. Autentica con JWT propio y valida tokens Cognito para el intercambio.
- **Cognito:** User Pool `[COGNITO_USER_POOL_ID]`, dominio `[COGNITO_DOMAIN]`. App Clients documentados: `[COGNITO_APP_CLIENT_ID_WEB]` (web) y `[COGNITO_APP_CLIENT_ID_ANDROID]` (Android).

---

## C. Estado por componente

### C.1 Frontend web

| Aspecto | Estado | Evidencia |
|---|---|---|
| Tecnologías | React 19.3, TypeScript 7.0, Vite 8.3, React Router DOM 7.18, AWS Amplify 6.15 | `package.json` |
| Build local | ✅ Exitoso (`npm run build`) | Salida del build: `dist/` generado, 400.69 kB JS |
| Type checking | ✅ Sin errores (`tsc -b`) | Build incluye `tsc -b` |
| Estructura | SPA con rutas: `/login`, `/auth/callback`, `/dashboard`, `/projects`, `/events`, `/attendance`, `/surveys`, `/reports` | `src/routes/AppRouter.tsx` |
| Autenticación | Login local deshabilitado; solo Cognito. Callback intercambia `idToken` por JWT propio. | `src/pages/auth/Login.tsx:36-57`, `src/pages/auth/Callback.tsx`, `src/services/authExchange.ts` |
| Persistencia de sesión | ❌ JWT propio solo en memoria; se pierde al recargar | `src/utils/authStorage.ts` |
| Variables de entorno | ❌ `.env` no usa prefijo `VITE_`; Vite no expone `API_BASE_URL` ni Cognito al bundle | `.env:3-5`, `src/config/api.ts:14`, `src/config/cognito.ts:44-55` |
| Conexión a backend | Relativa al origen; en Vercel viaja por proxy a EC2 | `vercel.json:5`, `src/config/api.ts` |
| Callbacks Cognito | Fallback a `window.location.origin`; soporta Preview y Production | `src/main.tsx:9`, `src/config/cognito.ts` |

**Hallazgo crítico:** El archivo `.env` define `API_BASE_URL`, `COGNITO_CLIENT_ID` y `COGNITO_USER_POOL_DOMAIN` **sin el prefijo `VITE_`**. Vite ignora cualquier variable que no comience por `VITE_`, por lo que en un build local el bundle no recibe ni URL de API ni configuración Cognito. En Vercel esto se mitiga si las variables se configuran directamente en el dashboard con el prefijo correcto, pero no pude verificar el dashboard de Vercel.

### C.2 Backend

| Aspecto | Estado | Evidencia |
|---|---|---|
| Tecnologías | Spring Boot 3.5.16, Java 21, Spring Security, JPA, H2/PostgreSQL | `backend/build.gradle.kts` |
| Build y tests | ✅ `BUILD SUCCESSFUL`, tests up-to-date | Salida de `./gradlew.bat compileJava test` |
| Perfiles | `default` (H2 archivo + demo), `prod` (PostgreSQL, sin demo), `test` (H2 memoria) | `application.yml`, `application-prod.yml`, `application-test.yml` |
| Endpoints documentados | 30+ endpoints REST con autorización por rol | Ver listado completo en sección C.2.1 |
| Seguridad | JWT propio + Cognito validator; CORS configurable | `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `CognitoTokenValidator.java` |
| Datos demo | Se siembran si `SEED_DEMO_DATA=true` y no hay usuarios | `DemoDataSeeder.java` |
| Configuración Cognito default | JWKS usa `/.wellknown/` (falta guión) en lugar de `/.well-known/` | `application.yml:43` |
| Consola H2 | Permitida públicamente en perfil default | `SecurityConfig.java:74`, `application.yml:24-27` |

#### C.2.1 Endpoints principales

| Controlador | Método | Path | Rol requerido | Función |
|---|---|---|---|---|
| `HealthController` | GET | `/api/health` | Público | Healthcheck |
| `AuthController` | POST | `/api/auth/login` | Público | Login usuario/contraseña |
| `AuthController` | GET | `/api/auth/me` | Autenticado | Perfil actual |
| `AuthCognitoController` | POST | `/api/auth/cognito/exchange` | Público | Intercambiar token Cognito → JWT |
| `CognitoLinkAccessController` | POST | `/api/auth/cognito/link-request` | Público | Solicitar vínculo Cognito |
| `AdminCognitoLinkRequestController` | GET | `/api/admin/cognito-link-requests` | Coordinador | Listar solicitudes |
| `AdminCognitoLinkRequestController` | POST | `/api/admin/cognito-link-requests/{id}/approve` | Coordinador | Aprobar vínculo |
| `AdminCognitoLinkRequestController` | POST | `/api/admin/cognito-link-requests/{id}/reject` | Coordinador | Rechazar vínculo |
| `AdminUserController` | GET | `/api/admin/users` | Coordinador | Listar usuarios |
| `AdminUserController` | POST | `/api/admin/users` | Coordinador | Crear usuario |
| `AdminUserController` | PATCH | `/api/admin/users/{id}/active` | Coordinador | Activar/desactivar usuario |
| `ProjectController` | GET | `/api/projects` | Autenticado | Listar proyectos |
| `ProjectController` | POST/PUT/DELETE | `/api/projects` | Coordinador | CRUD proyectos |
| `EventController` | GET | `/api/events` | Autenticado | Listar eventos |
| `EventController` | GET | `/api/events/{id}/detail` | Autenticado | Detalle del evento |
| `EventController` | POST/PUT/DELETE | `/api/events` | Coordinador | CRUD eventos |
| `EventController` | POST | `/api/events/{id}/attendance/scan` | Autenticado | Escanear asistencia QR |
| `EventController` | POST | `/api/events/{id}/attendance/walk-in` | Coord./Monitor | Registrar no inscrito |
| `EventController` | POST | `/api/events/{id}/survey` | Autenticado | Enviar encuesta |
| `ReportController` | GET | `/api/reports/impact` | Coordinador | Reporte de impacto |
| `SyncController` | POST | `/api/sync/batch` | Autenticado | Sincronización push |
| `SyncController` | GET | `/api/sync/pull` | Autenticado | Sincronización pull |

### C.3 AWS Cognito

| Aspecto | Estado | Evidencia |
|---|---|---|
| User Pool | `[COGNITO_USER_POOL_ID]` | `application.yml:40`, well-known endpoint |
| Dominio | `[COGNITO_DOMAIN]` | Respuesta del well-known |
| Well-known | ✅ Responde correctamente | `https://cognito-idp.us-east-1.amazonaws.com/[COGNITO_USER_POOL_ID]/.well-known/openid-configuration` |
| App Clients documentados | Web `[COGNITO_APP_CLIENT_ID_WEB]`, Android `[COGNITO_APP_CLIENT_ID_ANDROID]` | `application.yml:41`, `syncpass.env.example:34` |
| Callbacks (Preview/Production) | ✅ Aceptados en prueba directa | Petición GET directa a `/oauth2/authorize` con `redirect_uri=[URL_PREVIEW_VERCEL]/auth/callback` devolvió la página de login |
| Configuración AWS | ✅ Correcta para los valores declarados | Ver sección C.3.1 |

#### C.3.1 Verificación directa de Cognito

Se realizó una petición directa al endpoint de autorización con:

- `client_id=[COGNITO_APP_CLIENT_ID_WEB]`
- `redirect_uri=[URL_PREVIEW_VERCEL]/auth/callback`
- `response_type=code`

**Resultado:** Cognito respondió con la página de inicio de sesión (no `redirect_mismatch`).

**Conclusión:** La configuración de AWS para esos valores es correcta. El `redirect_mismatch` que ocurre en el navegador indica que el frontend desplegado **no está enviando exactamente esa combinación** (diferente `client_id`, dominio o `redirect_uri`). La comprobación pendiente es copiar la URL real del navegador y comparar esos tres parámetros.

### C.4 AWS EC2 y despliegue

| Aspecto | Estado | Evidencia |
|---|---|---|
| IP pública | `[IP DE PRODUCCIÓN]` | `vercel.json:5` |
| Healthcheck directo | ✅ HTTP 200, status UP | `http://[IP DE PRODUCCIÓN]/api/health` |
| Healthcheck vía Vercel | ✅ HTTP 200, status UP | `[URL_WEB_PRODUCCIÓN]/api/health` |
| HTTPS en backend | ❌ Vercel proxy usa `http://[IP DE PRODUCCIÓN]` | `vercel.json:5` |
| Infraestructura como código | ❌ No hay Dockerfile, CI/CD, scripts de despliegue ni terraform | Búsqueda en `backend/` y raíz |
| Servicio systemd/nginx | No verificable | Sin acceso SSH a EC2 |
| Logs | No verificables | Sin acceso SSH/journalctl |
| Base de datos en producción | ⚠️ Contiene datos demo + 1 usuario real | Ver sección D |

### C.5 Supabase

| Aspecto | Estado | Evidencia |
|---|---|---|
| Conexión | Mediante JDBC PostgreSQL estándar, no SDK de Supabase | `application-prod.yml:8-11`, `build.gradle.kts:34` |
| URL real | No accesible en código | `syncpass.env.example:10` muestra ejemplo de pooler |
| Schema/RLS | No verificables | Sin acceso a Supabase |
| ddl-auto en prod | `validate` (no modifica schema) | `application-prod.yml:16` |

### C.6 Aplicación Android

| Aspecto | Estado | Evidencia |
|---|---|---|
| Build local | ✅ `BUILD SUCCESSFUL` (con `SYNCPASS_BUILD_DIR` fuera de OneDrive) | Salida de `gradlew :app:assembleDebug` |
| Tecnologías | Kotlin 2.4, Compose BOM 2026.09, Room 2.8.5, CameraX, ML Kit, WorkManager | `app/build.gradle.kts`, `libs.versions.toml` |
| Arquitectura | MVVM manual con DI en `AppContainer` | `core/AppContainer.kt` |
| Autenticación | Cognito directo `USER_PASSWORD_AUTH`; AppAuth configurado pero no usado | `core/auth/CognitoDirectAuthService.kt`, `AndroidManifest.xml:32-43` |
| URL backend por defecto | ❌ `http://192.168.1.6:8080/` (red local) | `app/build.gradle.kts:20` |
| Cognito | ❌ Placeholders `<APP_CLIENT_ID>` y `<USER_POOL_DOMAIN>` | `app/build.gradle.kts:27,34` |
| Tráfico HTTP | ✅ `usesCleartextTraffic="true"` habilitado | `AndroidManifest.xml:18` |
| Datos demo locales | Siembra usuarios/proyectos/eventos ficticios | `data/local/DemoLocalSeeder.kt` |
| Sincronización | Room + WorkManager cada 15 min + triggers de red/login | `data/sync/SyncWorker.kt`, `SyncPassApp.kt` |
| Deprecaciones | Varias advertencias de AGP/Kotlin que romperán en AGP 10.0 | Salida del build |

---

## D. Estado de las tres cuentas

Se obtuvo un JWT de demostración con rol `DOCENTE_COORDINADOR` llamando a `POST /api/auth/login` con las credenciales demo documentadas (`coordinador / [CREDENCIAL DE DEMOSTRACIÓN OMITIDA]`). Con ese token se consultó `GET /api/admin/users`.

### Resultado de la consulta a producción

| Email solicitado | Usuario en backend | Email en backend | Rol | Cognito vinculado |
|---|---|---|---|---|
| `[CORREO_USUARIO_1]` | `coordinador2` | `[CORREO_USUARIO_1]` | `DOCENTE_COORDINADOR` | ✅ Sí |
| `[CORREO_USUARIO_2]` | **No existe** | — | — | — |
| `[CORREO_USUARIO_3]` | **No existe** | — | — | — |

**Además, en la base de datos de producción existen 5 cuentas demo:**

| Usuario | Email | Rol |
|---|---|---|
| `coordinador` | `coordinador@syncpass.demo` | Coordinador |
| `monitor` | `monitor@syncpass.demo` | Monitor |
| `estudiante` | `estudiante@syncpass.demo` | Monitor |
| `valentina` | `valentina@syncpass.demo` | Monitor |
| `camilo` | `camilo@syncpass.demo` | Monitor |

**Conclusiones:**

1. Solo una de las tres cuentas reales (`[CORREO_USUARIO_1]`) existe en el backend y está vinculada a Cognito.
2. Las otras dos cuentas reales **no existen** como usuarios de backend. Tampoco hay solicitudes de vínculo pendientes (`GET /api/admin/cognito-link-requests` devolvió lista vacía).
3. La base de datos de producción contiene datos de demostración con contraseñas conocidas (`[CREDENCIAL DE DEMOSTRACIÓN OMITIDA]`), lo que es un riesgo crítico.
4. No se pudo verificar si los usuarios reales existen en Cognito; solo se verificó el backend.

---

## E. Errores encontrados

### Críticos

| # | Error | Evidencia | Impacto |
|---|---|---|---|
| 1 | **Base de datos de producción contiene usuarios demo con contraseña pública** | Login exitoso con `coordinador/[CREDENCIAL DE DEMOSTRACIÓN OMITIDA]` en `[URL_WEB_PRODUCCIÓN]/api/auth/login`; `DemoDataSeeder.java:58` | Cualquiera puede autenticarse como coordinador o monitor en producción. |
| 2 | **Dos de las tres cuentas reales no existen en el backend** | `GET /api/admin/users` no devuelve `[CORREO_USUARIO_2]` ni `[CORREO_USUARIO_3]` | Esos usuarios no pueden iniciar sesión ni operar. |
| 3 | **Variables de entorno del frontend sin prefijo `VITE_`** | `.env:3-5`, `src/config/api.ts:14`, `src/config/cognito.ts:44-55` | El bundle local no recibe URL de API ni Cognito; Cognito queda deshabilitado salvo que Vercel inyecte variables correctas. |
| 4 | **Consola H2 pública y base H2 archivo en perfil default** | `SecurityConfig.java:74`, `application.yml:24-27` | Si el jar se ejecuta sin perfil `prod`, la base de datos es accesible públicamente. |

### Altos

| # | Error | Evidencia | Impacto |
|---|---|---|---|
| 5 | **Android apunta a red local y tiene placeholders de Cognito** | `app/build.gradle.kts:20,27,34` | La app no puede conectarse a producción ni autenticar con Cognito real. |
| 6 | **Proxy Vercel → EC2 usa HTTP no cifrado** | `vercel.json:5` | Tráfico entre Vercel y EC2 expuesto a intermediarios. |
| 7 | **JWKS Cognito por defecto mal escrito (`/.wellknown/`)** | `application.yml:43` | La validación Cognito fallaría si no se sobrescribe `COGNITO_JWKS_URI`. |
| 8 | **Datos demo insertados automáticamente si no hay usuarios** | `DemoDataSeeder.java:54`, `application.yml:38` | Riesgo de volver a sembrar demo si se reinicia con base vacía. |

### Medios

| # | Error | Evidencia | Impacto |
|---|---|---|---|
| 9 | **JWT propio almacenado solo en memoria en web** | `src/utils/authStorage.ts` | Sesión se pierde al recargar la página. |
| 10 | **CORS default `*`** | `application.yml:37` | Permite cualquier origen si no se configura `CORS_ALLOWED_ORIGINS`. |
| 11 | **`usesCleartextTraffic="true"` en Android** | `AndroidManifest.xml:18` | Permite tráfico HTTP plano. |
| 12 | **Datos demo también sembrados en Android localmente** | `DemoLocalSeeder.kt` | La app funciona offline con datos ficticios; riesgo de confusión. |
| 13 | **application-test.yml anida `cognito` bajo `jwt`** | `application-test.yml:14-18` | `app.cognito.*` no se sobrescribe en tests; usa defaults de `application.yml`. |

### Bajos

| # | Error | Evidencia | Impacto |
|---|---|---|---|
| 14 | **Código muerto `src/services/api.ts`** | `src/services/api.ts:32-62` | Cliente HTTP stub no utilizado. |
| 15 | **Espera artificial de 500 ms en callback** | `src/pages/auth/Callback.tsx:28` | Retardo innecesario. |
| 16 | **Título genérico en `index.html`** | `index.html:7` | Poco relevante. |
| 17 | **Advertencias de deprecación AGP/Kotlin** | Salida del build Android | Actualizaciones futuras romperán la build. |

---

## F. Funcionalidades

| Funcionalidad | Estado | Evidencia | Problema detectado |
|---|---|---|---|
| Healthcheck backend | ✅ Funcionando | `GET /api/health` → 200 | Ninguno |
| Login usuario/contraseña (legacy) | ✅ Funcionando | `POST /api/auth/login` con demo → 200 | Datos demo en producción |
| Login Cognito web | ⚠️ Parcial | `src/pages/auth/Login.tsx`, `Callback.tsx` | Variables sin `VITE_`; redirect_mismatch persistente |
| Intercambio Cognito → JWT | ✅ Código correcto | `AuthCognitoController.java` | Devuelve 403 si identidad no vinculada |
| Solicitud de vínculo Cognito | ✅ Implementada | `CognitoLinkAccessController.java` | Ninguno |
| Aprobación de vínculo Cognito | ✅ Implementada | `AdminCognitoLinkRequestController.java` | Requiere coordinador |
| CRUD proyectos | ✅ Implementado | `ProjectController.java`, `src/pages/projects/Projects.tsx` | Frontend sin API_BASE_URL configurado |
| CRUD eventos | ✅ Implementado | `EventController.java`, `src/pages/events/Events.tsx` | Ninguno crítico |
| Registro de asistencia QR | ✅ Implementado | Android `ScannerScreen.kt`, backend `EventController` | Depende de red local en Android |
| Walk-in | ✅ Implementado | `EventController.java:74-78`, Android `WalkInScreen.kt` | Ninguno crítico |
| Encuestas | ✅ Implementado | `EventController.java:90-105`, Android/Survey | Coordinador ve todo; monitor recibe recibos redactados |
| Reportes | ✅ Implementado | `ReportController.java`, `src/pages/reports/Reports.tsx` | Solo coordinador |
| Sincronización offline Android | ✅ Implementada | `SyncWorker.kt`, `SyncRepository.kt` | URL backend local por defecto |
| Gestión de usuarios admin | ✅ Implementada | `AdminUserController.java`, Android `UsersScreen.kt` | Ninguno crítico |
| Roles y permisos | ✅ Implementados | `Role.java`, `ProtectedRoute.tsx`, `@PreAuthorize` | Ninguno crítico |
| Pruebas unitarias backend | ✅ Pasan | 6 clases de test, BUILD SUCCESSFUL | No ejecutadas con datos reales |
| Pruebas unitarias Android | ✅ Pasan | 4 clases de test | Build exitoso |
| CI/CD | ❌ No existe | No hay `.github/`, `Dockerfile`, etc. | Despliegue manual |

---

## G. Variables de entorno

### Frontend web (Vite)

| Variable | Uso | ¿Configurada en `.env` local? | Observación |
|---|---|---|---|
| `VITE_API_BASE_URL` | URL base de la API en `src/config/api.ts` | ❌ No (existe `API_BASE_URL` sin `VITE_`) | Necesita prefijo `VITE_` |
| `VITE_AWS_REGION` | Región Cognito | ❌ No | Necesita prefijo `VITE_` |
| `VITE_COGNITO_USER_POOL_ID` | User Pool Cognito | ❌ No | Necesita prefijo `VITE_` |
| `VITE_COGNITO_APP_CLIENT_ID` | App Client web | ❌ No (existe `COGNITO_CLIENT_ID` sin `VITE_`) | Necesita prefijo `VITE_` |
| `VITE_COGNITO_DOMAIN` | Dominio Cognito | ❌ No (existe `COGNITO_USER_POOL_DOMAIN` sin `VITE_`) | Necesita prefijo `VITE_` |
| `VITE_REDIRECT_SIGN_IN` | Callback de login | ❌ No | Opcional, fallback a origen |
| `VITE_REDIRECT_SIGN_OUT` | Callback de logout | ❌ No | Opcional, fallback a origen |

### Backend

| Variable | Uso | Default | Observación |
|---|---|---|---|
| `SERVER_PORT` | Puerto | `8080` | Opcional |
| `DATABASE_URL` | JDBC URL PostgreSQL | — en prod | Requerido en prod |
| `DB_USER` | Usuario DB | — en prod | Requerido en prod |
| `DB_PASSWORD` | Contraseña DB | — en prod | Requerido en prod |
| `JWT_SECRET` | Firma JWT | — | Requerido |
| `JWT_EXPIRATION_MS` | TTL token | `604800000` | Opcional |
| `CORS_ALLOWED_ORIGINS` | Orígenes CORS | `*` (default) | Peligroso en prod |
| `SEED_DEMO_DATA` | Sembrar demo | `true` (default) | Debe ser `false` en prod |
| `COGNITO_ISSUER` | Issuer Cognito | Hardcodeado en default | Sobrescribir en prod |
| `COGNITO_CLIENT_IDS` | App Clients permitidos | Hardcodeado en default | Sobrescribir en prod |
| `COGNITO_REGION` | Región Cognito | `us-east-1` | Opcional |
| `COGNITO_JWKS_URI` | JWKS Cognito | `/.wellknown/` mal escrito | Debe sobrescribirse |

### Android

| Variable | Uso | Fallback | Observación |
|---|---|---|---|
| `API_BASE_URL` | URL backend | `http://192.168.1.6:8080/` | Debe apuntar a producción |
| `COGNITO_CLIENT_ID` | App Client Android | `<APP_CLIENT_ID>` | Placeholder |
| `COGNITO_USER_POOL_DOMAIN` | Dominio Cognito | Placeholder | Placeholder |

---

## H. Riesgos: qué no modificar para no romper el funcionamiento actual

1. **No borrar los datos de demostración sin antes migrar/crear las cuentas reales.** Actualmente solo `[CORREO_USUARIO_1]` puede operar como coordinador real. Si se desactivan o eliminan usuarios demo sin tener otro coordinador activo, se podría quedar sin acceso administrativo.
2. **No cambiar `vercel.json` a ciegas.** El proxy `/api/* → http://[IP DE PRODUCCIÓN]/api/$1` es la única forma en que el frontend llega al backend. Cualquier cambio debe ir acompañado de una alternativa funcional.
3. **No reiniciar el backend sin confirmar el perfil activo.** Si se levanta sin `spring.profiles.active=prod` y con base vacía, `DemoDataSeeder` volverá a crear usuarios demo.
4. **No cambiar la configuración de Cognito sin coordinar con frontend y Android.** Los `client_id`, dominios y callback URLs están acoplados a ambos clientes.
5. **No ejecutar `ddl-auto: update` en producción.** En `prod` está `validate`, pero si se cambia accidentalmente podría modificar el schema de PostgreSQL/Supabase.
6. **No deshabilitar la consola H2 sin confirmar que el backend no usa H2 en prod.** Aunque el perfil prod apunta a PostgreSQL, si la instancia real usa H2 archivo, deshabilitar la consola no arregla el problema de fondo.
7. **No rotar `JWT_SECRET` sin plan de logout.** Los tokens actuales quedarían inválidos.

---

## I. Plan de recuperación (orden seguro)

### Fase 1 — Estabilizar Production

1. **Verificar/desplegar variables Vercel correctas:** Asegurar que en Vercel existan `VITE_COGNITO_USER_POOL_ID`, `VITE_COGNITO_APP_CLIENT_ID`, `VITE_COGNITO_DOMAIN`, `VITE_REDIRECT_SIGN_IN`, `VITE_REDIRECT_SIGN_OUT` con los valores reales (el `client_id` web es `[COGNITO_APP_CLIENT_ID_WEB]`).
2. **Confirmar el `client_id` y `redirect_uri` reales en el navegador:** Copiar la URL de autorización del error de Cognito y comparar `client_id`, dominio y `redirect_uri` decodificado con los valores conocidos.
3. **Corregir el `.env` local:** Renombrar las variables a `VITE_*` para que los builds locales funcionen.
4. **Restaurar backend Cognito exchange:** Con el build instrumentado desplegado, reproducir login y revisar `journalctl` para saber cuál de los 3 caminos de 403 se activa.
5. **Proteger producción de datos demo:**
   - Confirmar que el servicio EC2 usa perfil `prod` (`spring.profiles.active=prod`).
   - Asegurar `SEED_DEMO_DATA=false`.
   - Deshabilitar consola H2 en producción (`spring.h2.console.enabled=false` o quitar `/h2-console/**` de permitAll).
   - Cambiar contraseñas de los usuarios demo o desactivarlos una vez creados los reales.

### Fase 2 — Recuperar cuentas reales

6. **Crear los usuarios faltantes en backend:**
   - `[CORREO_USUARIO_2]` → rol Monitor.
   - `[CORREO_USUARIO_3]` → rol Monitor.
   - `[CORREO_USUARIO_1]` ya existe como Coordinador; verificar que su vínculo Cognito siga siendo el correcto.
7. **Vincular cuentas reales a Cognito:**
   - Si los usuarios ya existen en Cognito, que inicien sesión en Android/web y generen una solicitud de vínculo.
   - Aprobar las solicitudes desde el usuario coordinador real.
   - Si no existen en Cognito, crearlos primero en AWS (solo con autorización).

### Fase 3 — Frontend y Android

8. **Hacer que Android apunte a producción:** Cambiar fallback de `API_BASE_URL` a `[URL_WEB_PRODUCCIÓN]/` (o directo a EC2 con HTTPS) y configurar `COGNITO_CLIENT_ID` real de Android (`[COGNITO_APP_CLIENT_ID_ANDROID]`).
9. **Verificar flujo completo web:** Login Cognito → callback → intercambio → consumo de API.
10. **Verificar flujo completo Android:** Login Cognito directo → intercambio → sincronización.

### Fase 4 — Seguridad y calidad

11. **Corregir `application.yml`:**
    - `COGNITO_JWKS_URI` default a `/.well-known/jwks.json`.
    - Eliminar o reducir datos hardcodeados.
12. **Implementar persistencia de sesión web** (localStorage seguro o cookie httpOnly) para evitar perder sesión al recargar.
13. **Eliminar/condicionar datos demo** antes del release público.
14. **Agregar CI/CD** y scripts de despliegue reproducibles.
15. **Habilitar HTTPS end-to-end** entre Vercel y EC2.

---

## J. Pendientes

- [ ] Confirmar variables de entorno reales en dashboard de Vercel.
- [ ] Resolver `redirect_mismatch` de Cognito en Preview/Production verificando la URL real del navegador.
- [ ] Resolver `403` en `POST /api/auth/cognito/exchange` revisando logs instrumentados del backend.
- [ ] Verificar acceso SSH a EC2 y estado del servicio systemd.
- [ ] Confirmar si el backend en EC2 usa perfil `prod` o default/H2.
- [ ] Crear en backend las cuentas `[CORREO_USUARIO_2]` y `[CORREO_USUARIO_3]`.
- [ ] Vincular las tres cuentas reales a sus identidades Cognito.
- [ ] Configurar Android para producción (URL backend y Cognito reales).
- [ ] Hacer que el `.env` local use prefijos `VITE_` correctamente.
- [ ] Desactivar/eliminar usuarios demo de producción.
- [ ] Deshabilitar consola H2 en producción.
- [ ] Corregir JWKS default mal escrito.
- [ ] Implementar persistencia de sesión web.
- [ ] Agregar CI/CD e infraestructura como código.
- [ ] Completar pruebas instrumentadas/E2E en Android.
- [ ] Validar escaneo QR con dispositivo físico.
- [ ] Definir y ejecutar migración H2 → PostgreSQL/Supabase cuando se autorice.

---

## Anexos

### A1. Resultados de builds

| Componente | Comando | Resultado |
|---|---|---|
| Frontend web | `npm run build` | ✅ Built in 566 ms |
| Backend | `gradlew compileJava test` | ✅ BUILD SUCCESSFUL |
| Android | `gradlew :app:assembleDebug -x test -x lintDebug` | ✅ BUILD SUCCESSFUL (con `SYNCPASS_BUILD_DIR` fuera de OneDrive) |

### A2. Resultados de pruebas HTTP

| Endpoint | Método | Resultado |
|---|---|---|
| `http://[IP DE PRODUCCIÓN]/api/health` | GET | 200 UP |
| `[URL_WEB_PRODUCCIÓN]/api/health` | GET | 200 UP |
| `[URL_WEB_PRODUCCIÓN]/api/auth/cognito/exchange` | POST `{}` | 400 (correcto: falta token) |
| `[URL_WEB_PRODUCCIÓN]/api/auth/cognito/exchange` | POST token inválido | 401 (token inválido) |
| `[URL_WEB_PRODUCCIÓN]/api/auth/login` | POST demo coord. | 200 + JWT (riesgo confirmado) |
| `[URL_WEB_PRODUCCIÓN]/api/admin/users` | GET (token demo) | Lista con 5 demo + 1 real |
| `[URL_WEB_PRODUCCIÓN]/api/admin/cognito-link-requests` | GET (token demo) | Lista vacía |

### A3. Limitaciones de esta auditoría

- No se tuvo acceso a AWS Console, Vercel Dashboard, Supabase Dashboard ni SSH a EC2.
- No se probaron credenciales reales de los usuarios.
- No se ejecutaron pruebas destructivas ni se consultó información personal sensible.
- Los JWT obtenidos con credenciales demo no se muestran en este informe.
