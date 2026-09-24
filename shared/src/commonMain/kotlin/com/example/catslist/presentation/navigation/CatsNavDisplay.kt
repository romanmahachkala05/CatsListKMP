package com.example.catslist.presentation.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.catslist.CatsListNavKey
import com.example.catslist.presentation.catslist.CatsListScreen
import com.example.catslist.presentation.favoritecats.FavoriteCatsNavKey
import com.example.catslist.presentation.favoritecats.FavoriteCatsScreen
import com.example.catslist.presentation.load
import com.example.catslist.shared.resources.Res
import com.example.catslist.shared.resources.catslist_nav_label
import com.example.catslist.shared.resources.favoritecats_nav_label
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.stringResource

/**
 * Peer top-level destinations switched from a floating bar, each with its own back stack so a
 * tab switch preserves that screen's scroll position and ViewModel. Mirrors Navigation 3's
 * `MultipleBackStackSample`.
 *
 * The bar follows the window: a pill along the bottom while the window is narrow, a rail down
 * the side from [RAIL_MIN_WIDTH] on — Material's compact/medium breakpoint — where a bottom
 * bar would sit under a grid of cards several columns wide.
 */
@Composable
fun CatsNavDisplay(notifier: SnackbarNotifier, modifier: Modifier = Modifier) {
    // Saveable, not just remembered: a rotation recreates the Activity, and a plain `remember`
    // came back on the first tab whichever one was open.
    var selected by rememberSaveable(stateSaver = TopLevelSaver) { mutableStateOf<NavKey>(CatsListNavKey) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notifier) {
        notifier.messages.collect { message ->
            snackbarHostState.showSnackbar(message.load())
        }
    }

    BoxWithConstraints(modifier = modifier) {
        CatsScaffold(
            selected = selected,
            onSelect = { selected = it },
            useRail = maxWidth >= RAIL_MIN_WIDTH,
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun CatsScaffold(
    selected: NavKey,
    onSelect: (NavKey) -> Unit,
    useRail: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!useRail) FloatingBottomBar(selected = selected, onSelect = onSelect)
        },
    ) { scaffoldPadding ->
        // The rail floats over the content as the bar does, so the lists make room for it at
        // the start the way they make room for the bar at the bottom.
        val layoutDirection = LocalLayoutDirection.current
        val innerPadding = if (useRail) {
            PaddingValues(
                start = scaffoldPadding.calculateStartPadding(layoutDirection) + RAIL_SPACE,
                top = scaffoldPadding.calculateTopPadding(),
                end = scaffoldPadding.calculateEndPadding(layoutDirection),
                bottom = scaffoldPadding.calculateBottomPadding(),
            )
        } else {
            scaffoldPadding
        }
        // The content is not padded: the bar floats over it and each list takes the insets as
        // contentPadding instead. Both tabs' entries are remembered on every recomposition so
        // their decorators stay alive; NavDisplay renders only the selected tab's.
        val catsListBackStack = rememberNavBackStack(NAV_KEYS, CatsListNavKey)
        val catsListEntries = rememberDecoratedNavEntries(
            backStack = catsListBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<CatsListNavKey> { CatsListScreen(contentPadding = innerPadding) }
            },
        )

        val favoriteCatsBackStack = rememberNavBackStack(NAV_KEYS, FavoriteCatsNavKey)
        val favoriteCatsEntries = rememberDecoratedNavEntries(
            backStack = favoriteCatsBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<FavoriteCatsNavKey> { FavoriteCatsScreen(contentPadding = innerPadding) }
            },
        )

        Box(modifier = Modifier.fillMaxSize()) {
            TabDisplay(
                entries = if (selected == CatsListNavKey) catsListEntries else favoriteCatsEntries,
                onBack = {
                    val backStack = if (selected == CatsListNavKey) catsListBackStack else favoriteCatsBackStack
                    backStack.removeLastOrNull()
                },
            )
            if (useRail) {
                FloatingNavRail(
                    selected = selected,
                    onSelect = onSelect,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = scaffoldPadding.calculateStartPadding(layoutDirection) + RAIL_MARGIN),
                )
            }
        }
    }
}

/**
 * Material3's [NavigationBar] shaped into a floating pill. `Modifier.shadow` clips to the shape,
 * so no wrapping Surface is needed, and `IntrinsicSize.Min` keeps the bar hugging its two items.
 */
@Composable
private fun FloatingBottomBar(
    selected: NavKey?,
    onSelect: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        // The bottomBar slot lays out from the start edge, so center the pill within it.
        modifier = modifier
            .testTag(NAV_BAR_TAG)
            .fillMaxWidth()
            .padding(32.dp)
            .wrapContentWidth()
            .width(IntrinsicSize.Min)
            .shadow(6.dp, BAR_SHAPE),
        // The Scaffold slot already handles system bars; these would double them.
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        NavigationBarItem(
            selected = selected == CatsListNavKey,
            onClick = { onSelect(CatsListNavKey) },
            // The label names the destination; a description here would announce it twice.
            icon = {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
            },
            label = { Text(stringResource(Res.string.catslist_nav_label), fontWeight = FontWeight.Normal) },
            colors = navigationBarItemColors(),
        )
        NavigationBarItem(
            selected = selected == FavoriteCatsNavKey,
            onClick = { onSelect(FavoriteCatsNavKey) },
            icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(ICON_SIZE)) },
            label = { Text(stringResource(Res.string.favoritecats_nav_label), fontWeight = FontWeight.Normal) },
            colors = navigationBarItemColors(),
        )
    }
}

/**
 * The bottom bar's pill turned on its side: the same two destinations, labels and colors,
 * floating at the start edge and hugging its items rather than running the full height.
 */
@Composable
private fun FloatingNavRail(
    selected: NavKey?,
    onSelect: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier
            .testTag(NAV_RAIL_TAG)
            .height(IntrinsicSize.Min)
            .shadow(6.dp, BAR_SHAPE),
        // The Scaffold's padding already clears the system bars and the display cutout.
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NavigationRailItem(
                selected = selected == CatsListNavKey,
                onClick = { onSelect(CatsListNavKey) },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
                },
                label = { Text(stringResource(Res.string.catslist_nav_label), fontWeight = FontWeight.Normal) },
                colors = navigationRailItemColors(),
            )
            NavigationRailItem(
                selected = selected == FavoriteCatsNavKey,
                onClick = { onSelect(FavoriteCatsNavKey) },
                icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(ICON_SIZE)) },
                label = { Text(stringResource(Res.string.favoritecats_nav_label), fontWeight = FontWeight.Normal) },
                colors = navigationRailItemColors(),
            )
        }
    }
}

/**
 * Each tab's stack holds one screen, so every change this animates is a tab switch, not a push.
 * It crossfades as Android and desktop do by default, stated so iOS does too: its default is a
 * push's slide from the right.
 */
@Composable
private fun TabDisplay(entries: List<NavEntry<NavKey>>, onBack: () -> Unit) {
    NavDisplay(
        entries = entries,
        onBack = onBack,
        transitionSpec = { tabSwitch() },
        popTransitionSpec = { tabSwitch() },
    )
}

private fun tabSwitch(): ContentTransform =
    fadeIn(tween(TAB_SWITCH_MILLIS)) togetherWith fadeOut(tween(TAB_SWITCH_MILLIS))

private const val TAB_SWITCH_MILLIS = 700

private val BAR_SHAPE = RoundedCornerShape(50)

/** Material's compact/medium width breakpoint: from here on the bar becomes a rail. */
private val RAIL_MIN_WIDTH = 600.dp

/** The margin either side of the rail, and the room the lists leave for it: Material's 80dp rail plus both. */
private val RAIL_MARGIN = 16.dp
private val RAIL_SPACE = 80.dp + RAIL_MARGIN * 2

/** For tests: which of the two bars is showing. */
internal const val NAV_BAR_TAG = "navBar"
internal const val NAV_RAIL_TAG = "navRail"

/** The bottom bar's destinations, in bar order. */
private val TOP_LEVEL_KEYS = listOf<NavKey>(CatsListNavKey, FavoriteCatsNavKey)

/** Saves the selected tab as its position in [TOP_LEVEL_KEYS] — an Int every platform can save. */
private val TopLevelSaver = Saver<NavKey, Int>(
    save = { TOP_LEVEL_KEYS.indexOf(it) },
    restore = { TOP_LEVEL_KEYS[it] },
)

/**
 * Every [NavKey] the back stacks hold, registered for saving. On Android the back stack falls
 * back to reflection to find a key's serializer; off Android there is no such fallback, and an
 * unregistered key fails the first time the stack is saved — so each one is listed here.
 */
private val NAV_KEYS = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(CatsListNavKey::class, CatsListNavKey.serializer())
            subclass(FavoriteCatsNavKey::class, FavoriteCatsNavKey.serializer())
        }
    }
}

/** Labels keep the same on-surface color in both states; only the icon reflects selection. */
@Composable
private fun navigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
)

/** As [navigationBarItemColors], for the rail. */
@Composable
private fun navigationRailItemColors() = NavigationRailItemDefaults.colors(
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
)

/** The M3 active indicator behind it is a fixed 64x32.dp, so much past this looks cramped. */
private val ICON_SIZE = 38.dp
