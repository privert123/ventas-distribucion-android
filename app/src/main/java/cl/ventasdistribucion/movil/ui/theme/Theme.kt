package cl.ventasdistribucion.movil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta centralizada: las pantallas consumen colores semánticos de MaterialTheme.
private val OperationsColors = lightColorScheme(
    primary = Color(0xFF2156B6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5EDFF),
    onPrimaryContainer = Color(0xFF163568),
    secondary = Color(0xFF25685D),
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EDF5),
    onSurface = Color(0xFF172338),
    onSurfaceVariant = Color(0xFF506078)
)

@Composable
fun VentasDistribucionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OperationsColors, content = content)
}
