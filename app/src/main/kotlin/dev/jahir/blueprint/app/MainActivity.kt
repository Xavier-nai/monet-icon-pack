package dev.jahir.blueprint.app

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ScrollView
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.platform.ComposeView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.javiersantos.piracychecker.PiracyChecker
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.jahir.blueprint.app.glass.GlassBackdropState
import dev.jahir.blueprint.app.glass.GlassTab
import dev.jahir.blueprint.app.glass.LiquidGlassBottomBar
import dev.jahir.blueprint.ui.activities.BottomNavigationBlueprintActivity

/**
 * You can choose between:
 * - DrawerBlueprintActivity
 * - BottomNavigationBlueprintActivity
 */
class MainActivity : BottomNavigationBlueprintActivity() {

    /**
     * These things here have the default values. You can delete the ones you don't want to change
     * and/or modify the ones you want to.
     */
    override val billingEnabled = true

    override fun amazonInstallsEnabled(): Boolean = false
    override fun checkLPF(): Boolean = false
    override fun checkStores(): Boolean = false
    override val isDebug: Boolean = BuildConfig.DEBUG

    /**
     * This is your app's license key. Get yours on Google Play Dev Console.
     * Default one isn't valid and could cause issues in your app.
     */
    override fun getLicKey(): String? = "MIIBIjANBgkqhkiGgKglYGYGihLuihUuhhuBlouBkuiu"

    /**
     * This is the license checker code. Feel free to create your own implementation or
     * leave it as it is.
     * Anyways, keep the 'destroyChecker()' as the very first line of this code block
     * Return null to disable license check
     */
    override fun getLicenseChecker(): PiracyChecker? {
        destroyChecker() // Important
        // License check disabled: personal, sideloaded icon pack (no Google Play LVL key).
        return null
    }

    override fun defaultTheme(): Int = R.style.MyApp_Default
    override fun amoledTheme(): Int = R.style.MyApp_Default_Amoled

    override fun defaultMaterialYouTheme(): Int = R.style.MyApp_Default_MaterialYou
    override fun amoledMaterialYouTheme(): Int = R.style.MyApp_Default_Amoled_MaterialYou

    // ---------------------------------------------------------------------------------------------
    // Liquid Glass bottom bar (status bar is left to Blueprint's adaptive theming)
    // ---------------------------------------------------------------------------------------------

    private val backdropState = GlassBackdropState()
    private val selectedId = mutableIntStateOf(-1)
    private val handler = Handler(Looper.getMainLooper())

    private var rootView: ViewGroup? = null
    private var bottomNav: BottomNavigationView? = null
    private var fragmentsContainer: View? = null
    private var glassInstalled = false

    private var lastCaptureAt = 0L
    private val trailingCapture = Runnable { captureBackdrop() }

    // Live refraction: re-snapshot the (down-scaled) content behind the bar as the page
    // scrolls, throttled to ~20fps. Cheap now that the snapshot is rendered at 0.34x.
    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        val now = SystemClock.uptimeMillis()
        if (now - lastCaptureAt >= 48L) {
            lastCaptureAt = now
            captureBackdrop()
        }
        handler.removeCallbacks(trailingCapture)
        handler.postDelayed(trailingCapture, 90L)
    }

    private fun resId(name: String, type: String): Int =
        resources.getIdentifier(name, type, packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installLiquidGlass()
    }

    private fun installLiquidGlass() {
        if (glassInstalled) return
        val root = findViewById<View>(resId("activity_root_view", "id")) as? ViewGroup ?: return
        val nav = findViewById<View>(resId("bottom_navigation", "id")) as? BottomNavigationView ?: return
        val container = findViewById<View>(resId("fragments_container", "id")) ?: return
        rootView = root
        bottomNav = nav
        fragmentsContainer = container
        glassInstalled = true

        // Hide the stock bar but keep it functional: selecting an item still drives navigation.
        nav.visibility = View.INVISIBLE
        selectedId.intValue = nav.selectedItemId

        // Build tabs from the *actual* visible menu items Blueprint configured.
        val tabs = buildList {
            val menu = nav.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                if (!item.isVisible) continue
                val entry = runCatching { resources.getResourceEntryName(item.itemId) }.getOrNull()
                val iconRes = if (entry != null) resId("ic_$entry", "drawable") else 0
                add(
                    GlassTab(
                        menuId = item.itemId,
                        iconRes = if (iconRes != 0) iconRes else android.R.drawable.ic_menu_help,
                        label = item.title?.toString().orEmpty()
                    )
                )
            }
        }

        // Add the Compose liquid-glass bar pinned to the bottom, above everything else.
        val composeView = ComposeView(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }
            setContent {
                LiquidGlassBottomBar(
                    tabs = tabs,
                    selectedId = selectedId.intValue,
                    onSelect = { menuId -> onGlassTabSelected(menuId) },
                    backdropState = backdropState
                )
            }
        }
        root.addView(composeView)

        root.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        scheduleCaptures()
    }

    private fun onGlassTabSelected(menuId: Int) {
        val nav = bottomNav ?: return
        if (nav.selectedItemId != menuId) {
            nav.selectedItemId = menuId // triggers Blueprint's fragment swap
        }
        selectedId.intValue = menuId
        // Refresh the refraction after the fragment settles.
        scheduleCaptures()
    }

    private fun captureBackdrop() {
        fragmentsContainer?.let { backdropState.capture(it) }
    }

    private fun scheduleCaptures() {
        captureBackdrop()
        applyContentInset()
        for (delay in longArrayOf(120L, 320L, 600L)) {
            handler.postDelayed({
                applyContentInset()
                captureBackdrop()
            }, delay)
        }
    }

    /**
     * Give the scrolling content a bottom inset the height of the floating bar so the
     * list doesn't scroll under/around the capsule (Blueprint only reserves room for the
     * original full-width nav bar, which is shorter than our floating glass bar).
     */
    private fun applyContentInset() {
        val container = fragmentsContainer as? ViewGroup ?: return
        val inset = (resources.displayMetrics.density * 104f).toInt()
        fun walk(v: View) {
            val scrollable = v is RecyclerView || v is ScrollView || v is NestedScrollView
            if (scrollable && v.paddingBottom < inset) {
                (v as ViewGroup).clipToPadding = false
                v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, inset)
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(container)
    }

    override fun onResume() {
        super.onResume()
        // Keep the stock bar hidden and the selection mirror in sync.
        bottomNav?.let {
            it.visibility = View.INVISIBLE
            selectedId.intValue = it.selectedItemId
        }
        handler.post {
            applyContentInset()
            captureBackdrop()
        }
    }

    override fun onDestroy() {
        rootView?.viewTreeObserver?.removeOnScrollChangedListener(scrollListener)
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
