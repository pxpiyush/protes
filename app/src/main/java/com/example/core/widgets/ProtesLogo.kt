package com.example.core.widgets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Scalable representation of the official Protes Logo.
 * Uses the official vector resource ic_protes_logo and handles responsive sizing and layout.
 */
@Composable
fun ProtesLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 120.dp,
    showText: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.ic_protes_logo),
            contentDescription = "Protes Logo Icon",
            modifier = Modifier.width(iconSize).heightIn(max = iconSize),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )

        if (showText) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "protes",
                fontSize = (iconSize.value * 0.25f).sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = if (textColor == Color.Unspecified) MaterialTheme.colorScheme.primary else textColor,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

