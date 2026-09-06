package com.example.tsuki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


data class TSukiNavTabItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun TSukiPillNavBar(
    items: List<TSukiNavTabItem>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .wrapContentWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                TSukiPillTab(
                    item = tab,
                    modifier = Modifier.animateContentSize(
                        animationSpec = tween(durationMillis = 220, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    )
                )
            }
        }
    }
}

@Composable
private fun TSukiPillTab(
    item: TSukiNavTabItem,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isSelected = item.selected

    val tabWidth by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isSelected) 64.dp else 44.dp,
        animationSpec = M3MotionTokens.expressiveBouncy(),
        label = "TabWidth"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "TabContainerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "TabContentColor"
    )

    Box(
        modifier = modifier
            .height(44.dp)
            .width(tabWidth)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = contentColor),
                onClick = item.onClick
            )
            .semantics {
                this.selected = isSelected
                this.role = Role.Tab
                this.contentDescription = item.label
            },
        contentAlignment = Alignment.Center
    ) {
        val iconScale = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(1f) }
        androidx.compose.runtime.LaunchedEffect(isSelected) {
            if (isSelected) {
                iconScale.animateTo(0.72f, M3MotionTokens.spatialFast())
                iconScale.animateTo(1.15f, M3MotionTokens.expressiveBouncy())
                iconScale.animateTo(1.0f, M3MotionTokens.spatialDefault())
            } else {
                iconScale.snapTo(1f)
            }
        }
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                }
        )
    }
}
