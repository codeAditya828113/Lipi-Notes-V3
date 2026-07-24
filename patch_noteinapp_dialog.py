import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

# Replace the onSave callback
old_call = """        AdvancedTemplateDialog(
            note = selectedNote,
            onDismiss = { showTemplateSelectionModal = false },
            onSave = { templateType, coverType, pageColor ->
                viewModel.updateNoteDesign(templateType, coverType, pageColor)
                showTemplateSelectionModal = false
            }
        )"""

new_call = """        AdvancedTemplateDialog(
            note = selectedNote,
            onDismiss = { showTemplateSelectionModal = false },
            onSave = { templateType, coverType, pageColor, coverTitle, coverSubtitle, coverAuthor, coverExtra ->
                viewModel.updateNoteDesign(templateType, coverType, pageColor)
                viewModel.updateCoverInfo(coverTitle, coverSubtitle, coverAuthor, coverExtra)
                showTemplateSelectionModal = false
            }
        )"""

content = content.replace(old_call, new_call)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)
