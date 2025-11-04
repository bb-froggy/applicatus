package de.applicatus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import de.applicatus.app.ui.navigation.ApplicatusNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = (application as ApplicatusApplication).repository
        
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    ApplicatusNavHost(
                        navController = navController,
                        repository = repository
                    )
                }
            }
        }
    }
}
