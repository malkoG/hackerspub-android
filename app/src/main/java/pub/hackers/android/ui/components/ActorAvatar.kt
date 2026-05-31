package pub.hackers.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pub.hackers.android.domain.model.Actor
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors

@Composable
fun ActorAvatar(
    actor: Actor,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    overlayActor: Actor? = null,
    overlaySize: Dp = AppShapes.avatarRepost,
) {
    Box(
        modifier = modifier
            .size(size)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
    ) {
        AvatarImage(
            actor = actor,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            initialFontSize = 16.sp,
        )

        if (overlayActor != null) {
            AvatarOverlay(
                actor = overlayActor,
                size = overlaySize,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 5.dp, y = 5.dp),
            )
        }
    }
}

@Composable
private fun AvatarOverlay(
    actor: Actor,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Box(
        modifier = modifier
            .size(size + 6.dp)
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.background, CircleShape)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        AvatarImage(
            actor = actor,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            initialFontSize = 9.sp,
        )
    }
}

@Composable
private fun AvatarImage(
    actor: Actor,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    initialFontSize: TextUnit = 14.sp,
) {
    val colors = LocalAppColors.current
    val avatarModifier = modifier
        .clip(CircleShape)
        .background(fallbackColorFor(actor.handle))
        .border(1.dp, colors.buttonOutline, CircleShape)

    Box(
        modifier = avatarModifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = fallbackInitial(actor),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = initialFontSize,
        )
        AsyncImage(
            model = actor.avatarUrl.ifBlank { null },
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

private val FallbackAvatarColors = listOf(
    Color(0xFF64748B),
    Color(0xFF0F766E),
    Color(0xFF2563EB),
    Color(0xFF7C3AED),
    Color(0xFFC2410C),
    Color(0xFFBE123C),
)

private fun fallbackColorFor(seed: String): Color {
    val index = seed.fold(0) { acc, char -> acc + char.code }
        .floorMod(FallbackAvatarColors.size)
    return FallbackAvatarColors[index]
}

private fun fallbackInitial(actor: Actor): String {
    val source = actor.name?.takeIf { it.isNotBlank() } ?: actor.handle
    return source.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
