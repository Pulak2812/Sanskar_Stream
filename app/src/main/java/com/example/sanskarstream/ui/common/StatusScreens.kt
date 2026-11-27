package com.example.sanskarstream.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sanskarstream.ui.theme.Saffron

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text("Sanskar Stream", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        CircularProgressIndicator(color = Saffron)
        Text("Channels load ho rahe hain…", color = Color(0xFFBDBDBD), fontSize = 16.sp)
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    val retry = remember { FocusRequester() }
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Text("Channels load nahi hue", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(message, color = Color(0xFFBDBDBD), fontSize = 16.sp)
        TvButton("⟳  Retry", modifier = Modifier.focusRequester(retry), selected = true, onClick = onRetry)
    }
    RequestInitialFocus(retry)
}
