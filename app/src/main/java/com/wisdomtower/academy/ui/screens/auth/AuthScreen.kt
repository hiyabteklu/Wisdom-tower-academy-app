package com.wisdomtower.academy.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.data.api.SupabaseClient
import com.wisdomtower.academy.data.repository.WisdomRepository
import com.wisdomtower.academy.ui.theme.CyanAccent
import com.wisdomtower.academy.ui.theme.CyanPrimary
import com.wisdomtower.academy.ui.theme.EmeraldSuccess
import com.wisdomtower.academy.ui.theme.NavyBackground
import com.wisdomtower.academy.ui.theme.NavyCardBorder
import com.wisdomtower.academy.ui.theme.NavySurface
import com.wisdomtower.academy.ui.theme.NavySurfaceVariant
import com.wisdomtower.academy.ui.theme.RoseError
import com.wisdomtower.academy.ui.theme.TextPrimary
import com.wisdomtower.academy.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: WisdomRepository,
    onAuthSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NavyBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Academy Brand Emblem
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CyanPrimary.copy(alpha = 0.25f), NavySurfaceVariant)
                        )
                    )
                    .border(1.5.dp, CyanPrimary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Wisdom Tower Academy Logo",
                    tint = CyanPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Wisdom Tower Academy",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Ethiopian Academic & Professional Prep Platform",
                fontSize = 13.sp,
                color = CyanAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Freshman curriculum is 100% free for all registered scholars.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // Auth Card Form
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, NavyCardBorder, RoundedCornerShape(20.dp)),
                color = NavySurface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSignUp) "Create Scholar Account" else "Welcome Back",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = isSignUp) {
                        Column {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name", color = TextSecondary) },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fullname_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = NavyCardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = CyanAccent)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = TextSecondary) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent)
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = NavyCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = RoseError,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    val scholarName = fullName.ifBlank { "Scholar" }
                                    val scholarEmail = email.ifBlank { "scholar@wisdomtower.academy" }
                                    repository.sessionManager.setGuestMode(true)
                                    val userId = "scholar_${System.currentTimeMillis()}"
                                    repository.sessionManager.saveSession(
                                        token = "scholar_session_token",
                                        refreshToken = null,
                                        userId = userId,
                                        email = scholarEmail,
                                        displayName = scholarName
                                    )
                                    coroutineScope.launch {
                                        repository.unlockPackage(
                                            userId = userId,
                                            packageId = "freshman",
                                            packageName = "Freshman Package"
                                        )
                                        onAuthSuccess()
                                    }
                                }
                            ) {
                                Text(
                                    text = "→ Bypass & Enter in Scholar Mode Now",
                                    color = CyanAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = successMessage != null) {
                        Text(
                            text = successMessage ?: "",
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Auth Action Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.length < 6) {
                                errorMessage = "Please enter a valid email and minimum 6-character password"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val supabase = SupabaseClient()
                                if (isSignUp) {
                                    val res = supabase.signUp(email.trim(), password, fullName.ifBlank { "Scholar" })
                                    isLoading = false
                                    if (res.isSuccess) {
                                        val userId = res.profile?.id ?: "usr_${System.currentTimeMillis()}"
                                        val token = res.accessToken ?: repository.supabaseClient.anonKey
                                        repository.supabaseClient.setSessionToken(token)
                                        repository.sessionManager.saveSession(
                                            token = token,
                                            refreshToken = res.refreshToken,
                                            userId = userId,
                                            email = email.trim(),
                                            displayName = fullName.ifBlank { "Scholar" }
                                        )
                                        // Auto-enroll in Freshman Free
                                        repository.unlockPackage(
                                            userId = userId,
                                            packageId = "freshman",
                                            packageName = "Freshman Package"
                                        )
                                        repository.syncRemoteEnrollments(userId)
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = res.errorMessage
                                    }
                                } else {
                                    val res = supabase.signIn(email.trim(), password)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        val userId = res.profile?.id ?: "usr_${System.currentTimeMillis()}"
                                        val token = res.accessToken ?: repository.supabaseClient.anonKey
                                        repository.supabaseClient.setSessionToken(token)
                                        repository.sessionManager.saveSession(
                                            token = token,
                                            refreshToken = res.refreshToken,
                                            userId = userId,
                                            email = email.trim(),
                                            displayName = res.profile?.displayName ?: "Scholar"
                                        )
                                        repository.unlockPackage(
                                            userId = userId,
                                            packageId = "freshman",
                                            packageName = "Freshman Package"
                                        )
                                        repository.syncRemoteEnrollments(userId)
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = res.errorMessage
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_auth_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = NavyBackground
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = NavyBackground,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isSignUp) "Register & Unlock Freshman Free" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Instant Scholar Access (Guest / Preview mode)
                    OutlinedButton(
                        onClick = {
                            val scholarName = fullName.ifBlank { "Scholar" }
                            val scholarEmail = email.ifBlank { "scholar@wisdomtower.academy" }
                            val userId = "scholar_${System.currentTimeMillis()}"
                            val validToken = repository.supabaseClient.anonKey

                            repository.supabaseClient.setSessionToken(validToken)
                            repository.sessionManager.setGuestMode(true)
                            repository.sessionManager.saveSession(
                                token = validToken,
                                refreshToken = null,
                                userId = userId,
                                email = scholarEmail,
                                displayName = scholarName
                            )
                            coroutineScope.launch {
                                repository.unlockPackage(
                                    userId = userId,
                                    packageId = "freshman",
                                    packageName = "Freshman Package"
                                )
                                repository.syncRemoteEnrollments(userId)
                                onAuthSuccess()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("instant_scholar_button"),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = NavySurfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Instant Access • Continue as Scholar",
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = {
                            isSignUp = !isSignUp
                            errorMessage = null
                            successMessage = null
                        },
                        modifier = Modifier.testTag("toggle_signup_button")
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "New to Wisdom Tower? Create Account",
                            color = CyanAccent,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Security assurance indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Encrypted",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "EncryptedSharedPreferences Session • Official Supabase Auth",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
