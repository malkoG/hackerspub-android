package pub.hackers.android.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import pub.hackers.android.domain.model.PostLink
import pub.hackers.android.ui.theme.AppShapes
import pub.hackers.android.ui.theme.LocalAppColors
import pub.hackers.android.ui.theme.LocalAppTypography
import java.net.URI

@Composable
fun LinkPreviewCard(
    link: PostLink,
    modifier: Modifier = Modifier,
    onProfileClick: ((String) -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val context = LocalContext.current
    val domain = try {
        URI(link.url).host?.removePrefix("www.") ?: link.url
    } catch (_: Exception) {
        link.url
    }

    val isWideImage = link.image?.let { img ->
        val w = img.width ?: 0
        val h = img.height ?: 1
        w > 0 && h > 0 && w.toFloat() / h > 1.5f
    } ?: false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = colors.divider,
                shape = RoundedCornerShape(AppShapes.mediaRadius)
            )
            .clip(RoundedCornerShape(AppShapes.mediaRadius))
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                context.startActivity(intent)
            }
    ) {
        if (link.image != null && isWideImage) {
            // Wide image: full width on top
            AsyncImage(
                model = link.image.url,
                contentDescription = link.image.alt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = AppShapes.mediaRadius, topEnd = AppShapes.mediaRadius)),
                contentScale = ContentScale.Crop
            )
        }

        if (link.image != null && !isWideImage) {
            // Compact: image left, content right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = link.image.url,
                    contentDescription = link.image.alt,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                LinkTextContent(
                    link = link,
                    domain = domain,
                    typography = typography,
                    colors = colors,
                    onProfileClick = onProfileClick
                )
            }
        } else {
            // No image or wide image: text below
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                LinkTextContent(
                    link = link,
                    domain = domain,
                    typography = typography,
                    colors = colors,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun LinkTextContent(
    link: PostLink,
    domain: String,
    typography: pub.hackers.android.ui.theme.AppTextStyles,
    colors: pub.hackers.android.ui.theme.AppColorScheme,
    onProfileClick: ((String) -> Unit)?
) {
    Column {
        if (!link.title.isNullOrBlank()) {
            Text(
                text = link.title,
                style = typography.bodyLargeSemiBold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!link.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            val descriptionText = if (!link.author.isNullOrBlank() && !link.author.startsWith("http")) {
                "${link.author} — ${link.description}"
            } else {
                link.description
            }
            Text(
                text = descriptionText,
                style = typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = domain,
                style = typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!link.siteName.isNullOrBlank()) {
                Text(
                    text = " · ${link.siteName}",
                    style = typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }

        if (link.creator != null && onProfileClick != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onProfileClick(link.creator.handle) }
            ) {
                AsyncImage(
                    model = link.creator.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(4.dp))
                RichDisplayName(
                    name = link.creator.name,
                    fallback = link.creator.handle,
                    style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textSecondary,
                    emojiHeight = 14.dp
                )
            }
        }
    }
}
