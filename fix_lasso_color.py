import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

lasso_settings = """                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = viewModel.lassoSolidLine,
                                onCheckedChange = { viewModel.lassoSolidLine = it },
                                modifier = Modifier.scale(0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Solid Lasso Line", fontSize = 14.sp)
                        }
                    }

                    if (showToolSettings == "laser") {"""

new_lasso_settings = """                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Switch(
                                checked = viewModel.lassoSolidLine,
                                onCheckedChange = { viewModel.lassoSolidLine = it },
                                modifier = Modifier.scale(0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Show Solid Lasso Line", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Lasso Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val swatches = listOf(
                            0xFF1E1B4B.toInt() to "Dark Navy",
                            0xFFDC2626.toInt() to "Red",
                            0xFF78350F.toInt() to "Brown",
                            0xFF6B21A8.toInt() to "Purple",
                            0xFF0284C7.toInt() to "Sky Blue",
                            0xFF3B82F6.toInt() to "Blue",
                            0xFF059669.toInt() to "Green",
                            0xFFF59E0B.toInt() to "Yellow"
                        )

                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            swatches.forEach { (colorVal, _) ->
                                val isSelected = viewModel.activeColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(colorVal))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            viewModel.activeColor = colorVal
                                        }
                                )
                            }
                        }
                    }

                    if (showToolSettings == "laser") {"""

content = content.replace(lasso_settings, new_lasso_settings)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

