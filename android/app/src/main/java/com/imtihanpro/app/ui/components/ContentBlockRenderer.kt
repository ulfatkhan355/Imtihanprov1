package com.imtihanpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.ui.theme.ImtihanMonoFamily
import com.imtihanpro.app.ui.theme.LocalImtihanColors

/**
 * Single reusable structured educational content renderer.
 * Supports all 11 content types, tap-to-copy formulas, and 1-tap "Night Before Exam" high-yield filtering.
 */
@Composable
fun ContentBlockRenderer(
    blocks: List<ContentBlockUi>,
    fontScaleMultiplier: Float = 1f,
    nightBeforeOnly: Boolean = false,
    onFormulaCopied: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visibleBlocks = remember(blocks, nightBeforeOnly) {
        if (!nightBeforeOnly) blocks
        else blocks.filter {
            it is ContentBlockUi.Heading ||
                it is ContentBlockUi.ImportantDefinition ||
                it is ContentBlockUi.MathExpression ||
                it is ContentBlockUi.HighlightedExamPoint
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        visibleBlocks.forEach { block ->
            RenderSingleBlock(
                block = block,
                fontScaleMultiplier = fontScaleMultiplier,
                onFormulaCopied = onFormulaCopied
            )
        }
    }
}

@Composable
private fun RenderSingleBlock(
    block: ContentBlockUi,
    fontScaleMultiplier: Float,
    onFormulaCopied: (() -> Unit)?
) {
    val colors = LocalImtihanColors.current
    val clipboardManager = LocalClipboardManager.current

    when (block) {
        is ContentBlockUi.Heading -> {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                if (!block.sectionCode.isNullOrBlank()) {
                    Text(
                        text = "SECTION ${block.sectionCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cyanPulse
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = if (block.sectionCode != null) "${block.sectionCode}  ${block.text}" else block.text,
                    style = if (block.isSubheading) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontSize = ((if (block.isSubheading) 16 else 20) * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = colors.cardBorder.copy(alpha = 0.35f)
                )
            }
        }

        is ContentBlockUi.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = (14.5f * fontScaleMultiplier).sp,
                lineHeight = (23f * fontScaleMultiplier).sp,
                color = colors.textSecondary
            )
        }

        is ContentBlockUi.HighlightedExamPoint -> {
            val tintBg = if (colors.isDark) Color(0xFF231A0A) else Color(0xFFFFFBEB)
            val tintBorder = if (colors.isDark) Color(0xFF9E6F1A) else Color(0xFFFCD34D)
            val tintText = if (colors.isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tintBg,
                border = BorderStroke(1.dp, tintBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = colors.warningGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = tintText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    block.points.forEach { pt ->
                        Text(
                            text = "• $pt",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = (13.5f * fontScaleMultiplier).sp,
                            lineHeight = (20f * fontScaleMultiplier).sp,
                            color = tintText,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        is ContentBlockUi.ImportantDefinition -> {
            SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = colors.cyanPulse,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DEFINITION · ${block.term.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cyanPulse
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = (14 * fontScaleMultiplier).sp,
                    lineHeight = (21 * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
                if (!block.formulaHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = block.formulaHint,
                        fontFamily = ImtihanMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (13 * fontScaleMultiplier).sp,
                        color = colors.cyanPulse
                    )
                }
            }
        }

        is ContentBlockUi.MathExpression -> {
            var copied by remember(block.id) { mutableStateOf(false) }
            SilverBlueCard(
                elevated = true,
                onClick = {
                    clipboardManager.setText(AnnotatedString(block.plainNotation))
                    copied = true
                    onFormulaCopied?.invoke()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FORMULA",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cyanPulse
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy formula",
                            tint = if (copied) colors.success else colors.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (copied) "COPIED" else "TAP TO COPY",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (copied) colors.success else colors.textMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                OfflineLatexMathView(
                    latex = block.latex,
                    plainFallback = block.plainNotation,
                    fontScaleMultiplier = fontScaleMultiplier
                )
                if (!block.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        is ContentBlockUi.Table -> {
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                if (!block.title.isNullOrBlank()) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    block.headers.forEach { header ->
                        Text(
                            text = header.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.cyanPulse,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = colors.cardBorder.copy(alpha = 0.6f)
                )
                block.rows.forEachIndexed { rIdx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = (12.5f * fontScaleMultiplier).sp,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (rIdx < block.rows.lastIndex) {
                        HorizontalDivider(color = colors.cardBorder.copy(alpha = 0.2f))
                    }
                }
            }
        }

        is ContentBlockUi.BulletOrNumberedList -> {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (!block.title.isNullOrBlank()) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                }
                block.items.forEachIndexed { index, item ->
                    val prefix = if (block.ordered) "${index + 1}." else "•"
                    Text(
                        text = "$prefix $item",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = (14 * fontScaleMultiplier).sp,
                        lineHeight = (21 * fontScaleMultiplier).sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        is ContentBlockUi.WorkedExample -> {
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = block.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cyanPulse
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = block.problemStatement,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                block.steps.forEachIndexed { i, step ->
                    Text(
                        text = "${i + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = (13 * fontScaleMultiplier).sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Result: ${block.finalAnswer}",
                    fontFamily = ImtihanMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (13 * fontScaleMultiplier).sp,
                    color = colors.success
                )
            }
        }

        is ContentBlockUi.Diagram -> {
            var expandedDiagram by remember(block.id) { mutableStateOf(false) }
            SilverBlueCard(
                elevated = true,
                onClick = { expandedDiagram = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = block.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "TAP TO ZOOM",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accentBlue
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .background(colors.canvasDeep, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = block.altText,
                            tint = colors.accentBlue,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scientific Schematic · ${block.assetKey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }

            if (expandedDiagram) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { expandedDiagram = false },
                    title = { Text(block.title) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .background(colors.canvasDeep, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = block.altText,
                                        tint = colors.accentBlue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = block.assetKey,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.accentBlue
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = block.altText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                            Text(
                                text = block.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { expandedDiagram = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }

        is ContentBlockUi.InlineMcq -> {
            var chosenId by remember(block.id) { mutableStateOf<String?>(null) }
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "QUICK CHECK MCQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cyanPulse
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.question,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    block.options.forEach { opt ->
                        val selected = chosenId == opt.id
                        val isCorrect = opt.id == block.correctOptionId
                        val borderColor = when {
                            chosenId == null -> colors.cardBorder
                            isCorrect -> colors.success
                            selected -> colors.danger
                            else -> colors.cardBorder
                        }
                        Surface(
                            onClick = { chosenId = opt.id },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${opt.id}. ${opt.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                if (chosenId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = block.explanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        is ContentBlockUi.SolvedQuestion -> {
            var expanded by remember(block.id) { mutableStateOf(true) }
            SilverBlueCard(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${block.questionType.uppercase()} · ${block.marks} MARKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.cyanPulse
                    )
                    if (!block.yearReference.isNullOrBlank()) {
                        Text(
                            text = block.yearReference,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.question,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = (14.5f * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    block.answerSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = (13 * fontScaleMultiplier).sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
