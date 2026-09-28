package io.github.halilozel1903.swipecards.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.github.halilozel1903.swipecards.core.SwipeDirection
import io.github.halilozel1903.swipecards.rememberSwipeCardState

/**
 * Swipes can't be performed reliably through adb, so `scripts/screenshots.sh` starts the app with
 * `--es scene <scene>` to set up each screenshot:
 *
 * - `deck`: the untouched deck (same as no extra)
 * - `dragging`: the top card frozen mid-drag to the right with the LIKE label showing
 * - `liked`: the stack after two destinations were liked
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra(EXTRA_SCENE)
        setContent {
            val colors = if (isSystemInDarkTheme()) {
                darkColorScheme(background = Color(0xFF0F1115), surface = Color(0xFF1A1D23))
            } else {
                lightColorScheme(background = Color(0xFFF7F4EF), surface = Color.White)
            }
            MaterialTheme(colorScheme = colors) {
                SampleApp(scene)
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}

@Composable
private fun SampleApp(scene: String?) {
    val state = rememberSwipeCardState()
    // Apply the scene once, not again after a configuration change.
    var sceneApplied by rememberSaveable { mutableStateOf(false) }
    if (!sceneApplied) {
        LaunchedEffect(scene) {
            when (scene) {
                "dragging" -> state.peek(SwipeDirection.Right, progress = 1.3f, animate = false)
                "liked" -> {
                    state.swipe(SwipeDirection.Right)
                    state.swipe(SwipeDirection.Right)
                }
            }
            sceneApplied = true
        }
    }
    DiscoverScreen(state)
}
