package jp.aoto.eiyoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import jp.aoto.eiyoapp.ui.EiyoApp
import jp.aoto.eiyoapp.ui.EiyoTheme
import jp.aoto.eiyoapp.ui.LineButton

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as EiyoApplication
        viewModel = ViewModelProvider(this, MainViewModel.Factory(app))[MainViewModel::class.java]
        setContent { EiyoTheme { EiyoApp(viewModel) } }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) viewModel.syncHealth()
    }
}

class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EiyoTheme {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    Text("ヘルスコネクトの利用目的", style=androidx.compose.material3.MaterialTheme.typography.headlineMedium)
                    Text(getString(R.string.health_permissions_rationale))
                    LineButton("閉じる", onClick=::finish)
                }
            }
        }
    }
}
