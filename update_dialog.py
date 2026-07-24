import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

shapes_dialog_code = """
                    if (showToolSettings == "shapes") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select Shape", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val shapes = listOf("rectangle", "square", "circle", "ellipse", "cube", "keyboard")
                        var rIdx = 0
                        while(rIdx < shapes.size) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                for(c in 0 until 3) {
                                    if (rIdx < shapes.size) {
                                        val s = shapes[rIdx]
                                        val isSel = viewModel.activeShapeType == s
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                                .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Gray, RoundedCornerShape(8.dp))
                                                .clickable { viewModel.activeShapeType = s }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(s.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                                        }
                                        rIdx++
                                    }
                                }
                            }
                        }
                    }
"""

content = content.replace("Text(\"Color\", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)", shapes_dialog_code.strip('\n') + "\n\n                    Text(\"Color\", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

