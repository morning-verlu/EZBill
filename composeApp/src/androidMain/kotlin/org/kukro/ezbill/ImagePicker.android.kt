package org.kukro.ezbill

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.AppLaunchChecker
import kotlinx.coroutines.CompletableDeferred

actual class ImagePicker(
    private val launchPicker: (CompletableDeferred<Uri?>) -> Unit,
    private val context: Context
) {
    actual suspend fun pickImageBytes(): ByteArray? {
        val deferred = CompletableDeferred<Uri?>()
        launchPicker(deferred)
        val uri = deferred.await() ?: return null
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val context = LocalContext.current
    val pending = remember { mutableListOf<CompletableDeferred<Uri?>>() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val deferred = pending.removeFirstOrNull()
        deferred?.complete(uri)
    }

    return remember(context, launcher) {
        ImagePicker(
            launchPicker = { deferred ->
                pending.add(deferred)
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            context = context
        )
    }
}
