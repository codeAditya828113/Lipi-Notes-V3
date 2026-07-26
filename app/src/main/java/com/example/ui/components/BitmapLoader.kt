package com.example.ui.components

import android.graphics.Bitmap
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

@Composable
fun rememberImageBitmaps(images: List<ImageElement>): Map<String, ImageBitmap> {
    val context = LocalContext.current
    val bitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }
    // Only extract URIs so that position/dimension drag changes don't restart Coil fetches
    val uriList = remember(images) { images.map { it.uri }.distinct() }

    LaunchedEffect(uriList) {
        withContext(Dispatchers.IO) {
            uriList.forEach { uri ->
                if (!bitmaps.containsKey(uri)) {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(uri)
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

