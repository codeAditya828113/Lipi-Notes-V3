import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

content = content.replace(
"""fun FloatingPenSection(
    viewModel: NoteViewModel,
    onExitFullscreen: () -> Unit,
    onChangeTemplateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {""",
"""fun FloatingPenSection(
    viewModel: NoteViewModel,
    onExitFullscreen: () -> Unit,
    onToolDoubleTap: (String) -> Unit,
    onChangeTemplateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {""")

content = content.replace(
"""                            FloatingPenSection(
                                viewModel = viewModel,
                                onExitFullscreen = { viewModel.isFullscreen = false },
                                onChangeTemplateClick = { showTemplateSelectionModal = true }
                            )""",
"""                            FloatingPenSection(
                                viewModel = viewModel,
                                onExitFullscreen = { viewModel.isFullscreen = false },
                                onToolDoubleTap = { showToolSettings = it },
                                onChangeTemplateClick = { showTemplateSelectionModal = true }
                            )""")

content = content.replace(
"""                    RealisticPenItem(
                            onDoubleTap = { showToolSettings = toolId },
                        toolId = toolId,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.activeToolType = toolId
                        }""",
"""                    RealisticPenItem(
                        onDoubleTap = { onToolDoubleTap(toolId) },
                        toolId = toolId,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.activeToolType = toolId
                        }""")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
