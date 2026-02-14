package org.kukro.ezbill

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

actual class ImagePicker(
    private val launchPicker: (CompletableDeferred<ByteArray?>) -> Unit
) {
    actual suspend fun pickImageBytes(): ByteArray? {
        val deferred = CompletableDeferred<ByteArray?>()
        launchPicker(deferred)
        return deferred.await()
    }
}

@Composable
actual fun rememberImagePicker(): ImagePicker {
    val pending = remember { mutableListOf<CompletableDeferred<ByteArray?>>() }

    val delegate = remember {
        object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                val deferred = pending.removeFirstOrNull() ?: run {
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                val result = didFinishPicking.firstOrNull() as? PHPickerResult
                val provider = result?.itemProvider

                if (provider == null || !provider.hasItemConformingToTypeIdentifier("public.image")) {
                    deferred.complete(null)
                    picker.dismissViewControllerAnimated(true, null)
                    return
                }

                provider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
                    val bytes = data?.toByteArray()
                    dispatch_async(dispatch_get_main_queue()) {
                        deferred.complete(bytes)
                        picker.dismissViewControllerAnimated(true, null)
                    }
                }
            }
        }
    }

    return remember(delegate) {
        ImagePicker(
            launchPicker = { deferred ->
                val root = UIApplication.sharedApplication.keyWindow?.rootViewController
                val top = root?.topMostViewController()
                if (top == null) {
                    deferred.complete(null)
                    return@ImagePicker
                }

                pending.add(deferred)

                val configuration = PHPickerConfiguration()
                configuration.filter = PHPickerFilter.imagesFilter()
                configuration.selectionLimit = 1

                val picker = PHPickerViewController(configuration)
                picker.delegate = delegate
                top.presentViewController(picker, true, null)
            }
        )
    }
}

private fun UIViewController.topMostViewController(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val output = ByteArray(size)
    output.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return output
}
