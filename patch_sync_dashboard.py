import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_func = """@Composable
fun SyncDashboard(viewModel: NoteViewModel) {
    val logs by viewModel.syncLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {"""

new_func = """@Composable
fun SyncDashboard(viewModel: NoteViewModel) {
    val logs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val googleSignInClient = androidx.compose.runtime.remember {
        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
            .build()
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }
    
    var isSignedIn by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) != null)
    }
    
    val signInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                isSignedIn = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {"""

content = content.replace(old_func, new_func)

old_sync_button = """                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = { viewModel.syncWithGoogleDrive() },
                            modifier = Modifier.testTag("force_sync_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }"""

new_sync_button = """                    if (!isSignedIn) {
                        Button(onClick = { signInLauncher.launch(googleSignInClient.signInIntent) }) {
                            Text("Sign In with Google")
                        }
                    } else if (viewModel.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Button(
                            onClick = { viewModel.syncWithGoogleDrive() },
                            modifier = Modifier.testTag("force_sync_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }
                    }"""

content = content.replace(old_sync_button, new_sync_button)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
