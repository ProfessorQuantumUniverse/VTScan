// ui/intro/IntroScreen.kt
package com.quantum_prof.vtscansuite.ui.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quantum_prof.vtscansuite.ui.components.GradientButton
import com.quantum_prof.vtscansuite.ui.theme.CookieShape
import com.quantum_prof.vtscansuite.ui.theme.auroraBrush
import com.quantum_prof.vtscansuite.ui.util.HapticType
import com.quantum_prof.vtscansuite.ui.util.VT_API_KEY_URL
import com.quantum_prof.vtscansuite.ui.util.openUrl
import com.quantum_prof.vtscansuite.ui.util.triggerHaptic
import kotlinx.coroutines.delay

/**
 * Animierte Splash- / Onboarding-Ansicht. Spielt eine kurze Logo-Animation und zeigt
 * danach entweder den API-Key-Eingabebereich (wenn noch kein Key gespeichert ist) oder
 * einen "Enter"-Button (und fährt automatisch fort).
 */
@Composable
fun IntroScreen(
    apiKeySaved: Boolean,
    onSaveApiKey: (String) -> Unit,
    onContinue: () -> Unit
) {
    // Eintritts-Animationen
    val logoScale = remember { Animatable(0.3f) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        showContent = true
    }
    val titleAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )

    // Endlos-Animationen
    val infinite = rememberInfiniteTransition(label = "intro")
    val ring by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "ring"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.94f, targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "spin"
    )

    // Regulärer Start (Key vorhanden): reine Splash-Animation, kein Button – automatisch weiter.
    LaunchedEffect(showContent, apiKeySaved) {
        if (showContent && apiKeySaved) {
            delay(1400)
            onContinue()
        }
    }

    val ringColor = MaterialTheme.colorScheme.primary
    val bg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // ---- Logo mit Radar-Ringen ----
        Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val maxR = size.minDimension / 2f
                val count = 3
                for (i in 0 until count) {
                    val p = (ring + i.toFloat() / count) % 1f
                    drawCircle(
                        color = ringColor,
                        radius = maxR * (0.45f + p * 0.55f),
                        style = Stroke(width = 3.dp.toPx()),
                        alpha = (1f - p) * 0.45f
                    )
                }
            }
            // rotierende Cookie-Form mit Aurora-Verlauf
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        rotationZ = spin
                        scaleX = logoScale.value * glow
                        scaleY = logoScale.value * glow
                    }
                    .clip(CookieShape(scallops = 12, amplitude = 0.08f))
                    .background(auroraBrush())
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { scaleX = logoScale.value; scaleY = logoScale.value }
            )
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.graphicsLayer { alpha = titleAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Veto",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "The native, open source\nVirusTotal client.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.weight(1f))

        // ---- Unterer Bereich ----
        // Regulärer Start (Key vorhanden): kein Button, nur die Splash-Animation, die
        // automatisch weiterleitet. Erstes Setup (kein Key): volles Onboarding wie gehabt.
        if (!apiKeySaved) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + slideInVertically(tween(450)) { it / 3 }
            ) {
                ApiKeyOnboarding(onSaveApiKey = onSaveApiKey, onContinue = onContinue)
            }
        }
    }
}

@Composable
private fun ApiKeyOnboarding(
    onSaveApiKey: (String) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var key by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Add your free VirusTotal API key to start scanning. Create a free account, open your profile menu (top-right) and choose “API key”, then paste it below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(
            onClick = {
                triggerHaptic(view, HapticType.CLICK)
                openUrl(context, VT_API_KEY_URL)
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Get your API key", fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("VirusTotal API key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            // Wie ein Passwort behandeln: standardmäßig verdeckt und ohne
            // Autokorrektur, damit die Tastatur den Key nicht lernt oder vorschlägt.
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            trailingIcon = {
                Row {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key"
                        )
                    }
                    if (key.isNotEmpty()) {
                        IconButton(onClick = { key = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            }
        )
        GradientButton(
            text = "Get started",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            enabled = key.isNotBlank(),
            onClick = {
                onSaveApiKey(key.trim())
                onContinue()
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = onContinue) {
            Text("Skip for now")
        }
    }
}
