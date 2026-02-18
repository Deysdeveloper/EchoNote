package com.Deysdeveloper.dailyvoicejournalapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.CardGradientDark
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.CardGradientLight
import com.Deysdeveloper.dailyvoicejournalapp.ui.theme.WarmTeal

@Composable
fun BeautifulDateHeader(
    text: String,
    modifier: Modifier = Modifier,
    isToday: Boolean = false
) {
    Box(
        modifier = modifier
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Pill-shaped container with gradient
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isToday) 4.dp else 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isToday) WarmTeal else MaterialTheme.colorScheme.primary
                )
                .background(
                    brush = if (isToday) {
                        CardGradientLight
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isToday) {
                    // Today indicator dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }

                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
