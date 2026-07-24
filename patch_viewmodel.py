import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

old_sync = """    fun syncWithGoogleDrive() {
        viewModelScope.launch {
            isSyncing = true
            logSyncEvent("Initiating Cloud Sync pipeline with Google Drive APIs...")
            kotlinx.coroutines.delay(1000)

            logSyncEvent("Authorizing OAuth Token with secure Google Cloud Project...")
            kotlinx.coroutines.delay(800)

            logSyncEvent("Scanning Notein local repository for modified files...")
            val notes = allNotes.value
            val unsyncedCount = notes.count { !it.isSynced }

            logSyncEvent("Found $unsyncedCount modified notes pending automated Google Drive backup.")
            if (unsyncedCount > 0) {
                kotlinx.coroutines.delay(1200)
                // Mark all local notes as synced in database
                withContext(Dispatchers.IO) {
                    notes.forEach { note ->
                        if (!note.isSynced) {
                            repository.insertNote(note.copy(isSynced = true))
                        }
                    }
                }
                logSyncEvent("Backup upload complete! Successfully transferred $unsyncedCount notes.")
            } else {
                logSyncEvent("All local files matching remote Google Drive index. No upload needed.")
            }

            isSyncing = false
            lastSyncTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            logSyncEvent("Database synchronization cycle finished successfully.")
        }
    }"""

new_sync = """    fun syncWithGoogleDrive() {
        viewModelScope.launch {
            isSyncing = true
            logSyncEvent("Initiating Cloud Sync pipeline with Google Drive APIs...")
            
            val drive = GoogleDriveBackupHelper.getDriveService(application)
            if (drive == null) {
                logSyncEvent("Error: Not signed in to Google. Please sign in first.")
                isSyncing = false
                return@launch
            }

            logSyncEvent("Scanning local repository for modified files...")
            val notes = allNotes.value
            val unsyncedCount = notes.count { !it.isSynced }

            logSyncEvent("Found $unsyncedCount modified notes pending automated Google Drive backup.")
            if (unsyncedCount > 0) {
                try {
                    withContext(Dispatchers.IO) {
                        notes.forEach { note ->
                            if (!note.isSynced) {
                                val fileMetadata = com.google.api.services.drive.model.File()
                                fileMetadata.name = "Notein_Backup_${note.title}.txt"
                                fileMetadata.mimeType = "text/plain"
                                
                                val contentString = "Title: ${note.title}\\n\\nContent:\\n${note.content}\\n\\nScribbleData: ${note.drawingData}"
                                val fileContent = com.google.api.client.http.ByteArrayContent.fromString(
                                    "text/plain", 
                                    contentString
                                )
                                
                                drive.files().create(fileMetadata, fileContent)
                                    .setFields("id")
                                    .execute()
                                
                                repository.insertNote(note.copy(isSynced = true))
                            }
                        }
                    }
                    logSyncEvent("Backup upload complete! Successfully transferred $unsyncedCount notes.")
                } catch (e: Exception) {
                    logSyncEvent("Backup failed: ${e.message}")
                    android.util.Log.e("DriveSync", "Error uploading", e)
                }
            } else {
                logSyncEvent("All local files matching remote Google Drive index. No upload needed.")
            }

            isSyncing = false
            lastSyncTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            logSyncEvent("Database synchronization cycle finished successfully.")
        }
    }"""

content = content.replace(old_sync, new_sync)

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)
