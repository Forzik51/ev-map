package feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import core.designsystem.AppTheme
import core.designsystem.Spacing
import core.ui.components.AuthTextField
import core.ui.components.PrimaryButton

@Preview(showBackground = true)
@Composable
private fun ProfileEditScreenPreview() {
    AppTheme {
        ProfileEditScreen(
            username = "username",
            phone = "phone",
            title = "title",
            bio = "bio",
            onUsernameChange = {},
            onPhoneChange = {},
            onTitleChange = {},
            onBioChange = {},
            onApply = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    username: String,
    phone: String,
    title: String,
    bio: String,
    onUsernameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Profile",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                Spacer(modifier = Modifier.height(Spacing.l))
                
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(94.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            // Reserved for future avatar selection
                        }
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Avatar",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.l))
                
                // Username field
                AuthTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = "username",
                    placeholder = "username"
                )
                
                // Phone field
                AuthTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = "phone",
                    placeholder = "phone"
                )
                
                // Title field
                AuthTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = "title",
                    placeholder = "title"
                )
                
                // Bio field (multiline)
                AuthTextField(
                    value = bio,
                    onValueChange = onBioChange,
                    label = "bio",
                    placeholder = "bio",
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp)
                )
                
                // Bottom padding to ensure apply button has space
                Spacer(modifier = Modifier.height(80.dp))
            }
            
            // Apply button positioned at bottom
            PrimaryButton(
                text = "Apply",
                onClick = onApply,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.m)
            )
        }
    }
}
