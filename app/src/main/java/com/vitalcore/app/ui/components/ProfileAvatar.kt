package com.vitalcore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import com.vitalcore.app.ui.theme.OnSurfaceMutedDark
import com.vitalcore.app.ui.theme.SurfaceDarkElevated
import java.io.File

/**
 * Circular profile photo: a locally-picked photo (file path) takes priority
 * over [fallbackUrl] (the Google account photo), which takes priority over a
 * generic person icon. Used in both the Home top bar and the Settings >
 * Profile card so both show the same avatar consistently.
 */
@Composable
fun ProfileAvatar(
    photoPath: String?,
    fallbackUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val model: Any? = photoPath?.let { File(it) } ?: fallbackUrl
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceDarkElevated)
            .then(clickableModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Default.Person, contentDescription = null, tint = OnSurfaceMutedDark)
        }
    }
}
