package dev.neolauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dev.neolauncher.ui.LauncherScreen

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // O tema (claro/escuro, cores, sombras) vem das configurações, aplicado dentro de LauncherScreen
        setContent {
            LauncherScreen(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    // Botão Home com o launcher já aberto
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.onHomePressed()
    }
}
