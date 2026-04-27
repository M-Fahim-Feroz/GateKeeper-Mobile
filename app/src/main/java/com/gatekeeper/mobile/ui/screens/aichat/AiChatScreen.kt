package com.gatekeeper.mobile.ui.screens.aichat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.domain.model.ChatMessage
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(viewModel: AiChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isServerOnline by viewModel.isServerOnline.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // App Bar / Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AccentOrange.copy(alpha = 0.08f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientOrange.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.SmartToy, null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Security Assistant", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    StatusBadge(
                        isActive = isServerOnline,
                        activeText = "Connected to Desktop AI",
                        inactiveText = "Disconnected from Backend"
                    )
                }
                IconButton(onClick = { viewModel.clearChat() }) {
                    Icon(Icons.Filled.DeleteOutline, "Clear Chat", tint = TextTertiary)
                }
            }
        }

        // ── Offline Banner — shown when desktop backend is unreachable ────────
        if (!isServerOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AccentOrange.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.WifiOff, contentDescription = "Offline", tint = AccentOrange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Desktop backend not connected",
                        style = MaterialTheme.typography.titleSmall,
                        color = AccentOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Go to Settings → Desktop Integration to set your PC's IP address.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Outlined.SmartToy, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("I am your AI Security Assistant.", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "I can help you monitor traffic, configure firewall rules, block malicious domains, and analyze device permissions.",
                                style = MaterialTheme.typography.bodyMedium, color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Text("Try asking:", color = TextSecondary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            SuggestionChip("Block YouTube from accessing internet", onClick = { inputText = "Block YouTube from accessing the internet" })
                            SuggestionChip("Allow Discord to connect", onClick = { inputText = "Remove Discord from the firewall blocklist" })
                            SuggestionChip("Are there any suspicious connections?", onClick = { inputText = "Are there any suspicious connections?" })
                        }
                    }
                }
            } else {
                items(messages) { message ->
                    ChatMessageItem(message)
                }
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SmartToy, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                                .background(DarkCard).padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            PulsingDots(color = AccentOrange)
                        }
                    }
                }
            }
        }

        // Input Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask security assistant...", color = TextTertiary) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentOrange.copy(alpha = 0.5f),
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )
                Spacer(Modifier.width(10.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && isServerOnline) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading && isServerOnline,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AccentOrange,
                        contentColor = DarkBackground,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) PrimaryCyan.copy(alpha = 0.15f) else DarkCard
    val shape = if (isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SmartToy, null, tint = AccentOrange, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
        }
        
        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .border(1.dp, if (isUser) PrimaryCyan.copy(alpha = 0.3f) else GlassBorder, shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            
            if (message.toolCalls.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Executed ${message.toolCalls.size} operations", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = DarkCard,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ArrowUpward, null, tint = TextTertiary, modifier = Modifier.size(14.dp))
        }
    }
}
