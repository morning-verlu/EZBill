package org.kukro.ezbill

import androidx.compose.runtime.Composable

expect class ImagePicker {
    suspend fun pickImageBytes(): ByteArray?
}

@Composable
expect fun rememberImagePicker(): ImagePicker