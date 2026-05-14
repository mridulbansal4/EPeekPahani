package io.sc.eppCordova.lossclaim.domain.engine

import io.sc.eppCordova.lossclaim.domain.model.AiPrompt
import io.sc.eppCordova.lossclaim.domain.model.DisasterType
import io.sc.eppCordova.lossclaim.domain.model.SurveyStage
import io.sc.eppCordova.lossclaim.domain.model.AiObservation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class SurveyState {
    object Idle : SurveyState()
    data class Active(
        val stage: SurveyStage,
        val currentPrompt: AiPrompt,
        val progress: Float
    ) : SurveyState()
    data class Reviewing(val missingEvidence: List<String>) : SurveyState()
    object Completed : SurveyState()
}

@Singleton
class SurveyStateMachine @Inject constructor(
    private val conversationalEngine: ConversationalSurveyEngine,
    private val confidenceScoringEngine: ConfidenceScoringEngine
) {
    private val _state = MutableStateFlow<SurveyState>(SurveyState.Idle)
    val state: StateFlow<SurveyState> = _state.asStateFlow()
    
    private var currentDisasterType = DisasterType.UNKNOWN
    private val collectedObservations = mutableListOf<AiObservation>()
    private val farmerAnswers = mutableMapOf<String, Any>()

    fun startSurvey(disasterType: DisasterType) {
        currentDisasterType = disasterType
        val firstPrompt = conversationalEngine.getInitialPrompt()
        _state.value = SurveyState.Active(SurveyStage.FIELD_ORIENTATION, firstPrompt, 0.0f)
    }

    fun processInput(
        observation: AiObservation? = null,
        farmerAnswer: Map<String, Any>? = null
    ) {
        val currentState = _state.value
        if (currentState !is SurveyState.Active) return

        observation?.let { collectedObservations.add(it) }
        farmerAnswer?.let { farmerAnswers.putAll(it) }

        // Evaluate Confidence
        val confidenceScore = confidenceScoringEngine.calculateCompleteness(
            currentDisasterType, collectedObservations, farmerAnswers
        )

        // Adaptive compression: If confidence is high enough, we can jump to COMPLETED
        if (confidenceScore >= 90) {
             _state.value = SurveyState.Completed
             return
        }

        val nextPrompt = conversationalEngine.determineNextQuestion(
            currentPromptId = currentState.currentPrompt.id,
            disasterType = currentDisasterType,
            observations = collectedObservations,
            farmerAnswers = farmerAnswers,
            confidenceScore = confidenceScore
        )

        if (nextPrompt == null) {
            val missing = confidenceScoringEngine.getMissingEvidence(currentDisasterType, collectedObservations, farmerAnswers)
            if (missing.isNotEmpty()) {
                _state.value = SurveyState.Reviewing(missing)
            } else {
                _state.value = SurveyState.Completed
            }
        } else {
            val newProgress = minOf(1.0f, currentState.progress + 0.1f)
            _state.value = SurveyState.Active(nextPrompt.stage, nextPrompt, newProgress)
        }
    }
}
