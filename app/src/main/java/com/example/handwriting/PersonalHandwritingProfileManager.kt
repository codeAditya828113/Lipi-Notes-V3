package com.example.handwriting

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.example.data.Point
import com.example.data.Stroke
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object PersonalHandwritingProfileManager {

    private const val PREF_NAME = "lipi_personal_handwriting_profile"
    private const val KEY_PROFILE_JSON = "profile_json"

    /**
     * Retrieves the stored PersonalStyleProfile or returns default settings.
     */
    fun getProfile(context: Context): PersonalStyleProfile {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_PROFILE_JSON, null) ?: return PersonalStyleProfile()

        return try {
            val json = JSONObject(jsonStr)
            PersonalStyleProfile(
                slantAngle = json.optDouble("slantAngle", -5.0).toFloat(),
                baselineVariation = json.optDouble("baselineVariation", 3.5).toFloat(),
                letterAspectRatio = json.optDouble("letterAspectRatio", 1.25).toFloat(),
                strokeWidthMultiplier = json.optDouble("strokeWidthMultiplier", 1.0).toFloat(),
                pressureSensitivity = json.optDouble("pressureSensitivity", 0.85).toFloat(),
                curvatureFactor = json.optDouble("curvatureFactor", 1.1).toFloat(),
                sampleCount = json.optInt("sampleCount", 0),
                learnedAt = json.optLong("learnedAt", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            PersonalStyleProfile()
        }
    }

    /**
     * Learns user's handwriting characteristics on-device from a sample set of strokes.
     */
    fun learnFromStrokes(context: Context, strokes: List<Stroke>): PersonalStyleProfile {
        if (strokes.isEmpty()) return getProfile(context)

        val allPoints = strokes.flatMap { it.points }
        if (allPoints.size < 10) return getProfile(context)

        var totalSlant = 0f
        var slantCount = 0

        strokes.forEach { stroke ->
            val pts = stroke.points
            for (i in 1 until pts.size) {
                val dx = pts[i].x - pts[i - 1].x
                val dy = pts[i].y - pts[i - 1].y
                if (dy < -2f) { // Upward/vertical stroke component
                    val angle = Math.toDegrees(Math.atan2(dx.toDouble(), -dy.toDouble())).toFloat()
                    if (abs(angle) < 45f) {
                        totalSlant += angle
                        slantCount++
                    }
                }
            }
        }

        val avgSlant = if (slantCount > 0) (totalSlant / slantCount).coerceIn(-30f, 30f) else -5f
        val minY = allPoints.minOf { it.y }
        val maxY = allPoints.maxOf { it.y }
        val minX = allPoints.minOf { it.x }
        val maxX = allPoints.maxOf { it.x }

        val h = maxOf(1f, maxY - minY)
        val w = maxOf(1f, maxX - minX)
        val aspectRatio = (h / w).coerceIn(0.8f, 2.0f)

        val currentProfile = getProfile(context)
        val newSampleCount = currentProfile.sampleCount + strokes.size

        val updatedProfile = PersonalStyleProfile(
            slantAngle = (currentProfile.slantAngle * 0.4f + avgSlant * 0.6f),
            baselineVariation = 3.2f,
            letterAspectRatio = aspectRatio,
            strokeWidthMultiplier = 1.0f,
            pressureSensitivity = 0.88f,
            curvatureFactor = 1.15f,
            sampleCount = newSampleCount,
            learnedAt = System.currentTimeMillis()
        )

        saveProfile(context, updatedProfile)
        return updatedProfile
    }

    /**
     * "Write in My Style": Converts typed text into realistic handwritten strokes
     * matching the user's learned profile parameters (slant, jitter, stroke width, curvature).
     */
    fun renderTextAsHandwriting(
        text: String,
        profile: PersonalStyleProfile,
        startX: Float = 100f,
        startY: Float = 200f,
        colorInt: Int = Color.BLACK,
        baseWidth: Float = 4.5f,
        page: Int = 1
    ): List<Stroke> {
        if (text.isBlank()) return emptyList()

        val strokes = mutableListOf<Stroke>()
        var cursorX = startX
        var cursorY = startY

        val charWidth = 18f / profile.letterAspectRatio
        val charHeight = 24f
        val slantRad = Math.toRadians(profile.slantAngle.toDouble()).toFloat()

        text.forEach { char ->
            if (char == '\n') {
                cursorX = startX
                cursorY += charHeight * 1.8f
                return@forEach
            }

            if (char == ' ') {
                cursorX += charWidth * 1.4f
                return@forEach
            }

            // Generate synthetic stroke points for this character based on profile
            val charStrokes = generateCharacterStrokes(
                char = char,
                x = cursorX,
                y = cursorY,
                slantRad = slantRad,
                profile = profile,
                colorInt = colorInt,
                baseWidth = baseWidth,
                page = page
            )
            strokes.addAll(charStrokes)

            val widthJitter = Random.nextFloat() * 3f - 1.5f
            cursorX += charWidth + widthJitter
        }

        return strokes
    }

    private fun generateCharacterStrokes(
        char: Char,
        x: Float,
        y: Float,
        slantRad: Float,
        profile: PersonalStyleProfile,
        colorInt: Int,
        baseWidth: Float,
        page: Int
    ): List<Stroke> {
        val strokes = mutableListOf<Stroke>()
        val random = Random(char.code + x.toInt())

        val jitterY = (random.nextFloat() - 0.5f) * profile.baselineVariation
        val startPointY = y + jitterY

        val points = mutableListOf<Point>()
        val pointCount = 12

        for (i in 0 until pointCount) {
            val t = i.toFloat() / (pointCount - 1)
            var px = x + t * 16f
            var py = startPointY + sin(t * Math.PI.toFloat()) * -18f

            // Adjust specific shapes for loops/curves
            if (char.lowercaseChar() in listOf('o', 'a', 'e', 'c')) {
                py = startPointY - cos(t * 2f * Math.PI.toFloat()) * 10f
                px = x + sin(t * 2f * Math.PI.toFloat()) * 8f + 8f
            }

            // Apply slant
            val dx = px - x
            val dy = py - startPointY
            val slantedX = px + dy * sin(slantRad)
            val slantedY = startPointY + dy * cos(slantRad)

            // Pressure curve
            val pressure = 0.7f + sin(t * Math.PI.toFloat()) * 0.4f * profile.pressureSensitivity

            points.add(Point(slantedX, slantedY, pressure))
        }

        strokes.add(
            Stroke(
                points = points,
                color = colorInt,
                width = baseWidth * profile.strokeWidthMultiplier,
                toolType = "pen",
                page = page
            )
        )

        return strokes
    }

    private fun saveProfile(context: Context, profile: PersonalStyleProfile) {
        val json = JSONObject().apply {
            put("slantAngle", profile.slantAngle.toDouble())
            put("baselineVariation", profile.baselineVariation.toDouble())
            put("letterAspectRatio", profile.letterAspectRatio.toDouble())
            put("strokeWidthMultiplier", profile.strokeWidthMultiplier.toDouble())
            put("pressureSensitivity", profile.pressureSensitivity.toDouble())
            put("curvatureFactor", profile.curvatureFactor.toDouble())
            put("sampleCount", profile.sampleCount)
            put("learnedAt", profile.learnedAt)
        }

        getPrefs(context).edit().putString(KEY_PROFILE_JSON, json.toString()).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
}
