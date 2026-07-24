import re

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content = f.read()

old_selection = """                    realPens.forEach { (toolId, label) ->
                        val isSelected = viewModel.activeToolType == toolId"""

new_selection = """                    realPens.forEach { (toolId, label) ->
                        val isSelected = viewModel.activeToolType == toolId || (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint")"""

content = content.replace(old_selection, new_selection)

old_selection_2 = """                realPens.forEach { (toolId, label) ->
                    val isSelected = viewModel.activeToolType == toolId"""

new_selection_2 = """                realPens.forEach { (toolId, label) ->
                    val isSelected = viewModel.activeToolType == toolId || (toolId == "fountain_pen" && viewModel.activeToolType == "ballpoint")"""

content = content.replace(old_selection_2, new_selection_2)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content)

