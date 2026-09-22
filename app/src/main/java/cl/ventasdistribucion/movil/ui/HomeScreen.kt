package cl.ventasdistribucion.movil.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.ventasdistribucion.movil.R
import cl.ventasdistribucion.movil.ui.theme.VentasDistribucionTheme

/** Pantalla base de la Guía 8. No depende de la API ni crea un ViewModel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onEnter: () -> Unit, modifier: Modifier = Modifier) {
    var showHelp by rememberSaveable { mutableStateOf(false) }

    // Scaffold organiza la barra superior y entrega el espacio seguro para el contenido.
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Ventas y Distribución", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        // Column y Modifier controlan márgenes, ancho y separación. El scroll admite pantallas pequeñas.
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .consumeWindowInsets(innerPadding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagen vectorial local equivalente al logo.png solicitado; funciona sin internet.
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Image(
                    painter = painterResource(R.drawable.ic_operaciones),
                    contentDescription = "Camión de reparto, símbolo de Ventas y Distribución",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.padding(20.dp).size(80.dp)
                )
            }

            // Jerarquía visual: categoría, título y texto de apoyo con estilos del tema.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GESTIÓN COMERCIAL Y LOGÍSTICA", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("Tu operación,\ndonde estés", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Consulta pedidos, acompaña las entregas y mantén a tu equipo informado desde terreno.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Row alinea horizontalmente el número y la descripción de cada función reutilizable.
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FeatureRow("01", "Ventas y pedidos", "Clientes, productos y seguimiento comercial.")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    FeatureRow("02", "Rutas y entregas", "Distribución, estados e historial de cada pedido.")
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    FeatureRow("03", "Indicadores", "Una vista clara del avance de la operación.")
                }
            }

            // La acción se recibe como callback: la pantalla puede reutilizarse y previsualizarse.
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEnter, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Ingresar a la operación")
                }
                OutlinedButton(onClick = { showHelp = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Conocer los perfiles")
                }
            }
            Text("Demostración académica · Datos ficticios", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // Elemento adicional de la guía: diálogo informativo con interacción local y accesible.
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Un acceso para cada función") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Administrador: gestiona roles y usuarios de prueba.")
                Text("Supervisor: consulta indicadores y seguimiento.")
                Text("Vendedor: consulta clientes y registra pedidos.")
                Text("Repartidor: revisa rutas y actualiza entregas asignadas.")
            } },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun FeatureRow(number: String, title: String, description: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Text(number, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Android Studio puede mostrar la pantalla sin servidor, credenciales ni emulador.
@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    VentasDistribucionTheme { HomeScreen(onEnter = {}) }
}

@Preview(name = "Pantalla pequeña y texto grande", showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun HomeScreenCompactPreview() {
    VentasDistribucionTheme { HomeScreen(onEnter = {}) }
}
