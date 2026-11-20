package com.imtihanpro.app.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import java.io.File

/**
 * Generates real binary PDF test fixtures covering:
 * 1. Mathematical equations & fractions
 * 2. High-precision vector scientific diagrams (landscape)
 * 3. Structured multi-column physics constants table
 * 4. High-res embedded raster scanned page
 * 5. Large 120-page multi-chapter textbook stress file
 */
object SyntheticPdfFixtures {

    fun createTextbookSamplePdf(outFile: File): File {
        PDDocument().use { doc ->
            // Page 1: Mathematical Textbook (Portrait A4)
            val page1 = PDPage(PDRectangle.A4)
            doc.addPage(page1)
            PDPageContentStream(doc, page1).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                cs.newLineAtOffset(52f, 770f)
                cs.showText("Chapter 1: Quadratic Equations & Matrices")
                cs.endText()

                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA, 12f)
                cs.newLineAtOffset(52f, 730f)
                cs.showText("Quadratic Formula: x = (-b +/- sqrt(b^2 - 4ac)) / (2a)")
                cs.newLineAtOffset(0f, -24f)
                cs.showText("Determinant of 2x2 Matrix A = | a  b | = ad - bc")
                cs.newLineAtOffset(0f, -24f)
                cs.showText("KPK HSSC-I Mathematics Derivation Notes")
                cs.endText()
            }

            // Page 2: Vector Scientific Diagram (Landscape Wide)
            val page2 = PDPage(PDRectangle(842f, 595f))
            doc.addPage(page2)
            PDPageContentStream(doc, page2).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                cs.newLineAtOffset(48f, 545f)
                cs.showText("Figure 2.1: Prism Refraction & Vector Ray Diagram")
                cs.endText()

                // Draw crisp vector triangle prism & incident/refracted rays
                cs.setLineWidth(2.0f)
                cs.moveTo(220f, 180f)
                cs.lineTo(420f, 440f)
                cs.lineTo(620f, 180f)
                cs.closePath()
                cs.stroke()

                // Incident & emergent rays
                cs.setLineWidth(1.5f)
                cs.moveTo(90f, 260f)
                cs.lineTo(310f, 300f)
                cs.lineTo(525f, 300f)
                cs.lineTo(740f, 210f)
                cs.stroke()
            }

            // Page 3: Multi-column Structured Table (Portrait A4)
            val page3 = PDPage(PDRectangle.A4)
            doc.addPage(page3)
            PDPageContentStream(doc, page3).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                cs.newLineAtOffset(52f, 770f)
                cs.showText("Table 3.1: Fundamental SI Constants (KPK Physics)")
                cs.endText()

                val rows = listOf(
                    Triple("Quantity", "Unit", "Value"),
                    Triple("Acceleration (g)", "m/s2", "9.81 m/s2"),
                    Triple("Gravitational Force", "Newton", "F = G m1 m2 / r^2"),
                    Triple("Planck Constant (h)", "Joule-sec", "6.626 x 10^-34")
                )
                val startY = 720f
                val rowH = 34f
                cs.setLineWidth(1.0f)
                rows.forEachIndexed { idx, row ->
                    val y = startY - idx * rowH
                    cs.addRect(52f, y - 22f, 480f, rowH)
                    cs.stroke()
                    cs.beginText()
                    cs.setFont(if (idx == 0) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, 11f)
                    cs.newLineAtOffset(60f, y - 10f)
                    cs.showText(row.first)
                    cs.newLineAtOffset(180f, 0f)
                    cs.showText(row.second)
                    cs.newLineAtOffset(130f, 0f)
                    cs.showText(row.third)
                    cs.endText()
                }
            }

            // Page 4: Embedded Raster Scanned Past Paper Page
            val page4 = PDPage(PDRectangle.A4)
            doc.addPage(page4)
            val scanBmp = Bitmap.createBitmap(420, 560, Bitmap.Config.ARGB_8888).apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.WHITE)
                val paint = Paint().apply {
                    color = Color.DKGRAY
                    strokeWidth = 4f
                }
                canvas.drawRect(24f, 24f, 396f, 536f, paint)
                canvas.drawLine(24f, 110f, 396f, 110f, paint)
                canvas.drawCircle(210f, 300f, 80f, paint)
            }
            val pdScan = LosslessFactory.createFromImage(doc, scanBmp)
            PDPageContentStream(doc, page4).use { cs ->
                cs.drawImage(pdScan, 65f, 160f, 465f, 620f)
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA, 10f)
                cs.newLineAtOffset(65f, 130f)
                cs.showText("Scanned Past Paper Archive - BISE Peshawar 2024")
                cs.endText()
            }

            // Page 5: Summary Formulas
            val page5 = PDPage(PDRectangle.A4)
            doc.addPage(page5)
            PDPageContentStream(doc, page5).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                cs.newLineAtOffset(52f, 770f)
                cs.showText("Summary: Key Board Examination Equations")
                cs.newLineAtOffset(0f, -28f)
                cs.setFont(PDType1Font.HELVETICA, 11f)
                cs.showText("Work-Energy Theorem: W = delta K = 1/2 m v^2 - 1/2 m u^2")
                cs.endText()
            }

            // Add real Document Outline / Table of Contents
            val outline = PDDocumentOutline()
            doc.documentCatalog.documentOutline = outline
            outline.addLast(PDOutlineItem().apply {
                title = "Chapter 1: Quadratic Equations & Matrices"
                setDestination(page1)
            })
            outline.addLast(PDOutlineItem().apply {
                title = "Chapter 2: Prism Refraction Diagram"
                setDestination(page2)
            })
            outline.addLast(PDOutlineItem().apply {
                title = "Summary Formulas"
                setDestination(page5)
            })

            doc.save(outFile)
        }
        return outFile
    }

    fun createLargeStressPdf(outFile: File, pageCount: Int = 120): File {
        PDDocument().use { doc ->
            for (p in 1..pageCount) {
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                PDPageContentStream(doc, page).use { cs ->
                    cs.beginText()
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                    cs.newLineAtOffset(50f, 760f)
                    cs.showText("Class 11 Textbook Page $p of $pageCount")
                    if (p == 95) {
                        cs.newLineAtOffset(0f, -24f)
                        cs.showText("Chapter 95 Benchmark: Torque tau = r x F")
                    }
                    cs.endText()
                }
            }
            doc.save(outFile)
        }
        return outFile
    }

    fun createCorruptPdf(outFile: File): File {
        outFile.writeText("NOT_A_VALID_PDF_HEADER_CORRUPTED_BYTES")
        return outFile
    }
}
