import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

lasso_code = """                    if (showToolSettings == "lasso") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectPen,
                                    onCheckedChange = { viewModel.lassoSelectPen = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pen", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectShape,
                                    onCheckedChange = { viewModel.lassoSelectShape = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Shape", fontSize = 14.sp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectHighlighter,
                                    onCheckedChange = { viewModel.lassoSelectHighlighter = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Highlighter", fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectText,
                                    onCheckedChange = { viewModel.lassoSelectText = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Text", fontSize = 14.sp)
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.Checkbox(
                                    checked = viewModel.lassoSelectImage,
                                    onCheckedChange = { viewModel.lassoSelectImage = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Image", fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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

content = content.replace('                    if (showToolSettings == "laser") {', lasso_code)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

