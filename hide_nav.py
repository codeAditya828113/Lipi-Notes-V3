import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace(
    "if (!isTablet && !viewModel.isFullscreen) {",
    "if (!isTablet && !viewModel.isFullscreen && activeTab != \"notes\") {"
)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
