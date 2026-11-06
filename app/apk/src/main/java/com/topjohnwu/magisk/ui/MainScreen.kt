package com.topjohnwu.magisk.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventState
import com.topjohnwu.magisk.arch.VMFactory
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.ui.home.HomeScreen
import com.topjohnwu.magisk.ui.home.HomeViewModel
import com.topjohnwu.magisk.ui.install.InstallViewModel
import com.topjohnwu.magisk.ui.log.LogScreen
import com.topjohnwu.magisk.ui.log.LogViewModel
import com.topjohnwu.magisk.ui.module.ModuleScreen
import com.topjohnwu.magisk.ui.module.ModuleViewModel
import com.topjohnwu.magisk.ui.navigation.CollectNavEvents
import com.topjohnwu.magisk.ui.navigation.LocalNavigator
import com.topjohnwu.magisk.ui.navigation.Navigator
import com.topjohnwu.magisk.ui.navigation.Route
import com.topjohnwu.magisk.ui.settings.SettingsScreen
import com.topjohnwu.magisk.ui.settings.SettingsViewModel
import com.topjohnwu.magisk.ui.superuser.SuperuserScreen
import com.topjohnwu.magisk.ui.superuser.SuperuserViewModel
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.glass.glassBackdropSource
import io.github.neoruaa.glasmio.glass.render.backdrops.rememberLayerBackdrop
import io.github.neoruaa.glasmio.glass.trackBackdropPosition
import io.github.neoruaa.glasmio.icons.SfIcon
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.ui.GlasmioIconButton
import io.github.neoruaa.glasmio.ui.GlasmioNavigationBar
import io.github.neoruaa.glasmio.ui.GlasmioSidebar
import io.github.neoruaa.glasmio.ui.GlasmioSidebarRow
import io.github.neoruaa.glasmio.ui.GlasmioSidebarToolbar
import io.github.neoruaa.glasmio.ui.GlasmioTabItem
import io.github.neoruaa.glasmio.ui.NavigationBarSurfaceHeight
import kotlinx.coroutines.launch
import com.topjohnwu.magisk.core.R as CoreR

enum class Tab(val titleRes: Int, val symbol: SfSymbol) {
    MODULES(CoreR.string.modules, SfSymbol.Layers),
    SUPERUSER(CoreR.string.superuser, SfSymbol.PersonFilled),
    HOME(CoreR.string.section_home, SfSymbol.HouseFilled),
    LOG(CoreR.string.logs, SfSymbol.Clock),
    SETTINGS(CoreR.string.settings, SfSymbol.Gearshape);
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = Tab.HOME.ordinal,
    requestedTab: Int? = null,
    onTabRequestHandled: () -> Unit = {},
    superuserViewModel: SuperuserViewModel? = null,
) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val visibleTabs = remember {
        Tab.entries.filter { tab ->
            when (tab) {
                Tab.SUPERUSER -> Info.showSuperUser
                Tab.MODULES -> Info.env.isActive && LocalModule.loaded()
                else -> true
            }
        }
    }
    val initialPage = visibleTabs.indexOf(Tab.entries[initialTab]).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { visibleTabs.size })

    val currentOnTabRequestHandled by rememberUpdatedState(onTabRequestHandled)
    LaunchedEffect(requestedTab) {
        if (requestedTab == null) return@LaunchedEffect
        val page = visibleTabs.indexOf(Tab.entries[requestedTab])
        if (page >= 0) pagerState.scrollToPage(page)
        currentOnTabRequestHandled()
    }

    var moduleFabAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var superuserFabAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val currentTab = visibleTabs.getOrNull(pagerState.currentPage)
    val showFab = (currentTab == Tab.MODULES && moduleFabAction != null) ||
        (currentTab == Tab.SUPERUSER && superuserFabAction != null)

    val homePageIndex = remember(visibleTabs) { visibleTabs.indexOf(Tab.HOME).coerceAtLeast(0) }
    val isNotAtHome = pagerState.currentPage != homePageIndex && navigator.current() == Route.Main
    val tabBackState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = tabBackState,
        isBackEnabled = isNotAtHome,
        onBackCompleted = {
            scope.launch { pagerState.animateScrollToPage(homePageIndex) }
        }
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 840.dp
        val pageBackdrop = rememberLayerBackdrop()
        val navigationBarBottom =
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        if (isWide) {
            Row(Modifier.fillMaxSize()) {
                GlasmioSidebar(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp),
                    ) {
                        GlasmioSidebarToolbar(
                            leading = {
                                Text(
                                    text = "Magisk",
                                    style = IosTypography.headline,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            },
                        )
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 10.dp),
                        ) {
                            items(visibleTabs, key = { it.name }) { tab ->
                                GlasmioSidebarRow(
                                    title = stringResource(tab.titleRes),
                                    symbol = tab.symbol,
                                    selected = tab == currentTab,
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(visibleTabs.indexOf(tab)) }
                                    },
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .glassBackdropSource(pageBackdrop)
                        .trackBackdropPosition(pageBackdrop),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        MainPagerPage(
                            page = page,
                            visibleTabs = visibleTabs,
                            pagerState = pagerState,
                            navigator = navigator,
                            superuserViewModel = superuserViewModel,
                            onModuleFab = { moduleFabAction = it },
                            onSuperuserFab = { superuserFabAction = it },
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropSource(pageBackdrop)
                    .trackBackdropPosition(pageBackdrop),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = NavigationBarSurfaceHeight + navigationBarBottom + 8.dp),
                    beyondViewportPageCount = 0,
                ) { page ->
                    MainPagerPage(
                        page = page,
                        visibleTabs = visibleTabs,
                        pagerState = pagerState,
                        navigator = navigator,
                        superuserViewModel = superuserViewModel,
                        onModuleFab = { moduleFabAction = it },
                        onSuperuserFab = { superuserFabAction = it },
                    )
                }
            }
            GlasmioNavigationBar(
                items = visibleTabs.map { tab ->
                    GlasmioTabItem(
                        key = tab,
                        label = stringResource(tab.titleRes),
                        symbol = tab.symbol,
                        contentDescription = "",
                    )
                },
                selectedKey = currentTab ?: Tab.HOME,
                onSelected = { tab ->
                    scope.launch { pagerState.animateScrollToPage(visibleTabs.indexOf(tab)) }
                },
                onExpand = {},
                compactProgress = remember { mutableStateOf(0f) },
                compactSize = 48.dp,
                backdrop = pageBackdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
            )
        }

        if (showFab) {
            GlasmioIconButton(
                onClick = {
                    if (currentTab == Tab.MODULES) moduleFabAction?.invoke()
                    else if (currentTab == Tab.SUPERUSER) superuserFabAction?.invoke()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = if (isWide) 20.dp else 92.dp),
                size = 56.dp,
            ) {
                SfIcon(
                    "plus",
                    contentDescription = stringResource(
                        if (currentTab == Tab.MODULES) CoreR.string.module_action_install_external
                        else CoreR.string.superuser_grant
                    ),
                    size = 26.dp,
                )
            }
        }
    }
}

@Composable
private fun MainPagerPage(
    page: Int,
    visibleTabs: List<Tab>,
    pagerState: PagerState,
    navigator: Navigator,
    superuserViewModel: SuperuserViewModel?,
    onModuleFab: (((() -> Unit)?) -> Unit),
    onSuperuserFab: (((() -> Unit)?) -> Unit),
) {
    val isCurrentPage = pagerState.currentPage == page
    val childOwner = rememberNavigationEventDispatcherOwner(enabled = isCurrentPage)
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides childOwner) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties {
                    onEnter = {
                        if (!isCurrentPage) cancelFocusChange()
                    }
                    onExit = {
                        when (requestedFocusDirection) {
                            FocusDirection.Left,
                            FocusDirection.Right,
                            FocusDirection.Up,
                            -> cancelFocusChange()
                        }
                    }
                }
                .focusGroup(),
        ) {
            MainTabPage(
                tab = visibleTabs[page],
                isCurrentPage = isCurrentPage,
                navigator = navigator,
                superuserViewModel = superuserViewModel,
                onModuleFab = onModuleFab,
                onSuperuserFab = onSuperuserFab,
            )
        }
    }
}

@Composable
private fun MainTabPage(
    tab: Tab,
    isCurrentPage: Boolean,
    navigator: Navigator,
    superuserViewModel: SuperuserViewModel?,
    onModuleFab: (((() -> Unit)?) -> Unit),
    onSuperuserFab: (((() -> Unit)?) -> Unit),
) {
    when (tab) {
        Tab.HOME -> {
            val vm: HomeViewModel = viewModel(factory = VMFactory)
            val installVm: InstallViewModel = viewModel(factory = VMFactory)
            LaunchedEffect(isCurrentPage) {
                if (isCurrentPage) vm.startLoading()
            }
            CollectNavEvents(vm, navigator)
            CollectNavEvents(installVm, navigator)
            HomeScreen(vm, installVm)
        }
        Tab.SUPERUSER -> {
            val activity = LocalActivity.current as? ComponentActivity
            val vm: SuperuserViewModel = superuserViewModel
                ?: if (activity != null) {
                    viewModel(viewModelStoreOwner = activity, factory = VMFactory)
                } else {
                    viewModel(factory = VMFactory)
                }
            LaunchedEffect(isCurrentPage) {
                if (isCurrentPage) vm.startLoading()
            }
            CollectNavEvents(vm, navigator)
            SuperuserScreen(
                viewModel = vm,
                onRegisterFab = onSuperuserFab,
                isFabAtBottom = true,
            )
        }
        Tab.LOG -> {
            val vm: LogViewModel = viewModel(factory = VMFactory)
            LaunchedEffect(isCurrentPage) {
                if (isCurrentPage) vm.startLoading()
            }
            LogScreen(vm)
        }
        Tab.MODULES -> {
            val vm: ModuleViewModel = viewModel(factory = VMFactory)
            LaunchedEffect(isCurrentPage) {
                if (isCurrentPage) vm.startLoading()
            }
            CollectNavEvents(vm, navigator)
            ModuleScreen(
                viewModel = vm,
                onRegisterFab = onModuleFab,
                isFabAtBottom = true,
            )
        }
        Tab.SETTINGS -> {
            val vm: SettingsViewModel = viewModel(factory = VMFactory)
            CollectNavEvents(vm, navigator)
            SettingsScreen(vm)
        }
    }
}
