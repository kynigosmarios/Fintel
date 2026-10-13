package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald700

@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Emerald700,
    textColor: Color = Color.White,
    fontSize: TextUnit = 16.sp
) {
    val trimmedPhoto = photoUrl?.trim().orEmpty()

    val decodedBitmap = remember(trimmedPhoto) {
        if (trimmedPhoto.startsWith("data:image")) {
            try {
                val base64Data = trimmedPhoto.substringAfter("base64,")
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = displayName ?: "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            trimmedPhoto.startsWith("http://") || trimmedPhoto.startsWith("https://") -> {
                AsyncImage(
                    model = trimmedPhoto,
                    contentDescription = displayName ?: "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                val initial = displayName?.trim()?.firstOrNull()?.uppercase() ?: "O"
                Text(
                    text = initial,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize
                )
            }
        }
    }
}
