package io.sc.eppCordova.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import io.sc.eppCordova.lossclaim.data.FarmerEntity
import io.sc.eppCordova.lossclaim.domain.model.EvidencePhoto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateSurveyReport(
        context: Context,
        farmer: FarmerEntity,
        photos: List<EvidencePhoto>,
        damageScore: Int,
        estimatedPayout: Double
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
        }

        val date = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

        var y = 50f
        val x = 40f

        canvas.drawText("Krishi Prabandh SwaSurvey - AI Crop Survey Report", x, y, titlePaint)
        y += 40f

        canvas.drawText("Date: $date", x, y, textPaint)
        y += 40f

        // 1. Farmer Details
        canvas.drawText("1. Farmer Details", x, y, headerPaint)
        y += 25f
        canvas.drawText("Name: ${farmer.farmerName}", x, y, textPaint)
        y += 20f
        canvas.drawText("Mobile: ${farmer.mobileNumber}", x, y, textPaint)
        y += 20f
        canvas.drawText("Village: ${farmer.village}, Taluka: ${farmer.taluka}", x, y, textPaint)
        y += 40f

        // 2. Crop Details
        canvas.drawText("2. Crop & Land Details", x, y, headerPaint)
        y += 25f
        canvas.drawText("Gat Number: ${farmer.gatNumber}", x, y, textPaint)
        y += 20f
        canvas.drawText("Primary Crop: ${farmer.primaryCrop ?: "N/A"}", x, y, textPaint)
        y += 20f
        val secCrop = farmer.secondaryCrop?.trim()
        if (!secCrop.isNullOrEmpty() && secCrop.lowercase() != "none" && secCrop.lowercase() != "na" && secCrop.lowercase() != "null") {
            canvas.drawText("Secondary Crop: $secCrop", x, y, textPaint)
            y += 20f
        }
        canvas.drawText("Area: ${farmer.area} Hectares", x, y, textPaint)
        y += 40f

        // 3. AI Assessment
        canvas.drawText("3. AI Damage Assessment", x, y, headerPaint)
        y += 25f
        canvas.drawText("Disaster Type: ${farmer.insuranceStatus}", x, y, textPaint) // Need disaster type here, but we pass it below
        y += 20f
        canvas.drawText("Final Damage Score: $damageScore%", x, y, textPaint)
        y += 20f
        canvas.drawText("Estimated Proportional Payout: ₹$estimatedPayout", x, y, textPaint)
        y += 40f

        // 4. Evidence Photos
        canvas.drawText("4. Geo-tagged Evidence Photos", x, y, headerPaint)
        y += 25f

        for (photo in photos) {
            if (y > 700) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }

            val file = File(photo.imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false)
                    canvas.drawBitmap(scaledBitmap, x, y, null)
                    
                    val textX = x + 270f
                    var textY = y + 40f
                    canvas.drawText("Lat: ${photo.latitude}", textX, textY, textPaint)
                    textY += 20f
                    canvas.drawText("Lon: ${photo.longitude}", textX, textY, textPaint)
                    textY += 20f
                    canvas.drawText("Disaster: ${photo.disasterType}", textX, textY, textPaint)
                    
                    y += 270f
                }
            }
        }

        document.finishPage(page)

        // Save PDF
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
        val pdfFile = File(storageDir, "AI_Survey_Report_${farmer.mobileNumber}_${System.currentTimeMillis()}.pdf")
        
        return try {
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            document.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }
}