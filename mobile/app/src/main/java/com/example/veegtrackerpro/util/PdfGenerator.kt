package com.example.veegtrackerpro.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.veegtrackerpro.R
import com.example.veegtrackerpro.data.local.entities.Route
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateRouteReport(context: Context, route: Route, pointCount: Int): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.report_title), 50f, 50f, paint)

        // Details
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateStr = dateFormat.format(Date(route.createdAt))

        canvas.drawText("${context.getString(R.string.tab_routes)}: ${route.name}", 50f, 100f, paint)
        canvas.drawText("${context.getString(R.string.label_date)}: $dateStr", 50f, 130f, paint)
        canvas.drawText(context.getString(R.string.report_driver), 50f, 160f, paint)
        canvas.drawText(context.getString(R.string.report_total_points, pointCount), 50f, 190f, paint)
        canvas.drawText(context.getString(R.string.report_summary), 50f, 220f, paint)

        pdfDocument.finishPage(page)

        val fileName = "Rapportage_${route.id}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
