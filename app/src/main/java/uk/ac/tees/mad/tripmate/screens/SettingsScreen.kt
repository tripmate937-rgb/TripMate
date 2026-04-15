package uk.ac.tees.mad.tripmate.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import uk.ac.tees.mad.tripmate.R
import uk.ac.tees.mad.tripmate.viewmodel.TripViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: TripViewModel = viewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF673AB7)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.people),
                            contentDescription = "Profile",
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Text(
                        text = currentUser?.email ?: "User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7)
                    )

                    Text(
                        text = "TripMate Member",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = R.drawable.delete,
                        title = "Clear Cache",
                        subtitle = "Free up storage space",
                        iconTint = Color(0xFF00BCD4),
                        onClick = { showClearCacheDialog = true }
                    )

                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        icon = R.drawable.people,
                        title = "Account",
                        subtitle = currentUser?.email ?: "",
                        iconTint = Color(0xFF673AB7),
                        onClick = {}
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsItem(
                        icon = R.drawable.right_arrow,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        iconTint = Color.Red,
                        onClick = { showLogoutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "TripMate v1.0.0",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                    showLogoutDialog = false
                },
                onDismiss = { showLogoutDialog = false }
            )
        }

        if (showClearCacheDialog) {
            ClearCacheDialog(
                onConfirm = {
                    currentUser?.uid?.let { userId ->
                        viewModel.clearLocalCache(userId)  // ADD THIS
                    }
                    cacheCleared = true
                    showClearCacheDialog = false
                },
                onDismiss = { showClearCacheDialog = false }
            )
        }

        if (cacheCleared) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                cacheCleared = false
            }

            Snackbar(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White
            ) {
                Text("Cache cleared successfully")
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: Int,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Image(
            painter = painterResource(id = R.drawable.right_arrow),
            contentDescription = "Arrow",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.right_arrow),
                contentDescription = "Logout",
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Logout?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to logout from your account?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF673AB7))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ClearCacheDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = "Clear Cache",
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Clear Cache?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will remove all locally stored data. Your trips will still be saved in the cloud."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00BCD4)
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF673AB7))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF00BCD4),
                                            Color(0xFF673AB7)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤",
                                fontSize = 40.sp
                            )
                        }

                        Text(
                            text = "user@example.com",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )

                        Text(
                            text = "TripMate Member",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsItemPreview(
                            emoji = "🗑️",
                            title = "Clear Cache",
                            subtitle = "Free up storage space",
                            iconColor = Color(0xFF00BCD4)
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )

                        SettingsItemPreview(
                            emoji = "👤",
                            title = "Account",
                            subtitle = "user@example.com",
                            iconColor = Color(0xFF673AB7)
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SettingsItemPreview(
                        emoji = "🚪",
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        iconColor = Color.Red
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TripMate v1.0.0",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsItemPreview(
    emoji: String,
    title: String,
    subtitle: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 24.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        Text(
            text = "→",
            fontSize = 20.sp,
            color = Color.Gray
        )
    }
}