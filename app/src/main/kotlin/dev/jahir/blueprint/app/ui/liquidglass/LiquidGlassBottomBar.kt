package dev.jahir.blueprint.app.ui.liquidglass

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import kotlin.math.roundToInt

@Composable
fun LiquidGlassBottomBar(
    tabs: List<LiquidGlassTab>,
    selectedRouteId: Int,
    onRouteSelected: (Int) -> Unit,
    backdropState: LiquidGlassBackdropState,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {}
) {
    if (tabs.isEmpty()) return

    val isDarkTheme = isSystemInDarkTheme()
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val selectedContentColor = if (isDarkTheme) Color(0xFF4DA3FF) else Color(0xFF007AFF)
    val fallbackBackdropColor = if (isDarkTheme) Color(0xFF121212) else Color.White
    val navBottom = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    var barPosition by remember { mutableStateOf(Offset.Zero) }
    val selectedIndex = tabs.indexOfFirst { it.routeId == selectedRouteId }.coerceAtLeast(0)
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val tabsState = rememberUpdatedState(tabs)
    val onRouteSelectedState = rememberUpdatedState(onRouteSelected)
    val selectedTabIndexProvider = remember { { selectedIndexState.value } }
    val onTabSelected: (Int) -> Unit = remember {
        { index ->
            tabsState.value.getOrNull(index)?.let { tab ->
                onRouteSelectedState.value(tab.routeId)
            }
        }
    }

    val backdrop = rememberCanvasBackdrop {
        drawRect(fallbackBackdropColor)
        val image = backdropState.image
        val fullWidth = backdropState.fullWidth
        val fullHeight = backdropState.fullHeight
        if (image != null && fullWidth > 0 && fullHeight > 0) {
            val contentTopLeft = backdropState.contentTopLeftInWindow
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(
                    (contentTopLeft.x - barPosition.x).roundToInt(),
                    (contentTopLeft.y - barPosition.y).roundToInt()
                ),
                dstSize = IntSize(fullWidth, fullHeight)
            )
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = navBottom + 14.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidBottomTabs(
            selectedTabIndex = selectedTabIndexProvider,
            onTabSelected = onTabSelected,
            backdrop = backdrop,
            tabsCount = tabs.size,
            onInteraction = onInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { barPosition = it.positionInWindow() }
        ) {
            tabs.forEachIndexed { index, tab ->
                val color = if (index == selectedIndex) selectedContentColor else contentColor
                LiquidBottomTab(
                    onClick = { onRouteSelected(tab.routeId) }
                ) {
                    Image(
                        painter = painterResource(tab.iconRes),
                        contentDescription = tab.label,
                        colorFilter = ColorFilter.tint(color),
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = tab.label,
                        style = TextStyle(
                            color = color,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
