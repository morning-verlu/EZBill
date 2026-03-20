package org.kukro.ezbill.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.kukro.ezbill.AppConfig
import org.kukro.ezbill.models.Profile
import org.kukro.ezbill.screenModels.TransferSuggestion
import org.kukro.ezbill.screenModels.UserNet

@Composable
fun SettlementTransferGraph(
    transfers: List<TransferSuggestion>,
    memberProfiles: Map<String, Profile>,
    nets: List<UserNet>,
    modifier: Modifier = Modifier
) {
    if (transfers.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            transfers.forEachIndexed { index, transfer ->
                TransferRow(
                    transfer = transfer,
                    memberProfiles = memberProfiles
                )
                if (index < transfers.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransferRow(
    transfer: TransferSuggestion,
    memberProfiles: Map<String, Profile>
) {
    val fromName = resolveName(transfer.fromUserId, transfer.fromName, memberProfiles)
    val toName = resolveName(transfer.toUserId, transfer.toName, memberProfiles)
    val fromAvatar = resolveAvatar(transfer.fromUserId, memberProfiles)
    val toAvatar = resolveAvatar(transfer.toUserId, memberProfiles)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            AsyncImage(
                model = fromAvatar,
                contentDescription = fromName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = fromName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArrowLine(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(
                text = "¥${fmtAmt(transfer.amount)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            AsyncImage(
                model = toAvatar,
                contentDescription = toName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = toName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ArrowLine(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.height(14.dp)) {
        val y = size.height / 2
        val arrowLen = 8.dp.toPx()
        val arrowHalf = 4.dp.toPx()
        val lineEnd = size.width - arrowLen

        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(lineEnd, y),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        val path = Path().apply {
            moveTo(size.width, y)
            lineTo(lineEnd, y - arrowHalf)
            lineTo(lineEnd, y + arrowHalf)
            close()
        }
        drawPath(path, color, style = Fill)
    }
}

private fun resolveName(userId: String, transferName: String?, profiles: Map<String, Profile>): String =
    transferName?.takeIf { it.isNotBlank() }
        ?: profiles[userId]?.username?.takeIf { it.isNotBlank() }
        ?: userId.take(6)

private fun resolveAvatar(userId: String, profiles: Map<String, Profile>): String =
    profiles[userId]?.avatarUrl?.takeIf { it.isNotBlank() } ?: AppConfig.DEFAULT_AVATAR

private fun fmtAmt(value: Double): String {
    val r = kotlin.math.round(value * 100.0) / 100.0
    val v = if (r == -0.0) 0.0 else r
    val s = v.toString()
    val d = s.indexOf('.')
    return when {
        d < 0 -> "$s.00"
        s.length - d - 1 == 1 -> "${s}0"
        else -> s
    }
}
