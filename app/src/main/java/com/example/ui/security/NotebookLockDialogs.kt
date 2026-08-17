package com.example.ui.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.data.NoteEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * Dialog to set up Passcode and Biometrics for a notebook
 */
@Composable
fun SetNotebookPasscodeDialog(
    initialPasscode: String = "",
    notebookTitle: String = "Notebook",
    onDismiss: () -> Unit,
    onPasscodeSet: (passcode: String, biometricEnabled: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1 = enter pin, 2 = confirm pin
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isBiometricAvail = remember { BiometricHelper.isBiometricAvailable(context) }

    fun triggerShake(message: String) {
        errorMessage = message
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 15f,
                animationSpec = repeatable(
                    iterations = 4,
                    animation = tween(durationMillis = 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            shakeOffset.snapTo(0f)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .graphicsLayer { translationX = shakeOffset.value }
                    .testTag("set_notebook_passcode_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (step == 1) "Set Notebook Passcode" else "Confirm Passcode",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (step == 1) "Choose a 4-digit passcode for \"$notebookTitle\"" else "Re-enter the 4-digit passcode to verify",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // PIN Dots Indicator
                    val currentPinLength = if (step == 1) enteredPin.length else confirmedPin.length
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < currentPinLength
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFF4F46E5) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) Color(0xFF4F46E5) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Error message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Biometric Option Card (only on step 1)
                    if (step == 1 && isBiometricAvail) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { biometricEnabled = !biometricEnabled },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Biometric Unlock",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Use fingerprint or face recognition",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { biometricEnabled = it },
                                    modifier = Modifier.testTag("biometric_switch")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Numeric Keypad
                    NumericKeypad(
                        onDigitClick = { digit ->
                            errorMessage = null
                            if (step == 1) {
                                if (enteredPin.length < 4) {
                                    enteredPin += digit
                                    if (enteredPin.length == 4) {
                                        step = 2
                                    }
                                }
                            } else {
                                if (confirmedPin.length < 4) {
                                    confirmedPin += digit
                                    if (confirmedPin.length == 4) {
                                        if (confirmedPin == enteredPin) {
                                            onPasscodeSet(enteredPin, biometricEnabled)
                                            onDismiss()
                                        } else {
                                            triggerShake("Passcodes do not match. Try again.")
                                            confirmedPin = ""
                                        }
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            errorMessage = null
                            if (step == 1) {
                                if (enteredPin.isNotEmpty()) {
                                    enteredPin = enteredPin.dropLast(1)
                                }
                            } else {
                                if (confirmedPin.isNotEmpty()) {
                                    confirmedPin = confirmedPin.dropLast(1)
                                } else {
                                    step = 1
                                    enteredPin = ""
                                }
                            }
                        },
                        onClearClick = {
                            errorMessage = null
                            if (step == 1) {
                                enteredPin = ""
                            } else {
                                confirmedPin = ""
                                step = 1
                                enteredPin = ""
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dismiss / Cancel Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().testTag("cancel_set_passcode_btn")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unlock Notebook Dialog (supports PIN and Biometric)
 */
@Composable
fun UnlockNotebookDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember { context.findFragmentActivity() }
    val isBiometricAvail = remember { BiometricHelper.isBiometricAvailable(context) }

    fun triggerShake(message: String) {
        errorMessage = message
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 15f,
                animationSpec = repeatable(
                    iterations = 4,
                    animation = tween(durationMillis = 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            shakeOffset.snapTo(0f)
        }
    }

    fun startBiometricAuth() {
        if (activity != null && isBiometricAvail) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Unlock \"${note.title}\"",
                subtitle = "Touch fingerprint sensor to unlock this notebook",
                negativeButtonText = "Use Passcode",
                onSuccess = {
                    onSuccess()
                },
                onError = { _, _ ->
                    // Fall back to PIN entry gracefully
                },
                onFailed = {
                    triggerShake("Biometric not recognized. Try passcode.")
                }
            )
        }
    }

    // Auto prompt biometric authentication once when dialog opens
    LaunchedEffect(Unit) {
        delay(200)
        startBiometricAuth()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .graphicsLayer { translationX = shakeOffset.value }
                    .testTag("unlock_notebook_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Lock Badge Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE11D48), Color(0xFFF43F5E))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Notebook Protected",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "\"${note.title}\" is locked with passcode protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFFE11D48) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) Color(0xFFE11D48) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Error message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Keypad
                    NumericKeypad(
                        onDigitClick = { digit ->
                            errorMessage = null
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                if (enteredPin.length == 4) {
                                    if (enteredPin == note.pinCode) {
                                        onSuccess()
                                    } else {
                                        triggerShake("Incorrect passcode. Try again.")
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            errorMessage = null
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onClearClick = {
                            errorMessage = null
                            enteredPin = ""
                        },
                        extraBottomLeftAction = {
                            if (isBiometricAvail) {
                                IconButton(
                                    onClick = { startBiometricAuth() },
                                    modifier = Modifier.size(56.dp).testTag("biometric_auth_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Unlock with fingerprint",
                                        tint = Color(0xFF4F46E5),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().testTag("cancel_unlock_btn")
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Remove Lock Confirmation Dialog
 */
@Composable
fun RemoveNotebookLockDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember { context.findFragmentActivity() }
    val isBiometricAvail = remember { BiometricHelper.isBiometricAvailable(context) }

    fun triggerShake(message: String) {
        errorMessage = message
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 15f,
                animationSpec = repeatable(
                    iterations = 4,
                    animation = tween(durationMillis = 50, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            shakeOffset.snapTo(0f)
        }
    }

    fun startBiometricAuth() {
        if (activity != null && isBiometricAvail) {
            BiometricHelper.authenticate(
                activity = activity,
                title = "Remove Lock for \"${note.title}\"",
                subtitle = "Verify your identity to remove passcode protection",
                negativeButtonText = "Use Passcode",
                onSuccess = {
                    onUnlocked()
                    onDismiss()
                },
                onError = { _, _ -> },
                onFailed = {
                    triggerShake("Biometric not recognized. Enter passcode.")
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .graphicsLayer { translationX = shakeOffset.value }
                    .testTag("remove_notebook_lock_dialog"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Remove Passcode Lock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Enter current passcode or use biometrics to unlock \"${note.title}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // PIN Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    NumericKeypad(
                        onDigitClick = { digit ->
                            errorMessage = null
                            if (enteredPin.length < 4) {
                                enteredPin += digit
                                if (enteredPin.length == 4) {
                                    if (enteredPin == note.pinCode) {
                                        onUnlocked()
                                        onDismiss()
                                    } else {
                                        triggerShake("Incorrect passcode.")
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            errorMessage = null
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                            }
                        },
                        onClearClick = {
                            errorMessage = null
                            enteredPin = ""
                        },
                        extraBottomLeftAction = {
                            if (isBiometricAvail) {
                                IconButton(
                                    onClick = { startBiometricAuth() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Unlock with fingerprint",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * Reusable high-performance numeric keypad for lock screens
 */
@Composable
private fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    extraBottomLeftAction: (@Composable () -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (digit in row) {
                    KeypadButton(
                        text = digit,
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }

        // Bottom row: [Extra/Biometric or Clear, 0, Backspace]
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (extraBottomLeftAction != null) {
                    extraBottomLeftAction()
                } else {
                    IconButton(
                        onClick = onClearClick,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Text(
                            text = "C",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") }
            )

            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onBackspaceClick,
                    modifier = Modifier.size(56.dp).testTag("keypad_backspace")
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .size(64.dp)
            .testTag("keypad_btn_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
