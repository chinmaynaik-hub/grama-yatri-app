package com.gramayatri.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.gramayatri.app.location.FusedLocationTracker
import com.gramayatri.app.repository.AuthRepository
import com.gramayatri.app.repository.FirebaseRepository
import com.gramayatri.app.ui.BusTrackerScreen
import com.gramayatri.app.ui.CreateAccountScreen
import com.gramayatri.app.ui.DriverModeScreen
import com.gramayatri.app.ui.ForgotPasswordScreen
import com.gramayatri.app.ui.LoginScreen
import com.gramayatri.app.ui.RouteListScreen
import com.gramayatri.app.ui.theme.GramayatrinewTheme
import com.gramayatri.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class AppRole {
    DRIVER_OR_ADMIN,
    USER
}

private enum class AuthScreen {
    LOGIN,
    CREATE_ACCOUNT,
    FORGOT_PASSWORD
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val authRepository = AuthRepository(
            auth = firebaseAuth,
            firestore = db
        )
        val repository = FirebaseRepository(db)
        viewModel = MainViewModel(
            repository = repository,
            auth = firebaseAuth
        )

        setContent {
            val routes by viewModel.routes.collectAsStateWithLifecycle()
            val selectedRoute by viewModel.selectedRoute.collectAsStateWithLifecycle()
            val liveLocation by viewModel.liveLocation.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
            val isAddingRoute by viewModel.isAddingRoute.collectAsStateWithLifecycle()
            var appRole by rememberSaveable { mutableStateOf<AppRole?>(null) }
            var isDriverMode by rememberSaveable { mutableStateOf(false) }
            var authScreen by rememberSaveable { mutableStateOf(AuthScreen.LOGIN) }
            var authError by rememberSaveable { mutableStateOf<String?>(null) }
            var authInfoMessage by rememberSaveable { mutableStateOf<String?>(null) }
            var isAuthenticating by rememberSaveable { mutableStateOf(false) }
            var isCheckingEmail by rememberSaveable { mutableStateOf(false) }
            var accountExists by rememberSaveable { mutableStateOf<Boolean?>(null) }
            var createAccountInitialEmail by rememberSaveable { mutableStateOf("") }
            var createAccountInitialRole by rememberSaveable { mutableStateOf(ROLE_USER) }
            val authScope = rememberCoroutineScope()
            val currentUser = firebaseAuth.currentUser
            val currentUserId = currentUser?.uid ?: ""
            val currentDisplayName = currentUser?.displayName.orEmpty()
            val currentEmail = currentUser?.email.orEmpty()
            val currentUserName = when {
                currentDisplayName.isNotBlank() -> currentDisplayName
                currentEmail.isNotBlank() -> currentEmail
                else -> currentUserId
            }

            val openCreateAccount: (String, String) -> Unit = { email, role ->
                authError = null
                authInfoMessage = null
                createAccountInitialEmail = email
                createAccountInitialRole = normalizeRole(role)
                authScreen = AuthScreen.CREATE_ACCOUNT
            }

            val performCheckEmail: (String) -> Unit = { email ->
                authScope.launch {
                    isCheckingEmail = true
                    val checkResult = authRepository.checkAccountExists(email = email.trim())
                    if (checkResult.success) {
                        accountExists = checkResult.accountExists
                    } else {
                        accountExists = null
                    }
                    isCheckingEmail = false
                }
            }

            val performLogin: (String, String) -> Unit = { email, password ->
                authError = null
                authInfoMessage = null
                isAuthenticating = true
                authScope.launch {
                    val normalizedEmail = email.trim()
                    val loginResult = authRepository.signIn(email = normalizedEmail, password = password)
                    if (loginResult.success) {
                        accountExists = true
                        appRole = mapToAppRole(loginResult.role)
                        authScreen = AuthScreen.LOGIN
                        isDriverMode = false
                        viewModel.clearSelection()
                    } else {
                        accountExists = loginResult.accountExists
                        authError = loginResult.message ?: "Login failed. Check your credentials."
                    }
                    isAuthenticating = false
                }
            }

            val performCreateAccount: (String, String, String) -> Unit = { email, password, role ->
                authError = null
                authInfoMessage = null
                isAuthenticating = true
                authScope.launch {
                    val createAccountResult = authRepository.createAccount(
                        email = email.trim(),
                        password = password,
                        role = role
                    )
                    if (createAccountResult.success) {
                        authScreen = AuthScreen.LOGIN
                        authInfoMessage = createAccountResult.message ?: "Account created successfully. Please login."
                        accountExists = true
                    } else {
                        authError = createAccountResult.message ?: "Unable to create account right now."
                    }
                    isAuthenticating = false
                }
            }

            val performForgotPassword: (String) -> Unit = { email ->
                authError = null
                authInfoMessage = null
                isAuthenticating = true
                authScope.launch {
                    val resetResult = authRepository.sendPasswordReset(email = email)
                    if (resetResult.success) {
                        authInfoMessage = resetResult.message ?: "Password reset link sent to your email."
                    } else {
                        authError = resetResult.message ?: "Unable to send reset email right now."
                    }
                    isAuthenticating = false
                }
            }
            val fusedLocationClient = remember {
                LocationServices.getFusedLocationProviderClient(this@MainActivity)
            }
            val locationTracker = remember {
                FusedLocationTracker(
                    context = this@MainActivity,
                    fusedLocationClient = fusedLocationClient
                )
            }

            GramayatrinewTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        appRole == null -> {
                            when (authScreen) {
                                AuthScreen.LOGIN -> {
                                    LoginScreen(
                                        isLoading = isAuthenticating,
                                        isCheckingEmail = isCheckingEmail,
                                        accountExists = accountExists,
                                        errorMessage = authError,
                                        infoMessage = authInfoMessage,
                                        onCheckEmailAccount = performCheckEmail,
                                        onLogin = performLogin,
                                        onNoAccountAsUserClick = { email ->
                                            openCreateAccount(email, ROLE_USER)
                                        },
                                        onNoAccountAsDriverOrAdminClick = { email ->
                                            openCreateAccount(email, ROLE_DRIVER)
                                        },
                                        onCreateAccountClick = {
                                            openCreateAccount("", ROLE_USER)
                                        },
                                        onForgotPasswordClick = {
                                            authError = null
                                            authInfoMessage = null
                                            authScreen = AuthScreen.FORGOT_PASSWORD
                                        }
                                    )
                                }

                                AuthScreen.CREATE_ACCOUNT -> {
                                    CreateAccountScreen(
                                        isLoading = isAuthenticating,
                                        errorMessage = authError,
                                        initialEmail = createAccountInitialEmail,
                                        initialRole = createAccountInitialRole,
                                        onCreateAccount = performCreateAccount,
                                        onBackToLogin = {
                                            authError = null
                                            authInfoMessage = null
                                            authScreen = AuthScreen.LOGIN
                                        }
                                    )
                                }

                                AuthScreen.FORGOT_PASSWORD -> {
                                    ForgotPasswordScreen(
                                        isLoading = isAuthenticating,
                                        errorMessage = authError,
                                        infoMessage = authInfoMessage,
                                        onSendResetEmail = { email ->
                                            performForgotPassword(email)
                                        },
                                        onBackToLogin = {
                                            authError = null
                                            authInfoMessage = null
                                            authScreen = AuthScreen.LOGIN
                                        }
                                    )
                                }
                            }
                        }

                        selectedRoute == null -> {
                            RouteListScreen(
                                routes = routes,
                                errorMessage = errorMessage,
                                showAddRouteAction = appRole == AppRole.DRIVER_OR_ADMIN,
                                isAddingRoute = isAddingRoute,
                                onAddRoute = { routeName, routeDescription, stopsInput, onResult ->
                                    viewModel.addRoute(
                                        routeName = routeName,
                                        routeDescription = routeDescription,
                                        stopsInput = stopsInput,
                                        onResult = onResult
                                    )
                                },
                                onRouteSelected = { route ->
                                    isDriverMode = false
                                    viewModel.selectRoute(route)
                                }
                            )
                        }

                        isDriverMode && appRole == AppRole.DRIVER_OR_ADMIN -> {
                            DriverModeScreen(
                                route = selectedRoute!!,
                                userId = currentUserId,
                                userName = currentUserName,
                                repository = repository,
                                locationTracker = locationTracker,
                                onBack = { isDriverMode = false }
                            )
                        }

                        else -> {
                            BusTrackerScreen(
                                route = selectedRoute!!,
                                busLocation = liveLocation,
                                onBack = {
                                    isDriverMode = false
                                    viewModel.clearSelection()
                                },
                                onDriverMode = { isDriverMode = true },
                                showDriverModeAction = appRole == AppRole.DRIVER_OR_ADMIN
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun mapToAppRole(role: String?): AppRole {
    return when (role?.trim()?.lowercase()) {
        ROLE_DRIVER, ROLE_ADMIN -> AppRole.DRIVER_OR_ADMIN
        else -> AppRole.USER
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
