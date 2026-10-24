package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Custom Fintel Logo component:
 * - Light theme: Exact green mark (#157347) on transparent background.
 * - Dark theme: Exact white mark (#FFFFFF) on transparent background.
 */
@Composable
fun OikoNomosLogo(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isDarkTheme) R.drawable.fintel_logo_white else R.drawable.fintel_logo
            ),
            contentDescription = "Fintel Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}
