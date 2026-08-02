package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class DirectoryItem(
    val id: String,
    val name: String,
    val parentId: String? = null, // null for root level, or parent Directory ID for nested subdirectories
    val colorHex: Long = 0xFF2196F3, // Custom text / icon color
    val iconName: String = "folder"
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("parentId", parentId ?: "")
            put("colorHex", colorHex)
            put("iconName", iconName)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): DirectoryItem {
            val pid = obj.optString("parentId", "").ifBlank { null }
            return DirectoryItem(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                name = obj.optString("name", "New Folder"),
                parentId = pid,
                colorHex = obj.optLong("colorHex", 0xFF2196F3),
                iconName = obj.optString("iconName", "folder")
            )
        }

        val DEFAULT_DIRECTORIES = listOf(
            DirectoryItem(id = "dir_work", name = "Work & Projects", parentId = null, colorHex = 0xFF1976D2),
            DirectoryItem(id = "dir_work_alpha", name = "Project Alpha", parentId = "dir_work", colorHex = 0xFF0288D1),
            DirectoryItem(id = "dir_school", name = "School & Academics", parentId = null, colorHex = 0xFF388E3C),
            DirectoryItem(id = "dir_school_sem1", name = "Semester 1 Notes", parentId = "dir_school", colorHex = 0xFF4CAF50),
            DirectoryItem(id = "dir_personal", name = "Personal Ideas", parentId = null, colorHex = 0xFF7B1FA2)
        )
    }
}

data class TagItem(
    val id: String,
    val name: String,
    val colorHex: Long = 0xFF6200EE, // Background/Border color of the tag
    val textColorHex: Long = 0xFFFFFFFF // Colored text color
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("colorHex", colorHex)
            put("textColorHex", textColorHex)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): TagItem {
            return TagItem(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                name = obj.optString("name", "tag"),
                colorHex = obj.optLong("colorHex", 0xFF6200EE),
                textColorHex = obj.optLong("textColorHex", 0xFFFFFFFF)
            )
        }

        val DEFAULT_TAGS = listOf(
            TagItem(id = "tag_urgent", name = "urgent", colorHex = 0xFFD32F2F, textColorHex = 0xFFFFFFFF),
            TagItem(id = "tag_work", name = "work", colorHex = 0xFF1976D2, textColorHex = 0xFFFFFFFF),
            TagItem(id = "tag_study", name = "study", colorHex = 0xFF388E3C, textColorHex = 0xFFFFFFFF),
            TagItem(id = "tag_ideas", name = "ideas", colorHex = 0xFFF57C00, textColorHex = 0xFFFFFFFF)
        )
    }
}
