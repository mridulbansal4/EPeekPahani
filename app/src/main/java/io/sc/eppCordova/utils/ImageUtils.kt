package io.sc.eppCordova.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    fun createImageUri(context: Context): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    fun addGeoWatermark(file: File, lat: Double, lon: Double, gatNumber: String, disasterType: String): File {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return file
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#99000000") // Semi-transparent black
            style = Paint.Style.FILL
        }

        val timeStamp = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date())
        
        val lines = listOf(
            "LAT: $lat",
            "LON: $lon",
            "ACC: ±4m",
            "TIME: $timeStamp",
            "GAT: $gatNumber",
            "DISASTER: $disasterType",
            "STAGE: FIELD INSPECTION"
        )

        // Calculate background box size
        var maxTextWidth = 0f
        for (line in lines) {
            val width = paint.measureText(line)
            if (width > maxTextWidth) maxTextWidth = width
        }

        val padding = 30f
        val lineHeight = 60f
        val boxHeight = (lines.size * lineHeight) + padding * 2
        val boxWidth = maxTextWidth + padding * 2

        // Draw at bottom left
        val startX = 20f
        val startY = mutableBitmap.height - boxHeight - 20f

        val rect = RectF(startX, startY, startX + boxWidth, startY + boxHeight)
        canvas.drawRoundRect(rect, 15f, 15f, bgPaint)

        var textY = startY + padding + 40f
        val textX = startX + padding

        for (line in lines) {
            canvas.drawText(line, textX, textY, paint)
            textY += lineHeight
        }

        try {
            val out = FileOutputStream(file)
            mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return file
    }
}
