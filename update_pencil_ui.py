import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_magic_settings_check = """if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter")) {"""
new_magic_settings_check = """if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter", "pencil")) {"""

content = content.replace(old_magic_settings_check, new_magic_settings_check)

old_magic_settings_content = """                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row("""

new_magic_settings_content = """                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        if (showToolSettings == "pencil") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.pencilRainbowEnabled,
                                    onCheckedChange = { viewModel.pencilRainbowEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rainbow", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row("""

content = content.replace(old_magic_settings_content, new_magic_settings_content)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

