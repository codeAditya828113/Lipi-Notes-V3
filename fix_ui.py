import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_code = """                                        rIdx++
                                    }
                                }
                            }
                        }
                    }

                    if (showToolSettings == "laser") {"""

new_code = """                                        rIdx++
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Switch(
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
                            Slider(
                                value = viewModel.fillShapeOpacity,
                                onValueChange = { viewModel.fillShapeOpacity = it },
                                valueRange = 0.05f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (showToolSettings == "laser") {"""

content = content.replace(old_code, new_code)

# Add import Switch if not present
if "import androidx.compose.material3.Switch" not in content:
    content = content.replace("import androidx.compose.material3.Slider", "import androidx.compose.material3.Slider\nimport androidx.compose.material3.Switch")

if "import androidx.compose.ui.draw.scale" not in content:
    content = content.replace("import androidx.compose.ui.draw.clip", "import androidx.compose.ui.draw.clip\nimport androidx.compose.ui.draw.scale")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

