package dev.jahir.blueprint.app.glass

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

/** One tab, mapped 1:1 to a Blueprint bottom-navigation menu item id. */
data class GlassTab(val menuId: Int, val iconRes: Int, val label: String)

/**
 * App-facing wrapper: feeds a live snapshot of the content behind the bar
 * ([backdropState]) into the interactive [LiquidBottomTabs] so the iOS-26 liquid
 * glass refracts the real UI, and bridges tab taps back to Blueprint via [onSelect].
 */
@Composable
fun LiquidGlassBottomBar(
    tabs: List<GlassTab>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    backdropState: GlassBackdropState,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
) {
    if (tabs.isEmpty()) return

    val navBottom = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val dark = isSystemInDarkTheme()
    val contentColor = if (dark) Color.White else Color.Black
    val selectedContentColor = Color(0xFF3881FA)

    var barPos by remember { mutableStateOf(Offset.Zero) }

    // Stable providers — the component uses selectedTabIndex/onTabSelected as
    // remember()/LaunchedEffect keys, so they MUST be stable instances. Passing a
    // fresh lambda that captures the (unstable) tabs list each recomposition reset
    // the internal animation state every frame -> jumpy "ghosting" and the pill not
    // following the finger.
    val selectedIndex = tabs.indexOfFirst { it.menuId == selectedId }.coerceAtLeast(0)
    val selectedIndexState = rememberUpdatedState(selectedIndex)
    val tabsState = rememberUpdatedState(tabs)
    val onSelectState = rememberUpdatedState(onSelect)
    val selectedTabIndexProvider = remember { { selectedIndexState.value } }
    val onTabSelectedCb: (Int) -> Unit = remember {
        { idx -> tabsState.value.getOrNull(idx)?.let { onSelectState.value(it.menuId) } }
    }

    val backdrop = rememberCanvasBackdrop {
        val img = backdropState.image
        val fw = backdropState.fullWidth
        val fh = backdropState.fullHeight
        if (img != null && fw > 0 && fh > 0) {
            val tl = backdropState.contentTopLeftInWindow
            // Stretch the down-scaled snapshot back to 1:1 window space, offset so the
            // slice directly behind the bar lands under it.
            drawImage(
                image = img,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset(
                    (tl.x - barPos.x).roundToInt(),
                    (tl.y - barPos.y).roundToInt()
                ),
                dstSize = IntSize(fw, fh)
            )
        } else {
            drawRect(if (dark) Color(0xFF202124) else Color(0xFFF2F2F2))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = navBottom + 10.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidBottomTabs(
            selectedTabIndex = selectedTabIndexProvider,
            onTabSelected = onTabSelectedCb,
            backdrop = backdrop,
            tabsCount = tabs.size,
            onInteraction = onInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { barPos = it.positionInWindow() }
        ) { hiddenIndex, selectedContentIndex ->
            tabs.forEachIndexed { index, tab ->
                val selectedContent = selectedContentIndex == index
                val tabColor = if (selectedContent) selectedContentColor else contentColor
                LiquidBottomTab(
                    onClick = { onSelect(tab.menuId) },
                    visible = selectedContentIndex != null || hiddenIndex != index,
                    enabled = selectedContentIndex != null
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(tab.iconRes),
                        contentDescription = tab.label,
                        colorFilter = ColorFilter.tint(tabColor),
                        modifier = Modifier.size(24.dp)
                    )
                    BasicText(
                        text = tab.label,
                        style = TextStyle(color = tabColor, fontSize = 10.sp, textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
