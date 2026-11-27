package feature.settings.ui
import androidx.compose.ui.res.stringResource
import com.evmap.mobileapp.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import core.designsystem.AppTheme
import core.ui.components.AppBottomBar
import core.ui.components.SettingsRow

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            onOpenProfileEdit = {},
            onOpenSaved = {},
            onOpenAdmin = {},
            showAdmin = true,
            onNavigate = {},
            onLogOut = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenProfileEdit: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenAdmin: () -> Unit,
    showAdmin: Boolean,
    onNavigate: (String) -> Unit,
    onLogOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                currentRoute = "profile",
                onNavigate = onNavigate
            )
        },
        topBar = {
             TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.form_back_b52b36),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.form_settings_c7f73b),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.form_back_b52b36),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SettingsRow(
                label = stringResource(R.string.form_profile_ff4fc0),
                onClick = onOpenProfileEdit
            )
            
            SettingsRow(
                label = stringResource(R.string.form_saved_events_f9cdab),
                onClick = onOpenSaved
            )

            if (showAdmin) {
                SettingsRow(
                    label = stringResource(R.string.form_admin_moderation_e37380),
                    onClick = onOpenAdmin
                )
            }
            
            SettingsRow(
                label = stringResource(R.string.form_log_out_6e78c9),
                onClick = onLogOut
            )
        }
    }
}
