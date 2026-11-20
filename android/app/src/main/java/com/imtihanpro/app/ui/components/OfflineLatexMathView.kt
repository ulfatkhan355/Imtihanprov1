package com.imtihanpro.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.imtihanpro.app.ui.theme.ImtihanMonoFamily
import com.imtihanpro.app.ui.theme.LocalImtihanColors

/**
 * Offline-first LaTeX & Scientific Equation Renderer.
 * Converts fractions, square roots, matrices, determinants, subscripts, superscripts,
 * Greek symbols, and chemical arrows into crisp HTML+CSS without network or JS dependencies.
 */
@Composable
fun OfflineLatexMathView(
    latex: String,
    plainFallback: String,
    fontScaleMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    val colors = LocalImtihanColors.current
    val isPreview = LocalInspectionMode.current
    val source = latex.ifBlank { plainFallback }

    if (isPreview) {
        Text(
            text = plainFallback.ifBlank { source },
            fontFamily = ImtihanMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * fontScaleMultiplier).sp,
            color = colors.textPrimary,
            modifier = modifier.padding(vertical = 4.dp)
        )
        return
    }

    val textHex = remember(colors.textPrimary) {
        String.format("#%06X", 0xFFFFFF and colors.textPrimary.toArgb())
    }
    val accentHex = remember(colors.accentBlue) {
        String.format("#%06X", 0xFFFFFF and colors.accentBlue.toArgb())
    }
    val htmlDocument = remember(source, textHex, accentHex, fontScaleMultiplier) {
        buildMathHtml(
            rawLatex = source,
            textHex = textHex,
            accentHex = accentHex,
            fontSizePx = (16f * fontScaleMultiplier).toInt().coerceIn(13, 28)
        )
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = true
                @SuppressLint("SetJavaScriptEnabled")
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.loadsImagesAutomatically = false
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlDocument, "text/html", "UTF-8", null)
        }
    )
}

internal fun buildMathHtml(
    rawLatex: String,
    textHex: String,
    accentHex: String,
    fontSizePx: Int
): String {
    val body = latexToHtmlFragment(rawLatex)
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              background: transparent;
              color: $textHex;
              font-family: 'Noto Serif', Georgia, serif;
              font-size: ${fontSizePx}px;
              line-height: 1.55;
              padding: 4px 2px;
              overflow-x: auto;
              white-space: nowrap;
            }
            .math-expr {
              display: inline-flex;
              align-items: center;
              flex-wrap: wrap;
              gap: 3px;
            }
            .frac {
              display: inline-flex;
              flex-direction: column;
              align-items: center;
              vertical-align: middle;
              margin: 0 3px;
              font-size: 0.88em;
            }
            .frac .num {
              padding: 0 4px 2px 4px;
              border-bottom: 1.5px solid $textHex;
            }
            .frac .den {
              padding: 2px 4px 0 4px;
            }
            .sqrt {
              display: inline-flex;
              align-items: stretch;
              vertical-align: middle;
              margin: 0 2px;
            }
            .sqrt .rad {
              font-size: 1.15em;
              line-height: 1;
              color: $accentHex;
              margin-right: 1px;
            }
            .sqrt .radicand {
              border-top: 1.4px solid $textHex;
              padding: 1px 3px 0 2px;
            }
            .matrix-wrap {
              display: inline-flex;
              align-items: center;
              vertical-align: middle;
              margin: 2px 4px;
              padding: 2px 6px;
              border-left: 1.8px solid $textHex;
              border-right: 1.8px solid $textHex;
              border-radius: 3px;
            }
            .matrix-wrap.paren {
              border-radius: 10px;
            }
            .matrix-wrap.det {
              border-radius: 0;
            }
            .matrix-grid {
              display: grid;
              gap: 4px 10px;
              text-align: center;
            }
            sup, sub { font-size: 0.72em; line-height: 0; }
            sup { vertical-align: super; }
            sub { vertical-align: sub; }
          </style>
        </head>
        <body><span class="math-expr">$body</span></body>
        </html>
    """.trimIndent()
}

internal fun latexToHtmlFragment(input: String): String {
    var s = input.trim()
        .replace("\\(", "")
        .replace("\\)", "")
        .replace("\\[", "")
        .replace("\\]", "")
        .replace("$", "")

    s = escapeHtml(s)

    // Matrices & Determinants: \begin{bmatrix} ... \end{bmatrix}, pmatrix, vmatrix
    val matrixRegex = Regex("\\\\begin\\{(bmatrix|pmatrix|vmatrix|matrix)\\}([\\s\\S]*?)\\\\end\\{\\1\\}")
    s = matrixRegex.replace(s) { match ->
        val kind = match.groupValues[1]
        val content = match.groupValues[2].trim()
        val rows = content.split("\\\\").map { row ->
            row.split("&").map { cell -> cell.trim() }
        }.filter { it.isNotEmpty() }
        val cols = rows.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        val wrapClass = when (kind) {
            "pmatrix" -> "matrix-wrap paren"
            "vmatrix" -> "matrix-wrap det"
            else -> "matrix-wrap"
        }
        val cellsHtml = rows.joinToString("") { row ->
            row.joinToString("") { cell -> "<span>${formatSimpleTokens(cell)}</span>" }
        }
        "<span class=\"$wrapClass\"><span class=\"matrix-grid\" style=\"grid-template-columns:repeat($cols, auto);\">$cellsHtml</span></span>"
    }

    // Nested-safe single-level fractions \frac{a}{b}
    val fracRegex = Regex("\\\\frac\\{([^{}]+)\\}\\{([^{}]+)\\}")
    repeat(3) {
        s = fracRegex.replace(s) { m ->
            "<span class=\"frac\"><span class=\"num\">${m.groupValues[1]}</span><span class=\"den\">${m.groupValues[2]}</span></span>"
        }
    }

    // Square roots \sqrt{x}
    val sqrtRegex = Regex("\\\\sqrt\\{([^{}]+)\\}")
    s = sqrtRegex.replace(s) { m ->
        "<span class=\"sqrt\"><span class=\"rad\">&#8730;</span><span class=\"radicand\">${m.groupValues[1]}</span></span>"
    }

    return formatSimpleTokens(s)
}

private fun formatSimpleTokens(input: String): String {
    var out = input
        .replace("\\alpha", "&#945;")
        .replace("\\beta", "&#946;")
        .replace("\\gamma", "&#947;")
        .replace("\\delta", "&#948;")
        .replace("\\Delta", "&#916;")
        .replace("\\epsilon", "&#949;")
        .replace("\\theta", "&#952;")
        .replace("\\lambda", "&#955;")
        .replace("\\mu", "&#956;")
        .replace("\\pi", "&#960;")
        .replace("\\rho", "&#961;")
        .replace("\\sigma", "&#963;")
        .replace("\\omega", "&#969;")
        .replace("\\Omega", "&#937;")
        .replace("\\phi", "&#966;")
        .replace("\\infty", "&#8734;")
        .replace("\\times", " &#215; ")
        .replace("\\cdot", " &#183; ")
        .replace("\\pm", " &#177; ")
        .replace("\\leq", " &#8804; ")
        .replace("\\geq", " &#8805; ")
        .replace("\\neq", " &#8800; ")
        .replace("\\approx", " &#8776; ")
        .replace("\\rightarrow", " &#8594; ")
        .replace("\\to", " &#8594; ")
        .replace("\\leftrightarrow", " &#8652; ")
        .replace("\\sum", "&#8721;")
        .replace("\\int", "&#8747;")
        .replace("\\degree", "&#176;")
        .replace("\\circ", "&#176;")

    out = Regex("\\\\vec\\{([^{}]+)\\}").replace(out) { "<b>${it.groupValues[1]}</b>&#8407;" }
    out = Regex("\\\\bar\\{([^{}]+)\\}").replace(out) { "<span style=\"text-decoration:overline;\">${it.groupValues[1]}</span>" }
    out = Regex("\\\\text\\{([^{}]+)\\}").replace(out) { "<span>${it.groupValues[1]}</span>" }

    out = Regex("\\^\\{([^{}]+)\\}").replace(out) { "<sup>${it.groupValues[1]}</sup>" }
    out = Regex("_\\{([^{}]+)\\}").replace(out) { "<sub>${it.groupValues[1]}</sub>" }
    out = Regex("\\^([A-Za-z0-9+\\-])").replace(out) { "<sup>${it.groupValues[1]}</sup>" }
    out = Regex("_([A-Za-z0-9+\\-])").replace(out) { "<sub>${it.groupValues[1]}</sub>" }

    return out
}

private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
