package com.vitalcore.app.ai

import com.vitalcore.app.data.repositories.NutritionRepository
import com.vitalcore.app.data.repositories.ScoreRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the day's context (Recovery/Sleep/Strain + nutrition) as plain text
 * and hands it to whichever [AiProvider] is configured. The calculation
 * engine's outputs are the source of truth — the AI Coach only explains and
 * summarizes them, it never computes its own scores.
 */
@Singleton
class AiCoachRepository @Inject constructor(
    private val aiProvider: AiProvider,
    private val scoreRepository: ScoreRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend fun buildTodayContext(): String {
        val today = LocalDate.now()
        val recovery = scoreRepository.recoveryFor(today)
        val sleep = scoreRepository.sleepFor(today)
        val strain = scoreRepository.strainFor(today)
        val (calories, water) = nutritionRepository.caloriesAndWaterForDay(today)

        return buildString {
            appendLine("You are VitalCore's AI Coach. You explain the user's own health data;")
            appendLine("you never diagnose conditions and you never invent numbers not given below.")
            appendLine("Speak plainly, keep answers concise, and never claim a causal relationship")
            appendLine("between metrics unless the user's own data makes it obvious - prefer")
            appendLine("\"associated with\" framing, matching VitalCore's own insights engine.")
            appendLine()
            appendLine("Today's data:")
            appendLine("- Recovery: ${recovery?.score ?: "no data"} (confidence: ${recovery?.confidence ?: "n/a"})")
            appendLine("- Sleep score: ${sleep?.score ?: "no data"} (duration: ${sleep?.durationMinutes ?: "?"} min)")
            appendLine("- Strain: ${strain?.score ?: "no data"}")
            appendLine("- Calories logged today: $calories kcal")
            appendLine("- Water logged today: $water ml")
        }
    }

    suspend fun ask(question: String): AiResponse {
        val context = buildTodayContext()
        return aiProvider.ask(context, question)
    }

    suspend fun explainRecovery(): AiResponse = ask("Explain today's Recovery score in 2-3 short sentences.")
    suspend fun explainSleep(): AiResponse = ask("Explain last night's Sleep score in 2-3 short sentences.")
    suspend fun explainStrain(): AiResponse = ask("Explain today's Strain score in 2-3 short sentences.")
    suspend fun summarizeDay(): AiResponse = ask("Give a short, friendly summary of my day so far across recovery, sleep, strain, and nutrition.")
}
