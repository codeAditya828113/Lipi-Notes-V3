import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

lasso_btn = """                    // Crop / Lasso selection
                    IconButton(
                        onClick = { viewModel.activeToolType = "lasso" },
                        modifier = Modifier.size(32.dp)
                    )"""

new_lasso_btn = """                    // Crop / Lasso selection
                    IconButton(
                        onClick = { 
                            if (viewModel.activeToolType == "lasso") {
                                showToolSettings = if (showToolSettings == "lasso") null else "lasso"
                            } else {
                                viewModel.activeToolType = "lasso"
                                showToolSettings = null
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    )"""

content = content.replace(lasso_btn, new_lasso_btn)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

