package com.example.catslist.presentation.navigation

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
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
 * Peer top-level destinations switched from the floating bottom bar, each with its own back
 * stack so a tab switch preserves that screen's scroll position and ViewModel. Mirrors
 * Navigation 3's `MultipleBackStackSample`.
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingBottomBar(selected = selected, onSelect = { selected = it })
        },
    ) { innerPadding ->
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

        NavDisplay(
            entries = if (selected == CatsListNavKey) catsListEntries else favoriteCatsEntries,
            onBack = {
                val backStack = if (selected == CatsListNavKey) catsListBackStack else favoriteCatsBackStack
                backStack.removeLastOrNull()
            },
        )
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

private val BAR_SHAPE = RoundedCornerShape(50)

/** The bottom bar's destinations, in bar order. */
private val TOP_LEVEL_KEYS = listOf(CatsListNavKey, FavoriteCatsNavKey)

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

/** The M3 active indicator behind it is a fixed 64x32.dp, so much past this looks cramped. */
private val ICON_SIZE = 38.dp
