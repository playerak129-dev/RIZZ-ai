package com.example.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.domain.model.AttachedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

  suspend fun processImageUri(context: Context, uri: Uri): AttachedImage? = withContext(Dispatchers.IO) {
    try {
      val contentResolver = context.contentResolver

      // 1. Decode bounds to determine sampling size
      val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
      }

      val origWidth = options.outWidth
      val origHeight = options.outHeight
      if (origWidth <= 0 || origHeight <= 0) return@withContext null

      var inSampleSize = 1
      val maxDim = max(origWidth, origHeight)
      if (maxDim > AiConfig.MAX_IMAGE_DIMENSION) {
        inSampleSize = maxDim / AiConfig.MAX_IMAGE_DIMENSION
      }

      // 2. Decode sampled bitmap
      val decodeOptions = BitmapFactory.Options().apply {
        this.inSampleSize = inSampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
      } ?: return@withContext null

      // 3. Final scale down if needed
      val finalBitmap = if (sampledBitmap.width > AiConfig.MAX_IMAGE_DIMENSION || sampledBitmap.height > AiConfig.MAX_IMAGE_DIMENSION) {
        val ratio = sampledBitmap.width.toFloat() / sampledBitmap.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (sampledBitmap.width > sampledBitmap.height) {
          targetWidth = AiConfig.MAX_IMAGE_DIMENSION
          targetHeight = (targetWidth / ratio).toInt()
        } else {
          targetHeight = AiConfig.MAX_IMAGE_DIMENSION
          targetWidth = (targetHeight * ratio).toInt()
        }
        Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
      } else {
        sampledBitmap
      }

      // 4. Compress to JPEG
      val outputStream = ByteArrayOutputStream()
      finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
      val byteArray = outputStream.toByteArray()
      val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

      AttachedImage(
        uri = uri.toString(),
        base64Data = base64,
        mimeType = "image/jpeg"
      )
    } catch (e: Exception) {
      null
    }
  }
}
