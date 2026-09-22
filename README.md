# Ventas y Distribución Android

MVP académico para el caso DSY1105. Proyecto autónomo basado en el caso Ventas y Distribución de DSY1105. Todos los usuarios, clientes, ubicaciones y operaciones son ficticios. La API de prueba representa el Dashboard descrito en el caso; no conecta con sistemas externos.

## Ejecutar

1. Inicia la API desde esta carpeta con Python 3: `python api/server.py`.
2. Abre esta carpeta en Android Studio y espera la sincronización Gradle. Requiere SDK Android 36 y JDK 17 o superior (se recomienda el JDK incluido en Android Studio).
3. Ejecuta `app` en un emulador Android API 26 o superior. La URL inicial `http://10.0.2.2:8000` conecta con la API del computador.
4. Ingresa con uno de los usuarios siguientes. Contraseña para todos: `Demo2026!`.

| Usuario | Permisos |
| --- | --- |
| administrador | Consultas, pedidos, entregas y gestión de roles/activación de usuarios de prueba |
| supervisor | Consulta de operación, ventas e indicadores |
| vendedor | Consulta y registro de pedidos |
| repartidor | Consulta de rutas y pedidos asignados, actualización de entregas |

Para teléfono conectado por USB: ejecuta `adb reverse tcp:8000 tcp:8000` y cambia la URL en el login a `http://127.0.0.1:8000`. La API escucha únicamente en localhost. No es necesario abrir puertos al exterior.

## Demostración

1. Vendedor: selecciona cliente y producto, cantidad y prioridad; registra un pedido. Los pedidos de este MVP se asignan a la ruta simulada Centro.
2. Repartidor: consulta Rutas; en Pedidos cambia Pendiente → En ruta → Entregado. Una incidencia exige una nota y permite volver a En ruta.
3. Supervisor: actualiza Resumen para ver las ventas entregadas, meta fija de $200.000 y pedidos por estado. Ventas muestra pedidos entregados, sin facturación ni pagos reales.
4. Administrador: cambia roles o desactiva usuarios desde Usuarios. La API valida los permisos en cada solicitud; no permite modificar el propio acceso.
5. Con la sesión abierta y datos cargados, detén la API y pulsa Actualizar: se conserva la última instantánea local y se muestra el aviso de desconexión. Los cambios quedan deshabilitados. Reinicia la API: sus sesiones son temporales, por lo que debes volver a iniciar sesión.

## Arquitectura y persistencia

- **Vista:** `MainActivity.kt`, pantallas Jetpack Compose / Material 3 con navegación por secciones.
- **ViewModel:** `OperationsViewModel.kt`, estado observable, carga, errores y operaciones en coroutines.
- **Repositorio:** `Repository`, consumo REST con HttpURLConnection en Dispatchers.IO y caché de instantánea JSON en SharedPreferences, separada por usuario. No almacena contraseñas ni tokens. Cerrar sesión borra la caché del usuario.
- **API:** `api/server.py`, servidor Python sin paquetes externos; persiste operaciones en `api/state.json` mediante reemplazo atómico. Sesiones aleatorias en memoria. Nunca usar estas credenciales ni el HTTP de desarrollo en producción.
- **Conectividad:** consulta local durante una sesión si falla la red; no hay login offline ni cola de escrituras. Ante una respuesta perdida, la app solicita actualizar antes de reintentar para evitar duplicados. Los cambios de entrega usan estado esperado y rechazan conflictos (409).

## API REST

Todas las rutas excepto login y health requieren `Authorization: Bearer <token de prueba>`.

| Método | Ruta | Función |
| --- | --- | --- |
| GET | /health | Comprobar disponibilidad |
| POST | /login | username y password → token y usuario |
| POST | /logout | Invalidar sesión |
| GET | /snapshot | Clientes, productos, pedidos, rutas, usuarios permitidos e indicadores |
| POST | /orders | clientId, productId, quantity (1–100), priority (Normal/Alta) |
| PATCH | /orders/{id} | status, expectedStatus y note; valida asignación y transición |
| PATCH | /users/{id} | role y active; exclusivo del administrador |

Los horarios, sucursal, vehículo y coordenadas referenciales se incluyen en las rutas; las entregas y su historial se representan en los pedidos. Cámara, GPS real y notificaciones quedan fuera del alcance opcional. No se crean usuarios nuevos desde la app: se administran los cuatro usuarios de prueba provistos.

## Verificación

- API: `python -m unittest discover -s api -v`.
- Android: `./gradlew assembleDebug lintDebug` (Windows: `gradlew.bat assembleDebug lintDebug`).
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

La suite de API cubre autenticación, roles, aislamiento por asignación, revocación de acceso, validación de cantidades, transiciones, conflictos, incidencias, ventas, historial y persistencia. El archivo de datos se crea al realizar el primer cambio; para reiniciar la demostración, detén la API y renombra `api/state.json` como respaldo.

Configuración Compose basada en la documentación oficial: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler

### Resultado comprobado (9 de septiembre de 2026)

- `assembleDebug`: APK generado e instalado en emulador Pixel 6.
- `lintDebug`: correcto, sin errores; quedan avisos de versiones de dependencias/SDK más recientes y sugerencias KTX.
- API: 6 pruebas aprobadas.
- Emulador: arranque, login de supervisor y consulta real a la API verificados; al detener la API y actualizar, se conserva la información y aparece el aviso de desconexión.
- Las capturas anteriores se retiraron porque correspondían a una identidad visual incorrecta.
- No se ha realizado una prueba manual completa de todos los formularios Android ni en un teléfono físico. Los flujos de escritura y autorización están cubiertos a nivel de API.
- El wrapper utiliza una distribución aislada en `.gradle/` del proyecto, con SHA-256 verificado, para evitar la distribución global incompleta detectada en este equipo.

### Guía 8: pantalla base (22 de septiembre de 2026)

La aplicación abre ahora una bienvenida independiente de la API, implementada en `ui/HomeScreen.kt` con Scaffold, TopAppBar, Image, Text, Button, Column y Row. “Ingresar a la operación” abre el acceso existente y Atrás vuelve a la bienvenida. “Conocer los perfiles” muestra un diálogo informativo. Los colores compartidos están en `ui/theme/Theme.kt` y las pantallas operativas en `ui/OperationsScreen.kt`.

Correspondencia con los puntos de la actividad y evidencia: [docs/guia-8.md](docs/guia-8.md).
