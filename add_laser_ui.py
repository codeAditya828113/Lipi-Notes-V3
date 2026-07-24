import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

laser_ui = """                    if (showToolSettings == "laser") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Laser Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // Line Laser
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.laserMode == "line") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.laserMode == "line") MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.laserMode = "line" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Line Laser", fontSize = 14.sp, fontWeight = if (viewModel.laserMode == "line") FontWeight.Bold else FontWeight.Normal)
                            }
                            // Spot Laser
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (viewModel.laserMode == "spot") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .border(1.dp, if (viewModel.laserMode == "spot") MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.laserMode = "spot" }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Spot Laser", fontSize = 14.sp, fontWeight = if (viewModel.laserMode == "spot") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Invisible After", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            androidx.compose.material3.Switch(
                                checked = viewModel.laserDisappearEnabled,
                                onCheckedChange = { viewModel.laserDisappearEnabled = it }
                            )
                        }
                        
                        if (viewModel.laserDisappearEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Delay: ${viewModel.laserDisappearDelay / 1000f}s", fontSize = 12.sp, modifier = Modifier.width(80.dp))
                                androidx.compose.material3.Slider(
                                    value = viewModel.laserDisappearDelay.toFloat(),
                                    onValueChange = { viewModel.laserDisappearDelay = it.toLong() },
                                    valueRange = 1000f..10000f,
                                    steps = 8,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)"""

content = content.replace('                    Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)', laser_ui)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
