import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Let's replace only the "pen", "fountain_pen" check block.
old_magic_block = """                                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter", "pencil")) {
                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
                    }"""

new_magic_block = """                                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint", "highlighter", "pencil")) {
                        if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ink Flow ${viewModel.inkFlow.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Slider(
                                value = viewModel.inkFlow,
                                onValueChange = { viewModel.inkFlow = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pressure Sensitivity ${viewModel.pressureSensitivity.toInt()}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Slider(
                                value = viewModel.pressureSensitivity,
                                onValueChange = { viewModel.pressureSensitivity = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        var magicSettingsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { magicSettingsExpanded = !magicSettingsExpanded }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Shape Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Icon(
                                imageVector = if (magicSettingsExpanded) androidx.compose.material.icons.Icons.Default.KeyboardArrowUp else androidx.compose.material.icons.Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Shape Settings",
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
                                Text("Draw in a straight line", fontSize = 14.sp)
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
                                Text("Ink Tint (Fill Color Opacity) ${(viewModel.fillShapeOpacity * 100).toInt()}%", fontSize = 12.sp)
                                androidx.compose.material3.Slider(
                                    value = viewModel.fillShapeOpacity,
                                    onValueChange = { viewModel.fillShapeOpacity = it },
                                    valueRange = 0.05f..1f,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }"""

content = content.replace(old_magic_block, new_magic_block)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

