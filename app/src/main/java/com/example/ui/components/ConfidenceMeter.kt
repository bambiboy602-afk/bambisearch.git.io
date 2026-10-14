package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RelevanceClassification
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.SlateSurfaceBorder
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class ReliabilityTier(
    val label: String,
    val badgeText: String,
    val color: Color,
    val icon: ImageVector,
    val defaultRationale: String
) {
    HIGH(
        label = "High Confidence",
        badgeText = "HIGH RELIABILITY",
        color = CyberEmerald,
        icon = Icons.Default.VerifiedUser,
        defaultRationale = "Verified original document, primary manual, or reproducible lab"
    ),
    MEDIUM(
        label = "Medium Confidence",
        badgeText = "MEDIUM RELIABILITY",
        color = CyberAmber,
        icon = Icons.Default.Warning,
        defaultRationale = "Reference citation or public community documentation"
    ),
    LOW(
        label = "Low Confidence",
        badgeText = "LOW RELIABILITY",
        color = CyberRed,
        icon = Icons.Default.ReportProblem,
        defaultRationale = "Secondary reporting, opinion, or unverified speculative lead"
    );

    companion object {
        fun fromScore(score: Int): ReliabilityTier = when {
            score >= 80 -> HIGH
            score >= 50 -> MEDIUM
            else -> LOW
        }
    }
}

/**
 * Compact inline confidence badge (e.g. for card headers or list summaries).
 */
@Composable
fun ConfidenceScoreBadge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val tier = ReliabilityTier.fromScore(score)
    val animatedColor by animateColorAsState(targetValue = tier.color, label = "badgeColor")

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(animatedColor.copy(alpha = 0.15f))
            .border(1.dp, animatedColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("confidence_badge_$score"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tier.icon,
            contentDescription = tier.label,
            tint = animatedColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$score%",
            color = animatedColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = tier.label,
            color = animatedColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Full interactive / detailed AI Confidence Score Meter component for evidence items.
 * Displays:
 * 1. Reliability tier indicator (Green = High, Yellow/Amber = Medium, Red = Low)
 * 2. Exact score percentage in bold monospace
 * 3. Animated progress gauge
 * 4. 3-stage reliability tick zones
 * 5. Explanatory reliability rationale based on classification
 */
@Composable
fun ConfidenceScoreMeter(
    score: Int,
    classification: RelevanceClassification? = null,
    modifier: Modifier = Modifier
) {
    val tier = ReliabilityTier.fromScore(score)

    val animatedColor by animateColorAsState(
        targetValue = tier.color,
        animationSpec = tween(400),
        label = "meterColor"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "meterProgress"
    )

    val rationaleText = when (classification) {
        RelevanceClassification.INSTRUCTIONAL -> "Instructional how-to / tutorial: provides concrete implementation guidance (+25%)"
        RelevanceClassification.PRIMARY_SOURCE -> "Primary source: original specification, code, or formal whitepaper (+20%)"
        RelevanceClassification.HISTORICAL -> "Historical archive: authentic preservation capture from verifiable source"
        RelevanceClassification.REFERENCE -> "Reference material: encyclopedic record or formal standard table"
        RelevanceClassification.NEWS -> "Secondary event news: contains high churn or secondhand reporting (-25%)"
        RelevanceClassification.OPINION -> "Forum opinion or commentary: unvetted claims requiring corroboration (-15%)"
        RelevanceClassification.UNVERIFIED -> "Unverified lead: speculative finding flagged for deeper verification"
        null -> tier.defaultRationale
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlateSurfaceElevated)
            .border(1.dp, SlateSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
            .testTag("confidence_meter_box")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Reliability Tier Badge + Percentage Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tier tag
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(animatedColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tier.badgeText,
                        color = animatedColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Numeric score
                Text(
                    text = "$score / 100",
                    color = animatedColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Progress Meter Gauge
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = animatedColor,
                trackColor = SlateSurfaceBorder
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Zone Reliability Scale Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0% [LOW]",
                    color = if (tier == ReliabilityTier.LOW) CyberRed else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (tier == ReliabilityTier.LOW) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "50% [MEDIUM]",
                    color = if (tier == ReliabilityTier.MEDIUM) CyberAmber else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (tier == ReliabilityTier.MEDIUM) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "80%–100% [HIGH]",
                    color = if (tier == ReliabilityTier.HIGH) CyberEmerald else TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (tier == ReliabilityTier.HIGH) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rationale explanation text
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = animatedColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .size(12.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rationaleText,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
