import re

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.data.Stroke\n", "import com.example.data.Stroke\nimport com.example.data.StrokeSerializer\n")

with open("app/src/main/java/com/example/ui/components/NoteViewModel.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "r") as f:
    content2 = f.read()

content2 = content2.replace("strokes = viewModel.currentStrokes,\n                    images = viewModel.currentImages,", "strokes = viewModel.currentStrokes,\n                    fadingStrokes = viewModel.fadingStrokes,\n                    images = viewModel.currentImages,")

with open("app/src/main/java/com/example/ui/components/NoteinApp.kt", "w") as f:
    f.write(content2)

