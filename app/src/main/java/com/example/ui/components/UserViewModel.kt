package com.example.ui.components

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State representing the authenticated user profile retrieved from Google Auth / account session,
 * including structured error handling and graceful fallbacks for missing or unavailable data.
 */
data class UserProfileState(
    val isSignedIn: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val provider: String = "Google",
    val isLoading: Boolean = false,
    val authError: String? = null,
    val isDataUnavailable: Boolean = false
) {
    /**
     * Flag indicating if an authentication or data retrieval error is active.
     */
    val hasError: Boolean
        get() = !authError.isNullOrBlank()

    /**
     * Extracts initials (e.g. "RC" for "Ramprit Choudhary") or fallback for avatar rendering.
     */
    val initials: String
        get() {
            if (displayName.isBlank() || displayName.equals("Guest User", ignoreCase = true) || displayName.equals("Connected User", ignoreCase = true)) {
                return if (email.isNotBlank()) email.take(2).uppercase() else "GU"
            }
            val parts = displayName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            return parts.mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifEmpty { "GU" }
        }

    /**
     * Extracts first name for greetings (e.g. "Ramprit" or empty if guest).
     */
    val firstName: String
        get() {
            if (!isSignedIn || displayName.isBlank() || displayName.equals("Guest User", ignoreCase = true) || displayName.equals("Google Account", ignoreCase = true) || displayName.equals("Connected User", ignoreCase = true)) {
                return ""
            }
            return displayName.trim().split("\\s+".toRegex()).firstOrNull().orEmpty()
        }

    /**
     * User-facing readable name for the profile card with graceful fallbacks.
     */
    val formattedName: String
        get() {
            if (displayName.isNotBlank() && !displayName.equals("Guest User", ignoreCase = true)) {
                return displayName
            }
            return if (isSignedIn) "Connected User" else "Guest User"
        }

    /**
     * User-facing readable email/status for the profile card with graceful fallbacks.
     */
    val formattedEmail: String
        get() {
            if (email.isNotBlank()) return email
            if (hasError) return "Auth unavailable (Offline)"
            return if (isSignedIn) "Account Connected" else "Offline (On-Device)"
        }
}

/**
 * ViewModel managing user authentication state, profile data retrieval from Google Auth,
 * robust error state handling, and dynamic fallback injection into UI components.
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val _userProfile = MutableStateFlow(UserProfileState(isLoading = true))
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    init {
        refreshUserProfile()
    }

    /**
     * Refreshes the user profile directly from GoogleSignIn and backup preferences,
     * catching exceptions to prevent broken UI states.
     */
    fun refreshUserProfile() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val lastAccount: GoogleSignInAccount? = try {
                    GoogleDriveBackupHelper.getLastSignedInAccount(context)
                } catch (e: Exception) {
                    null
                }

                val signedIn = GoogleDriveBackupHelper.isSignedIn(context) || lastAccount != null

                val name = if (lastAccount?.displayName != null && lastAccount.displayName!!.isNotBlank()) {
                    lastAccount.displayName!!
                } else {
                    GoogleDriveBackupHelper.getSavedAccountName(context)
                }

                val email = if (lastAccount?.email != null && lastAccount.email!!.isNotBlank()) {
                    lastAccount.email!!
                } else {
                    GoogleDriveBackupHelper.getSavedAccountEmail(context)
                }

                val photo = if (lastAccount?.photoUrl != null && lastAccount.photoUrl.toString().isNotBlank()) {
                    lastAccount.photoUrl.toString()
                } else {
                    GoogleDriveBackupHelper.getSavedPhotoUrl(context)
                }

                val provider = GoogleDriveBackupHelper.getSavedAccountProvider(context)
                val isDataUnavailable = signedIn && name.isBlank() && email.isBlank()

                _userProfile.update {
                    it.copy(
                        isSignedIn = signedIn,
                        displayName = name.ifBlank { if (signedIn) "Connected User" else "Guest User" },
                        email = email,
                        photoUrl = photo,
                        provider = provider,
                        isLoading = false,
                        authError = null,
                        isDataUnavailable = isDataUnavailable
                    )
                }
            } catch (e: Exception) {
                _userProfile.update {
                    it.copy(
                        isLoading = false,
                        authError = "Could not retrieve account details: ${e.localizedMessage ?: "Unknown error"}",
                        isDataUnavailable = true
                    )
                }
            }
        }
    }

    /**
     * Handles sign-in account callback from Google Sign-In intent.
     */
    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        val context = getApplication<Application>()
        val name = account.displayName.orEmpty()
        val email = account.email.orEmpty()
        val photo = account.photoUrl?.toString().orEmpty()
        val provider = "Google"

        GoogleDriveBackupHelper.saveConnectedAccount(
            context = context,
            name = name,
            email = email,
            photoUrl = photo,
            provider = provider
        )

        _userProfile.update {
            it.copy(
                isSignedIn = true,
                displayName = name.ifBlank { email.split("@").firstOrNull()?.replaceFirstChar { c -> c.uppercase() } ?: "Google User" },
                email = email,
                photoUrl = photo,
                provider = provider,
                isLoading = false,
                authError = null,
                isDataUnavailable = false
            )
        }
    }

    /**
     * Handles Google Auth failures with user-friendly diagnostics and fallback state.
     */
    fun onGoogleSignInFailure(error: Throwable?) {
        val userFriendlyMessage = when (error) {
            is ApiException -> {
                when (error.statusCode) {
                    7 -> "Network connection failed. Using offline local storage."
                    12501 -> "Sign-in was cancelled."
                    12500 -> "Google Play Services authentication issue. Please check device account settings."
                    12502 -> "Sign-in operation is already in progress."
                    else -> "Google Sign-in failed (Code ${error.statusCode}). Falling back to local offline mode."
                }
            }
            null -> "Google Sign-in was not completed."
            else -> error.localizedMessage ?: "Google Sign-in encountered an error."
        }

        _userProfile.update {
            it.copy(
                isLoading = false,
                authError = userFriendlyMessage,
                isDataUnavailable = it.displayName.isBlank() && it.email.isBlank()
            )
        }
    }

    /**
     * Dismisses the active error state.
     */
    fun dismissError() {
        _userProfile.update { it.copy(authError = null) }
    }

    /**
     * Saves manual or OAuth connected account details with validation.
     */
    fun updateConnectedAccount(name: String?, email: String?, photoUrl: String? = null, provider: String = "Google") {
        val context = getApplication<Application>()
        val finalName = name?.trim().orEmpty()
        val finalEmail = email?.trim().orEmpty()

        GoogleDriveBackupHelper.saveConnectedAccount(
            context = context,
            name = finalName,
            email = finalEmail,
            photoUrl = photoUrl,
            provider = provider
        )

        _userProfile.update {
            it.copy(
                isSignedIn = true,
                displayName = finalName.ifBlank { finalEmail.split("@").firstOrNull()?.replaceFirstChar { c -> c.uppercase() } ?: "Connected User" },
                email = finalEmail,
                photoUrl = photoUrl.orEmpty(),
                provider = provider,
                isLoading = false,
                authError = null,
                isDataUnavailable = false
            )
        }
    }

    /**
     * Clears authentication and disconnects account gracefully.
     */
    fun signOut() {
        val context = getApplication<Application>()
        try {
            GoogleDriveBackupHelper.getSignInClient(context).signOut()
        } catch (_: Exception) {}

        val prefs = context.getSharedPreferences("google_drive_backup_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("is_signed_in", false)
            .putString("account_name", "")
            .putString("account_email", "")
            .putString("account_photo_url", "")
            .apply()

        _userProfile.update {
            UserProfileState(
                isSignedIn = false,
                displayName = "Guest User",
                email = "",
                photoUrl = "",
                provider = "Google",
                isLoading = false,
                authError = null,
                isDataUnavailable = false
            )
        }
    }
}
