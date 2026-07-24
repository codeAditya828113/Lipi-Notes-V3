import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_title = """                    Text("Tool Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    
                    Text("Thickness", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)"""

new_title = """                    if (showToolSettings in listOf("pen", "fountain_pen", "ballpoint")) {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.Center) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .background(if (showToolSettings == "fountain_pen") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { 
                                        viewModel.activeToolType = "fountain_pen" 
                                        showToolSettings = "fountain_pen"
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Fountain", fontWeight = if (showToolSettings == "fountain_pen") FontWeight.Bold else FontWeight.Normal)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .background(if (showToolSettings == "ballpoint") MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { 
                                        viewModel.activeToolType = "ballpoint"
                                        showToolSettings = "ballpoint"
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ballpoint", fontWeight = if (showToolSettings == "ballpoint") FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    } else {
                        Text("Tool Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
                    }
                    
                    Text("Tool Thickness", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)"""

content = content.replace(old_title, new_title)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
