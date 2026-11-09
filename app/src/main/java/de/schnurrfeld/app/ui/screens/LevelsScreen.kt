package de.schnurrfeld.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.theme.Gold

@Composable
fun LevelsScreen(app: App, onBack: () -> Unit, onPlay: (Int) -> Unit) {
    val progress = app.store.progress
    Column(Modifier.fillMaxSize()) {
        TopBar("Levels", onBack)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(62.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for ((size, range) in Levels.groups()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "h$size") {
                    Row(Modifier.padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                        Text("$size × $size", style = MaterialTheme.typography.titleMedium)
                        val done = range.count { it in progress.stars }
                        Text(
                            "  $done/${range.count()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(range.toList(), key = { it }) { lvl ->
                    LevelTile(lvl, locked = lvl > progress.unlocked, stars = progress.stars[lvl] ?: 0) { onPlay(lvl) }
                }
            }
        }
    }
}

@Composable
private fun LevelTile(level: Int, locked: Boolean, stars: Int, onClick: () -> Unit) {
    val solved = stars > 0
    val current = !locked && !solved
    Surface(
        onClick = onClick,
        enabled = !locked,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = when {
            current -> MaterialTheme.colorScheme.primary
            solved -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (solved) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (locked) {
                Icon(Icons.Rounded.Lock, "gesperrt", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            } else {
                Text("$level", style = MaterialTheme.typography.titleMedium)
                if (solved) Row {
                    repeat(3) { i ->
                        Icon(
                            Icons.Rounded.Star, null, Modifier.size(12.dp),
                            tint = if (i < stars) Gold else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
