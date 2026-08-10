package com.example.ui.components

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.example.data.ImageElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun rememberImageBitmaps(images: List<ImageElement>): Map<String, ImageBitmap> {
    val context = LocalContext.current
    val bitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }
    // Only extract URIs so that position/dimension drag changes don't restart Coil fetches
    val uriList = remember(images) { images.map { it.uri }.filter { it.isNotBlank() }.distinct() }

    LaunchedEffect(uriList) {
        withContext(Dispatchers.IO) {
            uriList.forEach { uri ->
                if (!bitmaps.containsKey(uri)) {
                    try {
                        val cleanPath = uri.removePrefix("file://").removePrefix("file:")
                        val file = File(cleanPath)
                        val relativeFile = File(context.filesDir, cleanPath)
                        val modelToUse: Any = when {
                            file.exists() -> file
                            relativeFile.exists() -> relativeFile
                            else -> uri
                        }

                        val request = ImageRequest.Builder(context)
                            .data(modelToUse)
                            .size(2048, 2048)
                            .precision(Precision.INEXACT)
                            .allowHardware(true)
                            .build()
                        val result = context.imageLoader.execute(request)
                        if (result is SuccessResult) {
                            val drawable = result.drawable
                            if (drawable is BitmapDrawable) {
                                val imageBitmap = drawable.bitmap.asImageBitmap()
                                bitmaps[uri] = imageBitmap
                            }
                        } else if (file.exists() || relativeFile.exists()) {
                            val targetFile = if (file.exists()) file else relativeFile
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                            }
                            val bmp = BitmapFactory.decodeFile(targetFile.absolutePath, options)
                            if (bmp != null) {
                                bitmaps[uri] = bmp.asImageBitmap()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    return bitmaps
}

