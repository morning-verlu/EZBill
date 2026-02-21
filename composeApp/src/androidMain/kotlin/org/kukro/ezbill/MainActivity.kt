package org.kukro.ezbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.kukro.ezbill.app.AppRootIntent
import org.kukro.ezbill.di.AppGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        AppGraph.rootStateMachine.dispatch(AppRootIntent.AppForeground)
    }

    override fun onStop() {
        AppGraph.rootStateMachine.dispatch(AppRootIntent.AppBackground)
        super.onStop()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
