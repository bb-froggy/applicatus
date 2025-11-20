package de.applicatus.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import de.applicatus.app.ui.navigation.ApplicatusNavHost
import de.applicatus.app.data.nearby.NearbyConnectionsService

class MainActivity : ComponentActivity() {
    // Hält die URI einer zu importierenden Datei, wenn die App über einen Intent geöffnet wurde
    private val pendingImportUri = mutableStateOf<Uri?>(null)
    
    // Nearby Connections Service (singleton per Activity)
    private lateinit var nearbyService: NearbyConnectionsService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = (application as ApplicatusApplication).repository
        nearbyService = NearbyConnectionsService(this)
        
        // Prüfe, ob die App zum Öffnen einer JSON-Datei gestartet wurde
        handleIncomingIntent(intent)
        
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    ApplicatusNavHost(
                        navController = navController,
                        repository = repository,
                        nearbyService = nearbyService,
                        pendingImportUri = pendingImportUri.value,
                        onImportHandled = { pendingImportUri.value = null }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)  // Wichtig: Intent aktualisieren für singleTop LaunchMode
        handleIncomingIntent(intent)
    }
    
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                // Prüfe ob es eine JSON-Datei ist (über MIME-Type oder Dateiendung)
                val isJson = intent.type == "application/json" || 
                             uri.toString().endsWith(".json", ignoreCase = true)
                
                if (isJson) {
                    pendingImportUri.value = uri
                    Toast.makeText(this, "JSON-Datei wird importiert...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
