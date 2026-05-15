package com.gramayatri.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CreateAccountScreen(
    isLoading: Boolean,
    errorMessage: String?,
    initialEmail: String,
    initialRole: String,
    onCreateAccount: (email: String, password: String, role: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var selectedRole by rememberSaveable(initialRole) {
        mutableStateOf(normalizeRole(initialRole))
    }

    val isPasswordMatch = password == confirmPassword
    val canSubmit = email.trim().isNotEmpty() &&
        password.isNotEmpty() &&
        confirmPassword.isNotEmpty() &&
        isPasswordMatch &&
        !isLoading

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
            text = "Create your account",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select your role and sign up to start using Grama-Yatri",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "I am a:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        RoleOptionRow(
            title = "User",
            selected = selectedRole == ROLE_USER,
            onSelect = { selectedRole = ROLE_USER },
            enabled = !isLoading
        )
        RoleOptionRow(
            title = "Driver",
            selected = selectedRole == ROLE_DRIVER,
            onSelect = { selectedRole = ROLE_DRIVER },
            enabled = !isLoading
        )
        RoleOptionRow(
            title = "Admin",
            selected = selectedRole == ROLE_ADMIN,
            onSelect = { selectedRole = ROLE_ADMIN },
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            enabled = !isLoading
        )

        if (!isPasswordMatch && confirmPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passwords do not match.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
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
            onClick = { onCreateAccount(email.trim(), password, selectedRole) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit
        ) {
            Text("Create account")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBackToLogin, enabled = !isLoading) {
            Text("Back to login")
        }
    }
}

@Composable
private fun RoleOptionRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
                enabled = enabled
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            enabled = enabled
        )
        Text(text = title)
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateAccountScreenPreview() {
    MaterialTheme {
        CreateAccountScreen(
            isLoading = false,
            errorMessage = null,
            initialEmail = "",
            initialRole = ROLE_USER,
            onCreateAccount = { _, _, _ -> },
            onBackToLogin = {}
        )
    }
}

private fun normalizeRole(role: String): String {
    return when (role.trim().lowercase()) {
        ROLE_DRIVER -> ROLE_DRIVER
        ROLE_ADMIN -> ROLE_ADMIN
        else -> ROLE_USER
    }
}

private const val ROLE_USER = "user"
private const val ROLE_DRIVER = "driver"
private const val ROLE_ADMIN = "admin"
