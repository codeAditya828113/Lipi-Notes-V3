with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

import re

# We will replace the if (isTablet && !viewModel.isFullscreen) ... else ... block in NoteWorkspace.
# Let's find the start of the block
start_idx = content.find("    if (isTablet && !viewModel.isFullscreen) {")
if start_idx != -1:
    end_idx = content.find("    val showCreateDialog", start_idx) # Actually we can find where NoteWorkspace ends, which is just before @Composable fun NovaDashboard
    end_idx = content.find("@Composable\nfun NovaDashboard", start_idx)

