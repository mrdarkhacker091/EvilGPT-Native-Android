package com.evilgpt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Image as ImageIcon
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.evilgpt.app.data.ChatMessage
import com.evilgpt.app.data.ChatSession
import com.evilgpt.app.ui.AppState
import com.evilgpt.app.ui.EvilGptViewModel

private val Bg = Color(0xFF050706)
private val Panel = Color(0xFF0B100E)
private val Panel2 = Color(0xFF101713)
private val Green = Color(0xFF00F5A0)
private val Red = Color(0xFFFF3B45)
private val TextMain = Color(0xFFE8F1ED)
private val TextMuted = Color(0xFF8C9B93)
private val Border = Color(0xFF1B3027)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 7, 6)
        window.navigationBarColor = android.graphics.Color.rgb(5, 7, 6)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Green, onPrimary = Color.Black, secondary = Green,
                background = Bg, surface = Panel, onBackground = TextMain,
                onSurface = TextMain, error = Red
            )) {
                val vm: EvilGptViewModel = viewModel()
                val state by vm.state.collectAsState()
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    if (state.user == null) AuthScreen(state, vm)
                    else EvilGptChatScreen(state, vm)
                }
            }
        }
    }
}

@Composable
private fun AuthScreen(state: AppState, vm: EvilGptViewModel) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Bg).imePadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark(size = 76)
        Spacer(Modifier.height(18.dp))
        BrandWordmark()
        Text(if (isSignUp) "Create your EvilGPT account" else "Welcome back", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Email address") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(14.dp)
        )
        state.error?.let { Text(it, color = Red, modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center) }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { if (isSignUp) vm.signUp(email, password) else vm.signIn(email, password) },
            enabled = !state.busy && email.contains("@") && password.length >= 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            else Text(if (isSignUp) "Create account" else "Sign in", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { isSignUp = !isSignUp; vm.dismissError() }) {
            Text(if (isSignUp) "Already have an account? Sign in" else "New to EvilGPT? Create an account", color = Green)
        }
        Text("Your account and chat history use the existing EvilGPT Supabase backend.", color = TextMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
    }
    if (state.error != null && state.user == null) {
        // Inline error remains visible so authentication failures are actionable.
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvilGptChatScreen(state: AppState, vm: EvilGptViewModel) {
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var composer by remember { mutableStateOf("") }
    var modelMenu by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf<ChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }
    var historySearch by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(vm::attachImage) }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = Bg, drawerContentColor = TextMain, drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp), modifier = Modifier.width(310.dp)) {
                Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                    Spacer(Modifier.height(28.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(38)
                        Spacer(Modifier.width(10.dp))
                        BrandWordmark(compact = true)
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { vm.newChat(false); scope.launch { drawerState.close() } },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                        shape = RoundedCornerShape(13.dp)
                    ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New chat", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = historySearch, onValueChange = { historySearch = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Search conversations", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("YOUR CHATS", color = TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val filtered = state.sessions.filter { it.displayTitle.contains(historySearch, ignoreCase = true) }
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filtered, key = { it.id }) { session ->
                            var menu by remember { mutableStateOf(false) }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(if (session.id == state.activeSessionId) Panel2 else Color.Transparent)
                                    .clickable { vm.openSession(session.id); scope.launch { drawerState.close() } }
                                    .padding(start = 10.dp, top = 9.dp, bottom = 9.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, null, tint = TextMuted, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(session.displayTitle, modifier = Modifier.weight(1f), color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                if (session.starred) Icon(Icons.Default.Favorite, null, tint = Red, modifier = Modifier.size(14.dp))
                                Box {
                                    IconButton(onClick = { menu = true }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.MoreVert, null, tint = TextMuted, modifier = Modifier.size(18.dp)) }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { renameText = session.displayTitle; showRename = session; menu = false })
                                        DropdownMenuItem(text = { Text(if (session.starred) "Unfavourite" else "Favourite") }, leadingIcon = { Icon(if (session.starred) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) }, onClick = { vm.toggleStar(session); menu = false })
                                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) }, onClick = { vm.deleteSession(session.id); menu = false })
                                    }
                                }
                            }
                        }
                        if (filtered.isEmpty()) item { Text("No conversations found", color = TextMuted, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) }
                    }
                    Divider(color = Border)
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(state.user?.email.orEmpty(), color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                            Text("Signed in", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { vm.signOut() }) { Icon(Icons.Default.Logout, "Sign out", tint = TextMuted) }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Bg,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Evil", color = Red, fontWeight = FontWeight.Black)
                            Text("GPT", color = Green, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(8.dp))
                            Box {
                                Surface(
                                    onClick = { modelMenu = true },
                                    color = Panel2,
                                    shape = RoundedCornerShape(50),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                                ) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = Green, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(state.models.firstOrNull { it.id == state.selectedModel }?.label ?: state.selectedModel, color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                                    state.models.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Column { Text(model.label, fontWeight = FontWeight.SemiBold); Text(model.description, color = TextMuted, style = MaterialTheme.typography.labelSmall) } },
                                            onClick = { vm.setModel(model.id); modelMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawerState.open() } }) { Icon(Icons.Default.Menu, "Open chats", tint = TextMain) } },
                    actions = {
                        IconButton(onClick = { vm.newChat(!state.privateMode) }) {
                            Icon(Icons.Default.Lock, "Toggle private chat", tint = if (state.privateMode) Green else TextMuted)
                        }
                        IconButton(onClick = { vm.newChat(false) }) {
                            Icon(Icons.Default.Add, "New chat", tint = TextMain)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = TextMain)
                )
            },
            bottomBar = {
                Composer(
                    value = composer,
                    onValueChange = { composer = it },
                    busy = state.busy,
                    webSearch = state.webSearch,
                    deepThink = state.deepThink,
                    onWebSearch = vm::setWebSearch,
                    onDeepThink = vm::setDeepThink,
                    attachments = state.pendingAttachments.map { it.name to it.path },
                    onRemoveAttachment = vm::removeAttachment,
                    onAttach = { imagePicker.launch("image/*") },
                    onSend = { val text = composer; composer = ""; vm.sendMessage(text) }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (state.privateMode) {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF102219)).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Green, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Private chat · this conversation won't be saved to chat history", color = Green, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (state.error != null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF321719)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(state.error, color = Color(0xFFFFB4B4), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = vm::dismissError, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, tint = Color(0xFFFFB4B4)) }
                    }
                }
                if (state.loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
                } else if (state.messages.isEmpty()) {
                    WelcomePanel(onPrompt = { composer = it })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message -> MessageBubble(message) }
                        if (state.busy && state.messages.lastOrNull()?.role != "assistant") item { ThinkingIndicator(state.status) }
                    }
                }
            }
        }
    }

    if (showRename != null) {
        AlertDialog(
            onDismissRequest = { showRename = null },
            title = { Text("Rename conversation") },
            text = { OutlinedTextField(value = renameText, onValueChange = { renameText = it }, singleLine = true, label = { Text("Conversation name") }) },
            confirmButton = { TextButton(onClick = { showRename?.let { vm.renameSession(it.id, renameText) }; showRename = null }) { Text("Save", color = Green) } },
            dismissButton = { TextButton(onClick = { showRename = null }) { Text("Cancel") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun WelcomePanel(onPrompt: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandMark(68)
        Spacer(Modifier.height(18.dp))
        Text("What can I help you with?", color = TextMain, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Ask EvilGPT anything. Choose a model and start a conversation.", color = TextMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        val prompts = listOf("Explain a difficult topic simply", "Help me debug some code", "Brainstorm creative ideas", "Summarise a long document")
        prompts.forEach { prompt ->
            Surface(
                onClick = { onPrompt(prompt) },
                color = Panel,
                shape = RoundedCornerShape(13.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { Text(prompt, color = TextMain, modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp), style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    busy: Boolean,
    webSearch: Boolean,
    deepThink: Boolean,
    onWebSearch: (Boolean) -> Unit,
    onDeepThink: (Boolean) -> Unit,
    attachments: List<Pair<String, String>>,
    onRemoveAttachment: (String) -> Unit,
    onAttach: () -> Unit,
    onSend: () -> Unit
) {
    Column(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(10.dp)) {
                if (attachments.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        attachments.forEach { (name, path) ->
                            Surface(color = Color(0xFF13251B), shape = RoundedCornerShape(9.dp)) {
                                Row(Modifier.padding(start = 8.dp, end = 3.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ImageIcon, null, tint = Green, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(name, color = TextMain, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    IconButton(onClick = { onRemoveAttachment(path) }, modifier = Modifier.size(22.dp)) { Icon(Icons.Default.Close, "Remove image", tint = TextMuted, modifier = Modifier.size(13.dp)) }
                                }
                            }
                        }
                    }
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 54.dp).padding(horizontal = 5.dp, vertical = 8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextMain),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Green),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) Text("Message EvilGPT…", color = TextMuted)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAttach, enabled = !busy, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Add, "Attach image", tint = TextMuted) }
                    SmallToggle("Web", Icons.Default.Language, webSearch, onWebSearch)
                    Spacer(Modifier.width(6.dp))
                    SmallToggle("Deep think", Icons.Default.Psychology, deepThink, onDeepThink)
                    Spacer(Modifier.weight(1f))
                    Text("Enter to send", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onSend,
                        enabled = (value.isNotBlank() || attachments.isNotEmpty()) && !busy,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(if (value.isNotBlank() && !busy) Green else Panel2)
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Green, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Send, "Send message", tint = if (value.isNotBlank() || attachments.isNotEmpty()) Color.Black else TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Text("EvilGPT can make mistakes. Check important information.", color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.fillMaxWidth().padding(top = 7.dp), textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmallToggle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: (Boolean) -> Unit) {
    Surface(
        onClick = { onClick(!selected) },
        color = if (selected) Color(0xFF123323) else Panel2,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Green.copy(alpha = .55f) else Border)
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (selected) Green else TextMuted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (selected) Green else TextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        if (!isUser) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF10251B)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, null, tint = Green, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
        }
        Column(Modifier.widthIn(max = 320.dp).then(if (isUser) Modifier else Modifier.weight(1f, fill = false))) {
            Text(if (isUser) "YOU" else "EVILGPT", color = TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 3.dp, bottom = 5.dp))
            Surface(
                color = if (isUser) Color(0xFF10241B) else Panel,
                shape = RoundedCornerShape(if (isUser) 18.dp else 15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isUser) Color(0xFF1B4B35) else Border)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    if (message.images.isNotEmpty()) {
                        message.images.forEach { image ->
                            Row(Modifier.padding(bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ImageIcon, null, tint = Green, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(image.name, color = Green, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    if (message.content.isNotBlank()) Text(message.content, color = TextMain, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator(status: String) {
    Row(Modifier.fillMaxWidth().padding(start = 39.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(15.dp), color = Green, strokeWidth = 2.dp)
        Spacer(Modifier.width(9.dp))
        Text(status.ifBlank { "EvilGPT is thinking…" }, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BrandMark(size: Int) {
    Box(
        Modifier.size(size.dp).clip(RoundedCornerShape((size * .24f).dp)).background(Color(0xFF101A14)).border(1.dp, Border, RoundedCornerShape((size * .24f).dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(painter = painterResource(com.evilgpt.app.R.drawable.evilgpt_icon), contentDescription = "EvilGPT", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape((size * .24f).dp)))
    }
}

@Composable
private fun BrandWordmark(compact: Boolean = false) {
    Row {
        Text("Evil", color = Red, fontWeight = FontWeight.Black, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
        Text("GPT", color = Green, fontWeight = FontWeight.Black, style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium)
    }
}
