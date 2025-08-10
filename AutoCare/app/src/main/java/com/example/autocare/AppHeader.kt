package com.example.autocare

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String = "AutoCare",
    onBack: (() -> Unit)? = null
) {
    TopAppBar(
        navigationIcon = {
            onBack?.let { backAction ->
                IconButton(onClick = backAction) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint               = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text       = title,
                    modifier   = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                    color      = MaterialTheme.colorScheme.onPrimary,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Image(
                    painter           = painterResource(id = R.drawable.logotipo_num_1),
                    contentDescription = "Logo AutoCare",
                    modifier          = Modifier
                        .align(Alignment.CenterEnd)
                        .height(64.dp),
                    colorFilter       = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )
    )
}