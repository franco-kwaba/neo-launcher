package dev.neolauncher.ui

import android.Manifest
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neolauncher.LauncherUiState
import dev.neolauncher.LauncherViewModel
import dev.neolauncher.data.AppEntry
import dev.neolauncher.data.AppSlot
import dev.neolauncher.data.BatteryState
import dev.neolauncher.data.HomeLayout
import dev.neolauncher.data.MediaInfo
import dev.neolauncher.data.Weather
import dev.neolauncher.ui.neu.NeuButton
import dev.neolauncher.ui.neu.NeuSearchField
import dev.neolauncher.ui.neu.NeuSurface
import dev.neolauncher.ui.neu.neuGlow
import dev.neolauncher.ui.settings.SettingsPage
import dev.neolauncher.ui.shade.Shade
import dev.neolauncher.ui.theme.LocalNeu
import dev.neolauncher.ui.theme.LocalNeuStyle
import dev.neolauncher.ui.theme.textGlow
import dev.neolauncher.ui.theme.NeoTheme
import dev.neolauncher.ui.theme.isDarkTheme
import dev.neolauncher.ui.widgets.BatteryWidget
import dev.neolauncher.ui.widgets.MediaWidget
import dev.neolauncher.ui.widgets.WeatherWidget
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val HomeColumns = 4
private val HomeCellHeight = 100.dp

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshWeather()
    }
    val dark = isDarkTheme(state.settings.themeMode)
    val activity = LocalContext.current as? ComponentActivity
    // Ícones da barra de status acompanham o tema escolhido, não só o do sistema
    LaunchedEffect(dark) {
        val transparent = android.graphics.Color.TRANSPARENT
        activity?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
        )
    }
    NeoTheme(settings = state.settings, darkTheme = dark) {
        LauncherContent(
            state = state,
            actions = viewModel,
            onRequestLocation = { locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        )
    }
}

/** Sem dependência de ViewModel, para dar para usar no @Preview. */
@Composable
fun LauncherContent(
    state: LauncherUiState,
    actions: LauncherActions,
    onRequestLocation: () -> Unit = {},
) {
    val p = LocalNeu.current
    val drag = remember { DragState() }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    // Ao arrastar um app da gaveta, ela fica transparente (mas continua montada, senão o gesto seria cancelado)
    var drawerFading by remember { mutableStateOf(false) }
    val drawerAlpha by animateFloatAsState(if (drawerFading) 0f else 1f, label = "drawerAlpha")
    var shadeOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var menuKey by rememberSaveable { mutableStateOf<String?>(null) }
    val menuApp = menuKey?.let { key -> state.apps.find { it.key == key } }
    val showMenu: (AppEntry) -> Unit = { menuKey = it.key }
    // Mantém o último app durante a animação de saída do menu
    val lastMenuApp = remember { mutableStateOf<AppEntry?>(null) }
    if (menuApp != null) lastMenuApp.value = menuApp

    // Launcher nunca "sai" no voltar nem no Home: só fecha o que estiver aberto
    BackHandler {
        when {
            menuKey != null -> menuKey = null
            settingsOpen -> settingsOpen = false
            else -> {
                drawerOpen = false
                shadeOpen = false
            }
        }
    }
    LaunchedEffect(state.homeTick) {
        drawerOpen = false
        shadeOpen = false
        settingsOpen = false
        menuKey = null
    }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) drawerFading = false
    }
    LaunchedEffect(shadeOpen) {
        if (shadeOpen) actions.refreshQuickState()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.background),
    ) {
        HomePage(
            state = state,
            actions = actions,
            drag = drag,
            onOpenDrawer = { drawerOpen = true },
            onOpenShade = { shadeOpen = true },
            onOpenSettings = { settingsOpen = true },
            onShowMenu = showMenu,
            onRequestLocation = onRequestLocation,
        )
        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            AppDrawer(
                apps = state.apps,
                drag = drag,
                modifier = Modifier.graphicsLayer { alpha = drawerAlpha },
                onLaunch = {
                    drawerOpen = false
                    actions.launch(it)
                },
                onShowMenu = showMenu,
                onDragStarted = { drawerFading = true },
                onDrop = { app, target ->
                    if (target != null) actions.moveApp(app, target)
                    drawerOpen = false
                },
            )
        }
        AnimatedVisibility(
            visible = shadeOpen,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            Shade(
                state = state,
                actions = actions,
                onClose = { shadeOpen = false },
                onOpenLauncherSettings = { settingsOpen = true },
            )
        }
        AnimatedVisibility(
            visible = settingsOpen,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
        ) {
            SettingsPage(settings = state.settings, actions = actions, onClose = { settingsOpen = false })
        }
        AnimatedVisibility(visible = menuApp != null, enter = fadeIn(), exit = fadeOut()) {
            lastMenuApp.value?.let { app ->
                AppMenu(
                    app = app,
                    layout = state.layout,
                    homeFull = state.layout.firstFreeCell(state.homeCellCount) == null,
                    actions = actions,
                    onDismiss = { menuKey = null },
                )
            }
        }
        RemoveZone(drag)
        DragGhost(drag)
    }
}

/** Área "Remover" no topo, visível ao arrastar um app da tela inicial ou da barra. */
@Composable
private fun RemoveZone(drag: DragState) {
    val p = LocalNeu.current
    AnimatedVisibility(
        visible = drag.app != null && drag.source !is AppSlot.Drawer,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            val hovered = drag.hovered == AppSlot.Remove
            NeuSurface(
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp)
                    .dropTarget(drag, AppSlot.Remove),
                cornerRadius = 30.dp,
                elevation = 8.dp,
                pressed = hovered,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = if (hovered) p.accent else p.content)
                    Spacer(Modifier.width(10.dp))
                    Text("Remover", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (hovered) p.accent else p.content)
                }
            }
        }
    }
}

@Composable
private fun HomePage(
    state: LauncherUiState,
    actions: LauncherActions,
    drag: DragState,
    onOpenDrawer: () -> Unit,
    onOpenShade: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowMenu: (AppEntry) -> Unit,
    onRequestLocation: () -> Unit,
) {
    val openDrawer by rememberUpdatedState(onOpenDrawer)
    val openShade by rememberUpdatedState(onOpenShade)
    val openSettings by rememberUpdatedState(onOpenSettings)
    val byKey = remember(state.apps) { state.apps.associateBy { it.key } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            // Deslizar para cima abre a gaveta; para baixo, o painel
            .pointerInput(Unit) {
                val threshold = 64.dp.toPx()
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -threshold -> openDrawer()
                            total > threshold -> openShade()
                        }
                    },
                    onVerticalDrag = { change, amount ->
                        total += amount
                        change.consume()
                    },
                )
            }
            // Toque longo em área vazia abre Personalizar
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { if (!drag.touchingIcon) openSettings() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        ClockCard()
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BatteryWidget(
                battery = state.battery,
                onClick = actions::openBatterySettings,
                modifier = Modifier
                    .weight(1f)
                    .height(124.dp),
            )
            WeatherWidget(
                weather = state.weather,
                needsLocation = state.needsLocation,
                onClick = { if (state.needsLocation) onRequestLocation() else actions.refreshWeather() },
                modifier = Modifier
                    .weight(1f)
                    .height(124.dp),
            )
        }
        state.media?.let { media ->
            Spacer(Modifier.height(28.dp))
            MediaWidget(
                media = media,
                onPlayPause = actions::mediaPlayPause,
                onNext = actions::mediaNext,
                onPrevious = actions::mediaPrevious,
            )
        }
        HomeGrid(
            // Apps desinstalados somem sozinhos
            apps = state.layout.home.mapNotNull { (cell, key) -> byKey[key]?.let { cell to it } }.toMap(),
            drag = drag,
            actions = actions,
            onShowMenu = onShowMenu,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 24.dp),
        )
        Dock(state.layout.dock.mapNotNull { byKey[it] }, drag, actions, onOpenDrawer, onShowMenu)
    }
}

/** Grade livre da tela inicial: quantas linhas couberem no espaço que sobra. */
@Composable
private fun HomeGrid(
    apps: Map<Int, AppEntry>,
    drag: DragState,
    actions: LauncherActions,
    onShowMenu: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val rows = (maxHeight / HomeCellHeight).toInt().coerceAtLeast(0)
        val cellCount = rows * HomeColumns
        LaunchedEffect(cellCount) { actions.setHomeCellCount(cellCount) }
        Column {
            repeat(rows) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(HomeCellHeight),
                ) {
                    repeat(HomeColumns) { col ->
                        val cell = row * HomeColumns + col
                        HomeCell(
                            cell = cell,
                            app = apps[cell],
                            drag = drag,
                            actions = actions,
                            onShowMenu = onShowMenu,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeCell(
    cell: Int,
    app: AppEntry?,
    drag: DragState,
    actions: LauncherActions,
    onShowMenu: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slot = AppSlot.Home(cell)
    val hovered = drag.hovered == slot
    // O ícone sendo arrastado continua montado (dono do gesto), só fica invisível
    val lifted = app != null && drag.app?.key == app.key
    Box(modifier.dropTarget(drag, slot), contentAlignment = Alignment.TopCenter) {
        if (drag.app != null && (app == null || lifted)) {
            EmptySlot(hovered = hovered, modifier = Modifier.size(64.dp), cornerRadius = 20)
        }
        if (app != null) {
            DraggableAppTile(
                app = app,
                slot = slot,
                drag = drag,
                onTap = { actions.launch(app) },
                onMenu = { onShowMenu(app) },
                onDrop = { target -> if (target != null) actions.moveApp(app, target) },
                highlight = hovered && !lifted,
                modifier = Modifier.alpha(if (lifted) 0f else 1f),
            )
        }
    }
}

/** Encaixe vazio afundado; com um ponto na cor de destaque quando um app está sobre ele. */
@Composable
private fun EmptySlot(hovered: Boolean, modifier: Modifier = Modifier, cornerRadius: Int) {
    val p = LocalNeu.current
    NeuSurface(modifier, cornerRadius = cornerRadius.dp, elevation = if (hovered) 6.dp else 3.dp, pressed = true) {
        if (hovered) AccentDot()
    }
}

@Composable
private fun AccentDot() {
    Box(
        Modifier
            .size(10.dp)
            .neuGlow(cornerRadius = 5.dp, spread = 8.dp)
            .clip(CircleShape)
            .background(LocalNeu.current.accent),
    )
}

@Composable
private fun ClockCard() {
    val p = LocalNeu.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }
    NeuSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp),
        cornerRadius = 32.dp,
        elevation = 10.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = now.format(TimeFormat),
                fontSize = 60.sp,
                fontWeight = FontWeight.Light,
                color = p.content,
                // Os dígitos refletem a luz da cor de destaque
                style = TextStyle(shadow = LocalNeuStyle.current.textGlow(p.accent, 0.5f)),
            )
            Text(
                text = now.format(DateFormat).replaceFirstChar { it.uppercase() },
                fontSize = 16.sp,
                color = p.contentMuted,
            )
        }
    }
}

@Composable
private fun Dock(
    apps: List<AppEntry>,
    drag: DragState,
    actions: LauncherActions,
    onOpenDrawer: () -> Unit,
    onShowMenu: (AppEntry) -> Unit,
) {
    NeuSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        cornerRadius = 30.dp,
        pressed = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(LauncherViewModel.MAX_FAVORITES) { i ->
                val app = apps.getOrNull(i)
                val slot = AppSlot.Dock(i)
                val hovered = drag.hovered == slot
                val lifted = app != null && drag.app?.key == app.key
                Box(
                    Modifier
                        .size(60.dp)
                        .dropTarget(drag, slot),
                    contentAlignment = Alignment.Center,
                ) {
                    if (app == null || lifted) {
                        // Encaixe vazio: toque abre a gaveta para escolher um app
                        NeuButton(
                            onClick = onOpenDrawer,
                            active = true,
                            modifier = Modifier.size(56.dp),
                            cornerRadius = 28.dp,
                            elevation = if (hovered) 6.dp else 4.dp,
                        ) {
                            if (hovered) AccentDot()
                        }
                    }
                    if (app != null) {
                        DraggableAppTile(
                            app = app,
                            slot = slot,
                            drag = drag,
                            onTap = { actions.launch(app) },
                            onMenu = { onShowMenu(app) },
                            onDrop = { target -> if (target != null) actions.moveApp(app, target) },
                            size = 56.dp,
                            cornerRadius = 28.dp,
                            iconSize = 30.dp,
                            elevation = 5.dp,
                            showLabel = false,
                            highlight = hovered && !lifted,
                            modifier = Modifier.alpha(if (lifted) 0f else 1f),
                        )
                    }
                }
            }
            NeuButton(
                onClick = onOpenDrawer,
                modifier = Modifier.size(56.dp),
                cornerRadius = 28.dp,
                elevation = 5.dp,
            ) {
                DotsGridIcon()
            }
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<AppEntry>,
    drag: DragState,
    onLaunch: (AppEntry) -> Unit,
    onShowMenu: (AppEntry) -> Unit,
    onDragStarted: () -> Unit,
    onDrop: (AppEntry, AppSlot?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalNeu.current
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(p.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        NeuSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Buscar apps",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            // Espaço para as sombras não serem cortadas nas bordas da grade
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.key }) { app ->
                DraggableAppTile(
                    app = app,
                    slot = AppSlot.Drawer,
                    drag = drag,
                    onTap = { onLaunch(app) },
                    onMenu = { onShowMenu(app) },
                    onDragStarted = onDragStarted,
                    onDrop = { target -> onDrop(app, target) },
                )
            }
        }
    }
}

@Composable
private fun DotsGridIcon() {
    val color = LocalNeu.current.contentMuted
    Canvas(Modifier.size(22.dp)) {
        val step = size.width / 3
        val radius = step / 5
        for (row in 0..2) for (col in 0..2) {
            drawCircle(color, radius, Offset(step * col + step / 2, step * row + step / 2))
        }
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormat = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

@Preview(name = "Claro", widthDp = 360, heightDp = 760)
@Composable
private fun LauncherPreviewLight() {
    NeoTheme(darkTheme = false) { LauncherContent(previewState(), object : LauncherActions {}) }
}

@Preview(name = "Escuro", widthDp = 360, heightDp = 760)
@Composable
private fun LauncherPreviewDark() {
    NeoTheme(darkTheme = true) { LauncherContent(previewState(), object : LauncherActions {}) }
}

private fun previewState() = LauncherUiState(
    apps = listOf("Câmera", "Chrome", "WhatsApp", "Spotify").map {
        AppEntry(it, ComponentName("preview.$it", "Main"), ImageBitmap(48, 48))
    },
    layout = HomeLayout(
        dock = listOf("preview.Câmera/Main", "preview.Chrome/Main"),
        home = mapOf(0 to "preview.WhatsApp/Main", 5 to "preview.Spotify/Main"),
    ),
    battery = BatteryState(percent = 78, charging = false),
    weather = Weather(tempC = 24, code = 1, place = "São Paulo"),
    media = MediaInfo(title = "Música de exemplo", artist = "Artista", playing = true, art = null),
)
