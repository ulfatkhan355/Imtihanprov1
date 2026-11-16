package com.imtihanpro.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imtihanpro.app.domain.model.ContentBlockUi
import com.imtihanpro.app.ui.theme.LocalImtihanColors

/**
 * Single reusable structured educational content renderer.
 * Supports all 11 content types without duplicating screen layouts per chapter.
 */
@Composable
fun ContentBlockRenderer(
    blocks: List<ContentBlockUi>,
    fontScaleMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        blocks.forEach { block ->
            RenderSingleBlock(block = block, fontScaleMultiplier = fontScaleMultiplier)
        }
    }
}

@Composable
private fun RenderSingleBlock(block: ContentBlockUi, fontScaleMultiplier: Float) {
    val colors = LocalImtihanColors.current

    when (block) {
        is ContentBlockUi.Heading -> {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                if (!block.sectionCode.isNullOrBlank()) {
                    Text(
                        text = "Section ${block.sectionCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentBlue
                    )
                }
                Text(
                    text = if (block.sectionCode != null) "${block.sectionCode} ${block.text}" else block.text,
                    style = if (block.isSubheading) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontSize = ((if (block.isSubheading) 16 else 20) * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
            }
        }

        is ContentBlockUi.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = (14 * fontScaleMultiplier).sp,
                lineHeight = (22 * fontScaleMultiplier).sp,
                color = colors.textSecondary
            )
        }

        is ContentBlockUi.HighlightedExamPoint -> {
            val tintBg = if (colors.isDark) Color(0xFF28200D) else Color(0xFFFFFBEB)
            val tintBorder = if (colors.isDark) Color(0xFF8A6318) else Color(0xFFFCD34D)
            val tintText = if (colors.isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
            Surface(
                shape = RoundedCornerShape(14.dp),
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
                            fontSize = (13 * fontScaleMultiplier).sp,
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
                        tint = colors.accentBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Definition · ${block.term}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentBlue
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = block.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = (14 * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
                if (!block.formulaHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.formulaHint,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.accentBlue
                    )
                }
            }
        }

        is ContentBlockUi.MathExpression -> {
            SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = block.plainNotation,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (15 * fontScaleMultiplier).sp,
                    color = colors.textPrimary
                )
                if (!block.caption.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
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
                            text = header,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accentBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = colors.cardBorder.copy(alpha = 0.6f)
                )
                block.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { cell ->
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        is ContentBlockUi.BulletOrNumberedList -> {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        color = colors.textSecondary
                    )
                }
            }
        }

        is ContentBlockUi.WorkedExample -> {
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentBlue
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
                        color = colors.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Result: ${block.finalAnswer}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.success
                )
            }
        }

        is ContentBlockUi.Diagram -> {
            SilverBlueCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = block.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(colors.canvasDeep, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = block.altText,
                            tint = colors.accentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Offline-ready Diagram (${block.assetKey})",
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
        }

        is ContentBlockUi.InlineMcq -> {
            var chosenId by remember(block.id) { mutableStateOf<String?>(null) }
            SilverBlueCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Check MCQ",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentBlue
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
                        text = "${block.questionType.uppercase()} · ${block.marks} Marks",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentBlue
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
                    color = colors.textPrimary
                )
                if (expanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    block.answerSteps.forEachIndexed { idx, step ->
                        Text(
                            text = "${idx + 1}. $step",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
