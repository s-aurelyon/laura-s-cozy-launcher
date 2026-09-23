package app.cozy.launcher.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.cozy.launcher.data.Store
import app.cozy.launcher.data.Tile
import app.cozy.launcher.data.newId
import app.cozy.launcher.system.AppInfo
import app.cozy.launcher.system.Apps
import app.cozy.launcher.ui.CozyDialog
import app.cozy.launcher.ui.CozyField
import app.cozy.launcher.ui.Header
import app.cozy.launcher.ui.Navigator
import app.cozy.launcher.ui.Page
import app.cozy.launcher.ui.Pill
import app.cozy.launcher.ui.PillStyle
import app.cozy.launcher.ui.theme.LocalPalette
import app.cozy.launcher.ui.theme.T
import app.cozy.launcher.ui.theme.Txt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(nav: Navigator) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var search by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    val apps = remember { Apps.list(ctx, refresh = true) }
    val shown = apps.filter { search.isBlank() || it.label.contains(search, ignoreCase = true) }

    Page {
        Column(Modifier.padding(horizontal = 40.dp, vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Header("All apps", { nav.back() })
            CozyField(search, { search = it }, "Search apps", Modifier.fillMaxWidth(), leadingIcon = "search")
            Txt("Hold an app to add it to your home screen.", T.body(16, 600), color = p.muted)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(130.dp),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 40.dp),
            ) {
                items(shown, key = { it.pkg }) { app ->
                    Column(
                        Modifier.clip(RoundedCornerShape(22.dp))
                            .combinedClickable(
                                onClick = { Apps.launch(ctx, app.pkg) },
                                onLongClick = { menuFor = app },
                            )
                            .padding(vertical = 14.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AppIcon(app.pkg, 60)
                        Txt(app.label, T.body(15, 600), maxLines = 2, align = TextAlign.Center)
                    }
                }
            }
        }
    }

    menuFor?.let { app ->
        CozyDialog({ menuFor = null }, app.label) {
            Pill("Add to home screen", {
                Store.updateSettings { it.copy(tiles = it.tiles + Tile(newId(), app.label, "star", pkg = app.pkg)) }
                menuFor = null
            }, Modifier.fillMaxWidth(), style = PillStyle.DARK, icon = "plus")
            Pill("App info", {
                Apps.openAppInfo(ctx, app.pkg)
                menuFor = null
            }, Modifier.fillMaxWidth(), style = PillStyle.LIGHT)
            Pill("Cancel", { menuFor = null }, Modifier.fillMaxWidth(), style = PillStyle.SOFT)
        }
    }
}
