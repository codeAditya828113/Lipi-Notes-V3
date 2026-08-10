package com.example.handwriting

import androidx.compose.ui.geometry.Rect
import com.example.data.Stroke

enum class RefinementLevel(val displayName: String, val strengthFactor: Float) {
    NATURAL("Natural", 0.25f),
    LIGHT("Light", 0.50f),
    BALANCED("Balanced", 0.75f),
    STRONG("Strong", 0.95f);

    companion object {
        fun fromFactor(factor: Float): RefinementLevel {
            return when {
                factor <= 0.35f -> NATURAL
                factor <= 0.60f -> LIGHT
                factor <= 0.85f -> BALANCED
                else -> STRONG
            }
        }
    }
}

enum class SpacingMode {
    TIGHTEN,
    INCREASE,
    NORMALIZE_WORDS,
    NORMALIZE_LINES
}

data class PersonalStyleProfile(
    val slantAngle: Float = -5.0f,          // Average handwriting slant in degrees
    val baselineVariation: Float = 3.5f,     // Vertical fluctuation
    val letterAspectRatio: Float = 1.25f,   // Height-to-width ratio
    val strokeWidthMultiplier: Float = 1.0f,
    val pressureSensitivity: Float = 0.85f,
    val curvatureFactor: Float = 1.1f,
    val sampleCount: Int = 0,
    val learnedAt: Long = System.currentTimeMillis()
)

data class RefinementResult(
    val originalStrokes: List<Stroke>,
    val refinedStrokes: List<Stroke>,
    val level: RefinementLevel = RefinementLevel.BALANCED,
    val isStraightened: Boolean = false,
    val spacingMode: SpacingMode? = null,
    val smoothnessScore: Float = 0.92f,
    val alignmentScore: Float = 0.88f
)

data class RecognizedHandwritingResult(
    val rawStrokes: List<Stroke>,
    val recognizedText: String,
    val confidence: Float = 0.95f,
    val language: String = "en",
    val wordBounds: List<Rect> = emptyList()
)

enum class ScribbleGesture(val description: String) {
    DELETE("Scratch out to erase"),
    INSERT_SPACE("Vertical slash to insert space"),
    JOIN_WORDS("Curve to join words"),
    SELECT_WORD("Circle to select word"),
    UNDERLINE("Underline to select/highlight"),
    CROSS_OUT("Strikethrough to delete"),
    NONE("Normal stroke")
}
