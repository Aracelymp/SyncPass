# SyncPass

Aplicación Android nativa para gestión de proyectos comunitarios, eventos, inscripciones, asistencia por QR, registros sin inscripción previa, encuestas de satisfacción y reportes de impacto.

El repositorio contiene dos módulos independientes:

- `app/`: aplicación Android (Kotlin, Jetpack Compose, Material 3, MVVM, Room, WorkManager, Retrofit).
- `backend/`: API REST real (Spring Boot 3, Java 21, JPA, H2, JWT, BCrypt).

## Funcionalidades implementadas

- Inicio de sesión en línea contra el backend y modo local sin conexión para cuentas ya usadas en el dispositivo.
- Roles diferenciados:
  - `DOCENTE_COORDINADOR`: crea proyectos y eventos, registra asistentes sin inscripción, consulta participantes, encuestas, comentarios y reportes.
  - `ESTUDIANTE_VOLUNTARIO_MONITOR`: consulta proyectos/eventos, se inscribe, cancela inscripción, escanea QR, registra walk-ins y aplica encuestas asistidas únicamente a las asistencias que él mismo registró o escaneó. No puede crear proyectos/eventos, consultar respuestas ni acceder a reportes.
- Listado de proyectos y eventos con filtros y búsqueda.
- Detalle de proyecto con creación de eventos.
- Detalle de evento con inscripción, QR del evento, escaneo de asistencia y resumen de encuesta para coordinación.
- Escáner QR con CameraX + ML Kit y captura manual del token como alternativa.
- Registro de participantes no inscritos (walk-in) por coordinadores y monitores, con almacenamiento local y sincronización posterior.
- Encuestas de satisfacción, organización e impacto. El monitor puede aplicarlas de forma asistida a las asistencias que registró o escaneó; el beneficiario no necesita cuenta.
- Privacidad de encuestas: solo coordinación recibe respuestas, comentarios y resultados. El monitor conserva únicamente el estado de sincronización y un recibo sin contenido.
- Reportes calculados localmente desde Room y pantalla exclusiva para coordinadores.
- Caché local Room, bandeja de salida (outbox) y sincronización automática con WorkManager al recuperar conexión, al iniciar sesión y cada 15 minutos.
- Migración Room aditiva de versión 1 a 2 para asistencias, encuestas y operaciones pendientes.
- Tema centralizado con la paleta institucional `#271539`, `#501F5A`, `#915BA4`, `#C2649B`, `#E783B5` y degradados permitidos.

## Cuentas de demostración

Todas usan la contraseña `Demo1234`.

| Usuario | Rol |
| --- | --- |
| `coordinador` | Docente · Coordinador |
| `monitor` | Estudiante · Monitor |
| `estudiante` | Estudiante · Monitor |
| `valentina` | Estudiante · Monitor |
| `camilo` | Estudiante · Monitor |

Los datos son ficticios y solo sirven para demostración.

## Requisitos

- JDK 21.
- Android SDK con plataforma 37 y un emulador o dispositivo API 28 o superior.
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

La base H2 se guarda en `backend/data/syncpass` y conserva los datos entre ejecuciones. Para construir el jar:

```powershell
.\gradlew.bat bootJar
```

## Aplicación Android

### URL de la API

La URL se elige en este orden:

1. `-PapiBaseUrl=...`
2. Variable `API_BASE_URL` del archivo `.env`.
3. Variable de entorno `API_BASE_URL`.
4. Valor por defecto `http://10.0.2.2:8080/`.

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

## Comportamiento sin conexión

- Las cuentas que ya iniciaron sesión pueden volver a entrar sin red usando la credencial local.
- Los datos previamente descargados siguen visibles desde Room.
- Las asistencias, walk-ins y encuestas se guardan en la outbox local.
- Al recuperar la conexión se sincronizan automáticamente.
- Una sesión iniciada sin conexión no puede sincronizar hasta volver a iniciar sesión con internet; esto evita reutilizar un token no validado. La pantalla de perfil lo indica.

## Pruebas

Pruebas unitarias Android:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Resultado verificado: 10 pruebas, 0 fallos (parseo de QR, token crudo, payload inválido, hash local, etiquetas de rol/estado y política de elegibilidad de encuestas asistidas por rol).

Pruebas de integración del backend:

```powershell
cd backend
.\gradlew.bat test
```

Resultado verificado: 8 pruebas, 0 fallos (salud pública, login y lectura autenticada, prohibición de creación para monitor, rechazo de token ausente/inválido y contraseña incorrecta, walk-in para monitor, encuesta asistida con control de duplicados y privacidad del promedio de satisfacción por rol).

Validación de migración Room (1 → 2): verificada con script manual contra `app/schemas` para las tablas `attendance`, `surveys` y `pending_operations` (columnas, índices y valores por defecto).

También se ejecutó una verificación manual contra la API real y el emulador: login de ambos roles, listados y detalles, creación de proyecto y evento desde la app, inscripción, asistencia QR, walk-in, encuesta, reportes, sincronización, modo sin conexión, login local y reconexión.

## Limitaciones conocidas y trabajo pendiente

- No hay pruebas instrumentadas de UI (Compose) ni pruebas end-to-end automatizadas del APK.
- El escaneo con cámara se probó en el emulador solo hasta el flujo de permiso y la captura manual; falta validarlo con un dispositivo físico y un QR impreso.
- El botón de compartir reportes abre el selector del sistema, pero no se verificó la vista previa en una app externa.
- La sincronización es incremental por operaciones; no incluye resolución de conflictos avanzada ni paginación para catálogos grandes.
- Las credenciales y el secreto JWT son de demostración. Antes de producción se deben externalizar, usar HTTPS, rotar secretos y firmar el APK con una clave de release.
- No se incluye despliegue del backend ni configuración de base de datos PostgreSQL de producción.
