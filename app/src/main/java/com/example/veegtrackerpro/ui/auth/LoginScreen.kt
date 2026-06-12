package com.example.veegtrackerpro.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.veegtrackerpro.BuildConfig
import com.example.veegtrackerpro.R

@Composable
fun LoginScreen(
    onSignInClick: () -> Unit,
    isLoading: Boolean,
    authError: String?,
    onDevBypassDriverClick: () -> Unit = {},
    onDevBypassAdminClick: () -> Unit = {}
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Veegtracker Pro",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(stringResource(R.string.action_sign_in_google))
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = authError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            if (BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDevBypassDriverClick,
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                ) {
                    Text(stringResource(R.string.action_dev_bypass_driver))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDevBypassAdminClick,
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                ) {
                    Text(stringResource(R.string.action_dev_bypass_admin))
                }
            }
        }
    }
}
