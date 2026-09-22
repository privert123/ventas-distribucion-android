package cl.ventasdistribucion.movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cl.ventasdistribucion.movil.ui.HomeScreen
import cl.ventasdistribucion.movil.ui.OperationsApp
import cl.ventasdistribucion.movil.ui.theme.VentasDistribucionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // La actividad solo conecta el tema y las pantallas; la lógica sigue en el ViewModel.
        setContent {
            VentasDistribucionTheme {
                var showOperations by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = showOperations) { showOperations = false }
                if (showOperations) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { OperationsApp() }
                } else {
                    HomeScreen(onEnter = { showOperations = true })
                }
            }
        }
    }
}

