package com.gramayatri.app.ui

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    isLoading: Boolean,
    isCheckingEmail: Boolean,
    accountExists: Boolean?,
    errorMessage: String?,
    infoMessage: String?,
    onCheckEmailAccount: (email: String) -> Unit,
    onLogin: (email: String, password: String) -> Unit,
    onNoAccountAsUserClick: (email: String) -> Unit,
    onNoAccountAsDriverOrAdminClick: (email: String) -> Unit,
    onCreateAccountClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val lastCheckedEmailState = rememberSaveable { mutableStateOf("") }
    val lastCheckedEmail = lastCheckedEmailState.value

    val normalizedEmail = email.trim()
    val canCheckEmail = isValidEmail(normalizedEmail)
    val noAccountForCurrentEmail = accountExists == false &&
        normalizedEmail.equals(lastCheckedEmail, ignoreCase = true)
    val canSubmit = normalizedEmail.isNotEmpty() &&
        password.isNotEmpty() &&
        !isLoading &&
        !isCheckingEmail &&
        !noAccountForCurrentEmail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFEAF3FF),
                        Color(0xFFF6EDFF)
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Grama-Yatri",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Enter your email and password to login",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && canCheckEmail) {
                        lastCheckedEmailState.value = normalizedEmail
                        onCheckEmailAccount(normalizedEmail)
                    }
                },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            supportingText = {
                if (isCheckingEmail) {
                    Text("Checking account...")
                } else if (noAccountForCurrentEmail) {
                    Text("No account found for this email.")
                }
            },
            isError = noAccountForCurrentEmail
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        if (noAccountForCurrentEmail) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No account exists. Choose your type to create one:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNoAccountAsUserClick(normalizedEmail) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("I am User")
                }
                Button(
                    onClick = { onNoAccountAsDriverOrAdminClick(normalizedEmail) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    Text("I am Driver/Admin")
                }
            }
        }

        infoMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                onLogin(normalizedEmail, password)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCreateAccountClick, enabled = !isLoading) {
                Text("Create account")
            }
            TextButton(onClick = onForgotPasswordClick, enabled = !isLoading) {
                Text("Forgot password?")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            isLoading = false,
            isCheckingEmail = false,
            accountExists = null,
            errorMessage = null,
            infoMessage = null,
            onCheckEmailAccount = {},
            onLogin = { _, _ -> },
            onNoAccountAsUserClick = {},
            onNoAccountAsDriverOrAdminClick = {},
            onCreateAccountClick = {},
            onForgotPasswordClick = {}
        )
    }
}

private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
