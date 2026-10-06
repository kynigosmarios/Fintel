package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Custom OikoNomos Logo component adhering to styling specifications:
 * - Light theme: Green vector mark (#137A51) on transparent/white background without circles/frames.
 * - Dark theme: Green background (#137A51) badge with crisp white logo mark.
 */
@Composable
fun OikoNomosLogo(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    if (isDarkTheme) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.24f))
                .background(Color(0xFF137A51))
                .padding(size * 0.14f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_oikonomos_logo_white),
                contentDescription = "OikoNomos Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_oikonomos_logo),
                contentDescription = "OikoNomos Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
