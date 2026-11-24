package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.R
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.signOut
import com.example.data.model.ApprovalStatus
import com.example.data.model.FundCampaign
import com.example.data.model.UserProfile
import com.example.data.prefs.LanguagePreferences
import com.example.data.repository.AlumniRepository
import com.example.notifications.NotificationPreferences
import com.example.ui.components.AppLogoImage
import com.example.ui.components.ErrorBannerCard
import com.example.ui.components.ExitAppConfirmationDialog
import com.example.ui.components.LanguageSelectorButtons
import com.example.ui.components.SplashOverlayScreen
import com.example.ui.components.exitAndroidApp
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.BengaliStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BatchGalleryScreen
import com.example.ui.screens.BloodBankScreen
import com.example.ui.screens.DirectoryScreen
import com.example.ui.screens.EventsFundScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.ManagementPanelScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.TeachersScreen
import com.example.ui.state.UiState
import com.example.ui.theme.SpaAlumniTheme
import com.example.ui.viewmodel.AlumniViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_DIRECTORY = "directory"
const val ROUTE_BLOOD_BANK = "blood_bank"
const val ROUTE_EVENTS_FUND = "events_fund"
const val ROUTE_PROFILE = "profile"
const val ROUTE_TEACHERS = "teachers"
const val ROUTE_LEDGER = "ledger"
const val ROUTE_MANAGEMENT = "management"
const val ROUTE_ABOUT = "about"
const val ROUTE_GALLERY = "gallery"

private data class BottomTabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun AppRoot(
    openBloodRequestsFromNotification: Boolean = false,
    onConsumedBloodNotificationNav: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val languagePrefs = remember(context) { LanguagePreferences(context.applicationContext) }
    val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.BENGALI)
    val strings = if (language == AppLanguage.BENGALI) BengaliStrings else EnglishStrings

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var checkedAutoSignIn by remember { mutableStateOf(Firebase.auth.currentUser != null) }
    var showOpeningSplash by rememberSaveable { mutableStateOf(true) }
    var showSignInSuccessSplash by remember { mutableStateOf(false) }
    var showClosingSplash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showOpeningSplash = false
    }

    LaunchedEffect(showSignInSuccessSplash) {
        if (showSignInSuccessSplash) {
            delay(1600)
            showSignInSuccessSplash = false
        }
    }

    LaunchedEffect(showClosingSplash) {
        if (showClosingSplash) {
            delay(1600)
            exitAndroidApp(context)
        }
    }

    BackHandler(enabled = showClosingSplash) {}

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    checkedAutoSignIn = true
                },
                onUnauthenticated = {
                    checkedAutoSignIn = true
                },
                scope = scope
            )
        }
    }

    SpaAlumniTheme(isBengali = language == AppLanguage.BENGALI) {
        Box(modifier = Modifier.fillMaxSize()) {
            val user = currentUser
            if (!checkedAutoSignIn) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (user == null) {
                AuthScreen(
                    strings = strings,
                    currentLanguage = language,
                    onSelectLanguage = { selected ->
                        scope.launch {
                            languagePrefs.setLanguage(selected)
                        }
                    },
                    onRequestExit = { showClosingSplash = true },
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                        showSignInSuccessSplash = true
                    }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    AuthenticatedAppContent(
                        currentUserId = uid,
                        displayNameHint = user.displayName ?: "",
                        strings = strings,
                        currentLanguage = language,
                        openBloodRequestsFromNotification = openBloodRequestsFromNotification,
                        onConsumedBloodNotificationNav = onConsumedBloodNotificationNav,
                        onRequestExit = { showClosingSplash = true },
                        onSignOut = {
                            signOut(credentialManager = credentialManager, scope = scope)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = showOpeningSplash,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250))
            ) {
                SplashOverlayScreen(
                    imageResId = R.drawable.splash_opening,
                    contentDescription = strings.schoolFullName,
                    onTap = { showOpeningSplash = false }
                )
            }

            AnimatedVisibility(
                visible = showSignInSuccessSplash,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(250))
            ) {
                SplashOverlayScreen(
                    imageResId = R.drawable.splash_signin_success,
                    contentDescription = strings.signInTitle,
                    onTap = { showSignInSuccessSplash = false }
                )
            }

            AnimatedVisibility(
                visible = showClosingSplash,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(150))
            ) {
                SplashOverlayScreen(
                    imageResId = R.drawable.splash_closing,
                    contentDescription = strings.exitApp
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAppContent(
    currentUserId: String,
    displayNameHint: String,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    openBloodRequestsFromNotification: Boolean,
    onConsumedBloodNotificationNav: () -> Unit,
    onRequestExit: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: AlumniViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AlumniViewModel(
                    appContext = app,
                    repository = AlumniRepository(db),
                    languagePreferences = LanguagePreferences(app),
                    notificationPreferences = NotificationPreferences(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val ownProfileState by viewModel.ownProfileState.collectAsStateWithLifecycle()
    val ownPrivatePhone by viewModel.ownPrivatePhone.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()
    var editingRejected by remember { mutableStateOf(false) }

    when (val state = ownProfileState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorBannerCard(message = state.message)
                    Spacer(Modifier.padding(8.dp))
                    Button(onClick = onSignOut) {
                        Text(strings.signOut)
                    }
                }
            }
        }
        is UiState.Success -> {
            val profile = state.data
            if (profile == null || (profile.isRejected && editingRejected)) {
                RegistrationScreen(
                    strings = strings,
                    currentLanguage = currentLanguage,
                    onSelectLanguage = { viewModel.setLanguage(it) },
                    initialName = displayNameHint,
                    existingRejectedProfile = profile?.takeIf { it.isRejected },
                    existingPrivatePhone = ownPrivatePhone,
                    isSubmitting = isMutating,
                    onSubmit = { name, phone, hidePhone, batch, bg, prof, city, country ->
                        if (profile != null && profile.isRejected) {
                            viewModel.updateProfile(
                                existing = profile,
                                name = name,
                                rawPhone = phone,
                                hidePhone = hidePhone,
                                sscBatch = batch,
                                bloodGroup = bg,
                                profession = prof,
                                city = city,
                                country = country,
                                resubmitIfRejected = true
                            )
                            editingRejected = false
                        } else {
                            viewModel.registerProfile(name, phone, hidePhone, batch, bg, prof, city, country)
                        }
                    },
                    onSignOut = onSignOut
                )
            } else if (profile.status == ApprovalStatus.PENDING) {
                LaunchedEffect(profile.uid) {
                    viewModel.activatePendingOwnProfile()
                }
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (profile.isRejected) {
                PendingApprovalScreen(
                    profile = profile,
                    strings = strings,
                    currentLanguage = currentLanguage,
                    onSelectLanguage = { viewModel.setLanguage(it) },
                    onEditRejected = { editingRejected = true },
                    onRequestExit = onRequestExit,
                    onSignOut = onSignOut
                )
            } else {
                ApprovedMainShell(
                    profile = profile,
                    privatePhone = ownPrivatePhone,
                    viewModel = viewModel,
                    strings = strings,
                    currentLanguage = currentLanguage,
                    openBloodRequestsFromNotification = openBloodRequestsFromNotification,
                    onConsumedBloodNotificationNav = onConsumedBloodNotificationNav,
                    onRequestExit = onRequestExit,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApprovedMainShell(
    profile: UserProfile,
    privatePhone: String?,
    viewModel: AlumniViewModel,
    strings: AppStrings,
    currentLanguage: AppLanguage,
    openBloodRequestsFromNotification: Boolean,
    onConsumedBloodNotificationNav: () -> Unit,
    onRequestExit: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val alumniState by viewModel.approvedAlumniState.collectAsStateWithLifecycle()
    val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
    val bloodRequestsState by viewModel.bloodRequestsState.collectAsStateWithLifecycle()
    val eventsState by viewModel.eventsState.collectAsStateWithLifecycle()
    val fundsState by viewModel.fundsState.collectAsStateWithLifecycle()
    val teachersState by viewModel.teachersState.collectAsStateWithLifecycle()
    val approvedGalleryState by viewModel.approvedGalleryState.collectAsStateWithLifecycle()
    val myGalleryUploadsState by viewModel.myGalleryUploadsState.collectAsStateWithLifecycle()
    val pendingGalleryState by viewModel.pendingGalleryState.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerTransactionsState.collectAsStateWithLifecycle()
    val branding by viewModel.brandingState.collectAsStateWithLifecycle()
    val appUpdate by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val bloodAlertsEnabled by viewModel.bloodAlertsEnabled.collectAsStateWithLifecycle()
    val customAlertGroups by viewModel.customAlertGroups.collectAsStateWithLifecycle()
    val hasAvailableUpdate = appUpdate != null && (appUpdate?.versionCode ?: 0L) > BuildConfig.VERSION_CODE

    var selectedCampaign by remember { mutableStateOf<FundCampaign?>(null) }
    var openBloodRequestInitially by remember { mutableStateOf(false) }
    var openBloodRequestsTabInitially by remember { mutableStateOf(false) }
    var openFundsTabInitially by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(openBloodRequestsFromNotification) {
        if (openBloodRequestsFromNotification) {
            openBloodRequestInitially = false
            openBloodRequestsTabInitially = true
            navigateToTab(ROUTE_BLOOD_BANK)
            onConsumedBloodNotificationNav()
        }
    }

    val resolvedSchoolName = if (currentLanguage == AppLanguage.BENGALI) {
        branding?.schoolNameBn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    } else {
        branding?.schoolNameEn?.ifBlank { strings.schoolFullName } ?: strings.schoolFullName
    }

    val tabs = listOf(
        BottomTabItem(ROUTE_HOME, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        BottomTabItem(ROUTE_DIRECTORY, strings.navDirectory, Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_directory"),
        BottomTabItem(ROUTE_BLOOD_BANK, strings.navBloodBank, Icons.Rounded.Bloodtype, Icons.Outlined.Bloodtype, "nav_blood_bank"),
        BottomTabItem(ROUTE_EVENTS_FUND, strings.navEventsFund, Icons.Rounded.Event, Icons.Outlined.Event, "nav_events_fund"),
        BottomTabItem(ROUTE_PROFILE, strings.navProfile, Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, "nav_profile")
    )
    val isTopLevelTab = tabs.any { it.route == currentRoute }

    BackHandler(enabled = currentRoute == ROUTE_HOME) {
        showExitDialog = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                if (currentRoute == ROUTE_HOME) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            AppLogoImage(
                                logoDataOrUrl = branding?.logoDataOrUrl,
                                contentDescription = strings.appLogoLabel,
                                size = 119.dp,
                                modifier = Modifier.testTag("home_topbar_school_logo")
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LanguageSelectorButtons(
                                        currentLanguage = currentLanguage,
                                        onSelectLanguage = { viewModel.setLanguage(it) },
                                        compact = true
                                    )
                                    IconButton(
                                        onClick = { showExitDialog = true },
                                        modifier = Modifier.testTag("topbar_exit_app_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                            contentDescription = strings.exitApp,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = resolvedSchoolName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else if (isTopLevelTab) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppLogoImage(
                                    logoDataOrUrl = branding?.logoDataOrUrl,
                                    contentDescription = strings.appLogoLabel,
                                    size = 34.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.appTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = resolvedSchoolName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        },
                        actions = {
                            LanguageSelectorButtons(
                                currentLanguage = currentLanguage,
                                onSelectLanguage = { viewModel.setLanguage(it) },
                                compact = true
                            )
                            IconButton(
                                onClick = { showExitDialog = true },
                                modifier = Modifier.testTag("topbar_exit_app_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                    contentDescription = strings.exitApp,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        windowInsets = WindowInsets.statusBars
                    )
                }
            },
            bottomBar = {
                if (isTopLevelTab && !useNavRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == ROUTE_BLOOD_BANK) {
                                        openBloodRequestInitially = false
                                        openBloodRequestsTabInitially = false
                                    }
                                    if (tab.route == ROUTE_EVENTS_FUND) openFundsTabInitially = false
                                    navigateToTab(tab.route)
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.route == ROUTE_PROFILE && hasAvailableUpdate) {
                                                Badge()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevelTab && useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab.route == ROUTE_BLOOD_BANK) {
                                        openBloodRequestInitially = false
                                        openBloodRequestsTabInitially = false
                                    }
                                    if (tab.route == ROUTE_EVENTS_FUND) openFundsTabInitially = false
                                    navigateToTab(tab.route)
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab.route == ROUTE_PROFILE && hasAvailableUpdate) {
                                                Badge()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            currentUser = profile,
                            branding = branding,
                            noticesState = noticesState,
                            strings = strings,
                            onNavigateUrgentBlood = {
                                openBloodRequestInitially = true
                                navigateToTab(ROUTE_BLOOD_BANK)
                            },
                            onNavigateDonation = {
                                openFundsTabInitially = true
                                navigateToTab(ROUTE_EVENTS_FUND)
                            },
                            onNavigateTeachers = {
                                navController.navigate(ROUTE_TEACHERS)
                            },
                            onNavigateGallery = {
                                navController.navigate(ROUTE_GALLERY)
                            },
                            onNavigateAbout = {
                                navController.navigate(ROUTE_ABOUT)
                            },
                            onShareFeedPost = { title, desc, cat ->
                                viewModel.createNotice(title, desc, cat, false, profile.name)
                            },
                            onUpdateNotice = { id, title, desc, cat, pin ->
                                viewModel.updateNotice(id, title, desc, cat, pin)
                            },
                            onDeleteNotice = { viewModel.deleteNotice(it) }
                        )
                    }
                    composable(ROUTE_DIRECTORY) {
                        DirectoryScreen(
                            currentUser = profile,
                            alumniState = alumniState,
                            strings = strings,
                            onAdminUpdateUser = { uid, name, phone, hide, batch, bg, prof, city, country, role, status ->
                                viewModel.adminUpdateUserProfile(uid, name, phone, hide, batch, bg, prof, city, country, role, status)
                            },
                            onAdminDeleteUser = { viewModel.deleteUser(it) }
                        )
                    }
                    composable(ROUTE_BLOOD_BANK) {
                        BloodBankScreen(
                            currentUser = profile,
                            donorsState = alumniState,
                            requestsState = bloodRequestsState,
                            strings = strings,
                            bloodAlertsEnabled = bloodAlertsEnabled,
                            subscribedAlertGroups = customAlertGroups,
                            openRequestFormInitially = openBloodRequestInitially,
                            openRequestsTabInitially = openBloodRequestsTabInitially,
                            onToggleBloodAlerts = { viewModel.setBloodAlertsEnabled(it) },
                            onCreateRequest = { patient, bg, targets, hospital, phone, bags ->
                                viewModel.createBloodRequest(patient, bg, targets, hospital, phone, bags, profile.name)
                            },
                            onMarkFulfilled = { viewModel.markBloodRequestFulfilled(it) },
                            onUpdateRequest = { id, patient, bg, hospital, phone, bags, status ->
                                viewModel.updateBloodRequest(id, patient, bg, hospital, phone, bags, status)
                            },
                            onDeleteRequest = { viewModel.deleteBloodRequest(it) }
                        )
                    }
                    composable(ROUTE_EVENTS_FUND) {
                        EventsFundScreen(
                            currentUserId = profile.uid,
                            isSuperAdmin = profile.isSuperAdmin,
                            eventsState = eventsState,
                            fundsState = fundsState,
                            strings = strings,
                            openFundsTabInitially = openFundsTabInitially,
                            onToggleGoing = { viewModel.toggleEventRegistration(it) },
                            onOpenLedger = { campaign ->
                                selectedCampaign = campaign
                                viewModel.selectCampaignForLedger(campaign.id)
                                navController.navigate(ROUTE_LEDGER)
                            },
                            onUpdateEvent = { id, title, date, loc, fee, desc ->
                                viewModel.updateEvent(id, title, date, loc, fee, desc)
                            },
                            onDeleteEvent = { viewModel.deleteEvent(it) },
                            onUpdateCampaign = { id, title, target, status ->
                                viewModel.updateFundCampaign(id, title, target, status)
                            },
                            onDeleteCampaign = { viewModel.deleteFundCampaign(it) }
                        )
                    }
                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            profile = profile,
                            privatePhone = privatePhone,
                            appUpdate = appUpdate,
                            bloodAlertsEnabled = bloodAlertsEnabled,
                            subscribedAlertGroups = customAlertGroups,
                            strings = strings,
                            currentLanguage = currentLanguage,
                            onSelectLanguage = { viewModel.setLanguage(it) },
                            onToggleBloodAlerts = { viewModel.setBloodAlertsEnabled(it) },
                            onToggleAlertBloodGroup = { viewModel.toggleAlertBloodGroup(it, profile.bloodGroup) },
                            onToggleHidePhone = { hide ->
                                viewModel.updatePrivacyAndDonation(privatePhone, hide, null)
                            },
                            onUpdateDonationDate = { ts ->
                                viewModel.updatePrivacyAndDonation(privatePhone, profile.hidePhone, ts)
                            },
                            onSaveProfile = { name, phone, hide, batch, bg, prof, city, country ->
                                viewModel.updateProfile(profile, name, phone, hide, batch, bg, prof, city, country)
                            },
                            onUpdateProfilePhoto = { photoData ->
                                viewModel.updateOwnProfilePhoto(photoData)
                            },
                            onOpenManagementPanel = {
                                navController.navigate(ROUTE_MANAGEMENT)
                            },
                            onOpenAbout = {
                                navController.navigate(ROUTE_ABOUT)
                            },
                            onRequestExit = onRequestExit,
                            onSignOut = onSignOut
                        )
                    }
                    composable(ROUTE_GALLERY) {
                        BatchGalleryScreen(
                            currentUser = profile,
                            approvedGalleryState = approvedGalleryState,
                            myUploadsState = myGalleryUploadsState,
                            strings = strings,
                            onUploadPhoto = { batch, caption, img ->
                                viewModel.uploadBatchGalleryPhoto(
                                    sscBatch = batch,
                                    caption = caption,
                                    imageDataOrUrl = img,
                                    uploadedByName = profile.name,
                                    isSuperAdmin = profile.isSuperAdmin
                                )
                            },
                            onDeletePhoto = { viewModel.deleteBatchGalleryPhoto(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_ABOUT) {
                        AboutScreen(
                            branding = branding,
                            strings = strings,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_TEACHERS) {
                        TeachersScreen(
                            isSuperAdmin = profile.isSuperAdmin,
                            teachersState = teachersState,
                            strings = strings,
                            onUpdateTeacher = { id, n, s, t, p, st ->
                                viewModel.updateTeacher(id, n, s, t, p, st)
                            },
                            onDeleteTeacher = { viewModel.deleteTeacher(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_LEDGER) {
                        LedgerScreen(
                            campaign = selectedCampaign,
                            isSuperAdmin = profile.isSuperAdmin,
                            transactionsState = ledgerState,
                            strings = strings,
                            onDeleteTransaction = { tx ->
                                selectedCampaign?.id?.let { cid ->
                                    viewModel.deleteFundTransaction(cid, tx)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(ROUTE_MANAGEMENT) {
                        ManagementPanelScreen(
                            branding = branding,
                            appUpdate = appUpdate,
                            approvedAlumniState = alumniState,
                            fundsState = fundsState,
                            teachersState = teachersState,
                            pendingGalleryState = pendingGalleryState,
                            strings = strings,
                            onSaveBranding = { bn, en, sBn, sEn, logo ->
                                viewModel.updateAppBranding(bn, en, sBn, sEn, logo)
                            },
                            onPushAppUpdate = { code, verName, notes, url, mandatory ->
                                viewModel.pushAppUpdate(code, verName, notes, url, mandatory, profile.name)
                            },
                            onClearAppUpdate = { viewModel.clearAppUpdate() },
                            onCreateNotice = { t, d, c, pin ->
                                viewModel.createNotice(t, d, c, pin, profile.name)
                            },
                            onCreateEvent = { t, dt, loc, fee, desc ->
                                viewModel.createEvent(t, dt, loc, fee, desc)
                            },
                            onCreateCampaign = { t, target ->
                                viewModel.createFundCampaign(t, target)
                            },
                            onAddTransaction = { cid, type, amt, desc, dt ->
                                viewModel.addFundTransaction(cid, type, amt, desc, dt, profile.name)
                            },
                            onCreateTeacher = { n, sub, ten, ph, st ->
                                viewModel.createTeacher(n, sub, ten, ph, st)
                            },
                            onUpdateTeacher = { id, n, sub, ten, ph, st ->
                                viewModel.updateTeacher(id, n, sub, ten, ph, st)
                            },
                            onDeleteTeacher = { viewModel.deleteTeacher(it) },
                            onSetModerator = { user, makeMod ->
                                viewModel.setBatchModerator(user, makeMod)
                            },
                            onApproveGalleryPhoto = { viewModel.approveBatchGalleryPhoto(it) },
                            onRejectGalleryPhoto = { viewModel.rejectBatchGalleryPhoto(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    if (showExitDialog) {
        ExitAppConfirmationDialog(
            strings = strings,
            onConfirmExit = {
                showExitDialog = false
                onRequestExit()
            },
            onDismiss = { showExitDialog = false }
        )
    }
}
