package jp.aoto.eiyoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.health.connect.client.PermissionController
import jp.aoto.eiyoapp.ui.EiyoApp
import jp.aoto.eiyoapp.ui.EiyoTheme

/** Entry point used when the user starts connecting this app from Health Connect. */
class HealthConnectOnboardingActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) {
        openApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestHealthPermissions()
    }

    private fun requestHealthPermissions() {
        val health = (application as EiyoApplication).health
        permissionLauncher.launch(health.permissions)
    }

    private fun openApp() {
        val app = application as EiyoApplication
        val viewModel = androidx.lifecycle.ViewModelProvider(
            this,
            MainViewModel.Factory(app),
        )[MainViewModel::class.java]
        setContent { EiyoTheme { EiyoApp(viewModel) } }
    }
}
