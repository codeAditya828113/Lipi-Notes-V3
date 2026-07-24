import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# We need to add the laser settings UI.
# Find the end of "Thickness" row:
#                                 Box(modifier = Modifier.size(dotSize).background(Color.Black, CircleShape))
#                             }
#                         }
#                     }
#
#                     Spacer(modifier = Modifier.height(16.dp))
#
#                     if (showToolSettings == "shapes") {

settings_inject = """                    }

                    if (showToolSettings == "laser") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Invisible After", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("0.5s", fontSize = 12.sp, color = Color.Gray)
                            androidx.compose.material3.Slider(
                                value = viewModel.laserInvisibleAfter,
                                onValueChange = { viewModel.laserInvisibleAfter = it },
                                valueRange = 0.5f..5.0f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            Text("5s", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text("${String.format("%.1f", viewModel.laserInvisibleAfter)}s", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            androidx.compose.material3.Checkbox(
                                checked = viewModel.laserDisappearOnLift,
                                onCheckedChange = { viewModel.laserDisappearOnLift = it }
                            )
                            Text("Disappear Upon Lifting Stylus", fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showToolSettings == "shapes") {"""

content = content.replace('                    }\n\n                    Spacer(modifier = Modifier.height(16.dp))\n\n                    if (showToolSettings == "shapes") {', settings_inject)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
