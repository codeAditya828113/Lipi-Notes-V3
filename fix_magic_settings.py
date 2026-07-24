import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

new_magic_settings = """                    if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter")) {
                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { magicSettingsExpanded = !magicSettingsExpanded }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Magic Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Icon(
                                imageVector = if (magicSettingsExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Magic Settings",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (magicSettingsExpanded) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.drawStraightLines,
                                    onCheckedChange = { viewModel.drawStraightLines = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Draw Straight Lines", fontSize = 14.sp)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.smartShapesEnabled,
                                    onCheckedChange = { viewModel.smartShapesEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto Shape Recognition", fontSize = 14.sp)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Switch(
                                    checked = viewModel.fillShapeEnabled,
                                    onCheckedChange = { viewModel.fillShapeEnabled = it },
                                    modifier = Modifier.scale(0.8f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fill Shape With Color", fontSize = 14.sp)
                            }

                            if (viewModel.fillShapeEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Shape fill color opacity: ${(viewModel.fillShapeOpacity * 100).toInt()}%", fontSize = 12.sp)
                                androidx.compose.material3.Slider(
                                    value = viewModel.fillShapeOpacity,
                                    onValueChange = { viewModel.fillShapeOpacity = it },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (showToolSettings == "shapes") {"""

content = content.replace('                    if (showToolSettings == "shapes") {', new_magic_settings)

if "import androidx.compose.material.icons.filled.KeyboardArrowDown" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Edit", "import androidx.compose.material.icons.filled.Edit\nimport androidx.compose.material.icons.filled.KeyboardArrowDown\nimport androidx.compose.material.icons.filled.KeyboardArrowUp")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

