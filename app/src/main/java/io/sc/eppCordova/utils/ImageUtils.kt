package io.sc.eppCordova.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
            color = Color.YELLOW
            textSize = 60f
            isAntiAlias = true
            setShadowLayer(5f, 2f, 2f, Color.BLACK)
        }

        val timeStamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val lines = listOf(
            "Gat: $gatNumber",
            "Lat: $lat, Lon: $lon",
            "Time: $timeStamp",
            "Damage: $disasterType"
        )

        var y = mutableBitmap.height - (lines.size * 70f) - 20f
        val x = 20f

        for (line in lines) {
            canvas.drawText(line, x, y, paint)
            y += 70f
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
