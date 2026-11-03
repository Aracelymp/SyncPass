# SyncPass

Aplicación Android nativa y cliente web para la gestión de proyectos comunitarios, eventos, inscripciones, asistencia por QR, registros sin inscripción previa (walk-in), encuestas de satisfacción y reportes de impacto.

El repositorio contiene tres módulos:

- `app/`: aplicación Android (Kotlin, Jetpack Compose, Material 3, MVVM, Room, WorkManager, Retrofit).
- `backend/`: API REST (Spring Boot 3, Java 21, JPA, Spring Security, JWT, BCrypt).
- `src/`: aplicación web (React 19, TypeScript, Vite, React Router, AWS Amplify v6).

La arquitectura completa y actualizada se encuentra en `ARQUITECTURA_COMPLETA_SYNCPASS.md`.

## Funcionalidades implementadas

- Autenticación con AWS Cognito en Android y web.
- Intercambio del token Cognito por un JWT propio de SyncPass (`POST /api/auth/cognito/exchange`).
- Roles diferenciados:
  - `DOCENTE_COORDINADOR`: crea proyectos y eventos, consulta participantes, encuestas, comentarios y reportes.
  - `ESTUDIANTE_VOLUNTARIO_MONITOR`: consulta proyectos/eventos, se inscribe, escanea QR, registra walk-ins y aplica encuestas asistidas únicamente a las asistencias que él mismo registró o escaneó.
- Listado de proyectos y eventos con filtros y búsqueda.
- Detalle de proyecto con creación de eventos (coordinador).
- Detalle de evento con inscripción, QR del evento, escaneo de asistencia y resumen de encuesta para coordinación.
- Escáner QR con CameraX + ML Kit y captura manual del token como alternativa.
- Registro de participantes no inscritos (walk-in) por coordinadores y monitores, con almacenamiento local y sincronización posterior.
- Encuestas de satisfacción, organización e impacto. El monitor puede aplicarlas de forma asistida; el beneficiario no necesita cuenta.
- Privacidad de encuestas: solo coordinación recibe respuestas, comentarios y resultados. El monitor conserva únicamente el estado de sincronización y un recibo sin contenido.
- Reportes calculados desde el backend para coordinadores.
- Caché local Room, bandeja de salida (outbox) y sincronización automática con WorkManager al recuperar conexión, al iniciar sesión y cada 15 minutos.
- Migración Room aditiva de versión 1 a 2 para asistencias, encuestas y operaciones pendientes.

## Tecnologías principales

| Capa | Tecnología |
|---|---|
| Android | Kotlin, Jetpack Compose, Room, WorkManager, Retrofit, CameraX, ML Kit |
| Backend | Java 21, Spring Boot 3, Spring Security, JWT HS256, JPA/Hibernate |
| Web | React 19, TypeScript, Vite, React Router, AWS Amplify v6 |
| BD producción | PostgreSQL (Supabase) |
| BD desarrollo | H2 archivo |
| Auth | AWS Cognito User Pool |
| Frontend hosting | Vercel |
| Backend hosting | AWS EC2 |

## Requisitos

- JDK 21.
- Android SDK con plataforma 37 y un emulador o dispositivo API 28 o superior.
- Node.js 20+ para el frontend.
- No se necesita instalar Gradle: se incluyen los wrappers.

## Backend

El backend es un build Gradle independiente. Desde `backend/`:

```powershell
$env:JAVA_HOME="C:\Users\<usuario>\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
.\gradlew.bat bootRun
```

La API queda en `http://localhost:8080`. La salud se verifica con:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

En desarrollo usa H2 archivo en `backend/data/syncpass`. En producción se conecta a PostgreSQL en Supabase mediante variables de entorno.

Para construir el jar:

```powershell
.\gradlew.bat bootJar
```

## Aplicación Android

### URL de la API

La URL se elige en este orden:

1. `-PapiBaseUrl=...`
2. Variable `API_BASE_URL` del archivo `.env` en la raíz del proyecto Gradle.
3. Variable de entorno `API_BASE_URL`.
4. Valor por defecto `http://192.168.1.6:8080/`.

Para un emulador con el backend en la misma máquina:

```powershell
$env:JAVA_HOME="C:\Users\<usuario>\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
$env:SYNCPASS_BUILD_DIR="C:\Users\<usuario>\AppData\Local\Temp\opencode\syncpass-build"
.\gradlew.bat :app:assembleDebug "-PapiBaseUrl=http://10.0.2.2:8080/"
```

`SYNCPASS_BUILD_DIR` es opcional, pero recomendable si el proyecto está dentro de OneDrive: evita que los archivos generados se conviertan en marcadores en la nube y bloqueen Gradle.

Instalación en un emulador encendido:

```powershell
adb install -r "$env:SYNCPASS_BUILD_DIR\app\outputs\apk\debug\app-debug.apk"
adb shell am start -n com.rojas.syncpass/.MainActivity
```

## Aplicación Web

Desde la raíz del proyecto:

```powershell
npm install
npm run dev
```

El servidor de desarrollo queda en `http://localhost:5173`.

Build de producción:

```powershell
npm run build
```

La web de producción está desplegada en Vercel. Vercel proxies las llamadas `/api/*` al backend en EC2.

## Comportamiento sin conexión

- Las cuentas que ya iniciaron sesión pueden volver a entrar sin red usando la credencial local.
- Los datos previamente descargados siguen visibles desde Room.
- Las asistencias, walk-ins y encuestas se guardan en la outbox local.
- Al recuperar la conexión se sincronizan automáticamente.
- La creación de proyectos y eventos requiere conexión.

## Pruebas

Pruebas unitarias Android:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Pruebas de integración del backend:

```powershell
cd backend
.\gradlew.bat test
```

## Limitaciones conocidas

- No hay pruebas instrumentadas de UI (Compose) ni pruebas end-to-end automatizadas del APK.
- El escaneo con cámara se probó en el emulador; falta validarlo con un dispositivo físico y un QR impreso.
- La sincronización es incremental por operaciones; no incluye resolución de conflictos avanzada ni paginación para catálogos grandes.
- Las credenciales demo solo sirven para desarrollo local; la autenticación de producción usa AWS Cognito.
- La migración H2 → PostgreSQL está pausada; los datos de H2 y sus backups se conservan intactos.
