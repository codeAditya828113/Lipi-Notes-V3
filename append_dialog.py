import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

dialog_code = """
    if (showToolSettings != null) {
        Dialog(onDismissRequest = { showToolSettings = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tool Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Text("Thickness", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(4f, 10f, 22f).forEach { width ->
                            val isSelected = viewModel.activeWidth == width
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { viewModel.activeWidth = width }
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                val dotSize = when (width) {
                                    4f -> 4.dp
                                    10f -> 8.dp
                                    else -> 16.dp
                                }
                                Box(modifier = Modifier.size(dotSize).background(Color.Black, CircleShape))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Grid of colors
                    val swatches = listOf(
                        0xFF1E1B4B.toInt(), 0xFFDC2626.toInt(), 0xFF78350F.toInt(),
                        0xFF6B21A8.toInt(), 0xFF0284C7.toInt(), 0xFF0D9488.toInt(),
                        0xFFEAB308.toInt(), 0xFFEA580C.toInt(), 0xFF000000.toInt()
                    )
                    
                    var index = 0
                    while(index < swatches.size) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for(i in 0 until 3) {
                                if (index < swatches.size) {
                                    val c = swatches[index]
                                    val isSel = viewModel.activeColor == c
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                            .clickable { viewModel.activeColor = c }
                                    )
                                    index++
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showToolSettings = null }, modifier = Modifier.fillMaxWidth()) {
                        Text("Done")
                    }
                }
            }
        }
    }
"""

# Insert before "if (showScribbleToTextDialog) {"
content = content.replace("    if (showScribbleToTextDialog) {", dialog_code + "\n    if (showScribbleToTextDialog) {")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

