package com.duckblast.game.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duckblast.game.data.model.Profile
import com.duckblast.game.ui.theme.DuckBlastAccent
import com.duckblast.game.ui.theme.DuckBlastOutline
import com.duckblast.game.ui.theme.DuckBlastPrimary
import com.duckblast.game.ui.theme.DuckBlastSurface
import com.duckblast.game.ui.theme.DuckBlastText
import com.duckblast.game.ui.theme.SkyTop
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onProfileSelected: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SkyTop)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "SELECT HUNTER",
                style = MaterialTheme.typography.headlineMedium,
                color = DuckBlastText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.profiles.isEmpty() && !state.loading) {
                    item {
                        Text(
                            text = "No hunters yet — tap NEW HUNTER below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DuckBlastText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(state.profiles, key = { it.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        selected = profile.id == state.selectedId,
                        onSelect = {
                            viewModel.selectProfile(profile.id)
                            onProfileSelected()
                        },
                        onLongPress = { viewModel.requestDelete(profile.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBack != null) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DuckBlastSurface,
                            contentColor = DuckBlastText
                        )
                    ) { Text("BACK", style = MaterialTheme.typography.labelLarge) }
                }
                Button(
                    onClick = viewModel::openCreateDialog,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DuckBlastPrimary,
                        contentColor = DuckBlastSurface
                    )
                ) { Text("NEW HUNTER", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateHunterDialog(
            onDismiss = viewModel::dismissCreateDialog,
            onConfirm = viewModel::createProfile,
            error = state.errorMessage
        )
    }

    val deleteId = state.pendingDeleteId
    if (deleteId != null) {
        val target = state.profiles.firstOrNull { it.id == deleteId }
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Delete hunter?") },
            text = {
                Text("This will remove ${target?.name ?: "this hunter"} and their scores.")
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    selected: Boolean,
    onSelect: () -> Unit,
    onLongPress: () -> Unit
) {
    val borderColor = if (selected) DuckBlastAccent else DuckBlastOutline
    val borderWidth = if (selected) 3.dp else 1.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DuckBlastSurface)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onSelect, onLongClick = onLongPress)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(profile = profile)
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.name.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = DuckBlastText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HI ${profile.highScore} • LVL ${profile.highestLevelReached}",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText
            )
            Text(
                text = "ACC ${"%.0f".format(profile.accuracy * 100)}% • ${profile.totalGamesPlayed} GAMES",
                style = MaterialTheme.typography.labelSmall,
                color = DuckBlastText
            )
        }
        if (selected) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.headlineMedium,
                color = DuckBlastAccent
            )
        }
    }
}

@Composable
private fun Avatar(profile: Profile) {
    val color = runCatching { Color(android.graphics.Color.parseColor(profile.avatarColorHex)) }
        .getOrDefault(DuckBlastPrimary)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .border(2.dp, DuckBlastOutline, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.initial,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CreateHunterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    error: String?
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Hunter") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(16) },
                    label = { Text("Hunter name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(name) })
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("CREATE") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
