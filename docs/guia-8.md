# Guía 8 · Pantalla base con Jetpack Compose

Adaptación al proyecto independiente **Ventas y Distribución** (DSY1105).

| Punto de la guía | Implementación |
| --- | --- |
| Archivo HomeScreen.kt dentro de ui/ | `app/src/main/java/cl/ventasdistribucion/movil/ui/HomeScreen.kt` |
| Scaffold y TopAppBar | Barra superior con nombre del proyecto y contenido con innerPadding |
| Text, Button, Image, Column y Row | Bienvenida, acciones, imagen de reparto y filas de funciones |
| Imagen en res/drawable | `ic_operaciones.xml`, vector local equivalente al recurso similar permitido |
| Modifier | Tamaños, ancho, padding, scroll y espacio seguro de Scaffold |
| Espaciado uniforme | Arrangement.spacedBy; márgenes de 24 dp y secciones separadas por 20 dp |
| MaterialTheme | Paleta compartida en `ui/theme/Theme.kt` y tipografía Material 3 |
| Comentarios explicativos | Secciones comentadas dentro de HomeScreen y MainActivity |
| Nuevos elementos visuales | Tarjeta de funciones y diálogo interactivo de perfiles |
| Integración en MainActivity | Bienvenida inicial, ingreso a operación y regreso mediante Atrás |
| Vista previa | Preview normal y Preview compacto con fuente ampliada |

La pantalla de bienvenida se visualiza sin conexión a la API. El botón “Ingresar a la operación” abre el flujo de acceso existente; la autenticación sí requiere la API de prueba.

## Organización

`MainActivity.kt` conecta la navegación y el tema. `ui/HomeScreen.kt` contiene la pantalla base reutilizable y recibe la acción como callback. `ui/OperationsScreen.kt` mantiene las pantallas operativas existentes. `OperationsViewModel.kt` conserva el estado, repositorio y comunicación con la API.

## Evidencia colaborativa pendiente

Se inicializó Git localmente en la rama main. La conexión y publicación en GitHub y la actualización de Trello siguen pendientes. Cuando se defina el repositorio y tablero del equipo, el mensaje de commit indicado por la guía es `Pantalla HomeScreen con estructura Scaffold`, y la rama sugerida, si corresponde a su flujo, es `feature/home-screen`. Las capturas verificadas se guardan en esta carpeta para poder adjuntarlas.

## Referencia oficial consultada

Scaffold y uso de innerPadding: https://developer.android.com/develop/ui/compose/components/scaffold

## Verificación realizada el 22 de septiembre de 2026

- `gradlew.bat assembleDebug lintDebug`: BUILD SUCCESSFUL; sin errores de lint.
- APK actualizado instalado y abierto en emulador Android API 37.
- Revisión visual de bienvenida: título, logo, tarjetas, ambos botones y pie visibles sin superposición.
- Diálogo de perfiles: abierto y cerrado correctamente.
- Ingreso a la operación: abre el login existente; Atrás regresa a HomeScreen.
- Captura: [home-guia8.png](home-guia8.png).
- Las dos funciones Preview están compiladas; su render dentro del editor de Android Studio no se verificó.

