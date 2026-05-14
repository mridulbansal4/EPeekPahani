package io.sc.eppCordova.lossclaim.domain.engine

import io.sc.eppCordova.lossclaim.domain.model.AiPrompt
import io.sc.eppCordova.lossclaim.domain.model.DisasterType
import io.sc.eppCordova.lossclaim.domain.model.QuestionType
import io.sc.eppCordova.lossclaim.domain.model.SurveyStage
import io.sc.eppCordova.lossclaim.domain.model.AiObservation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationalSurveyEngine @Inject constructor() {

    // Redesigned dynamic question bank with more natural prompts and simplified language
    private val prompts = listOf(
        // Stage 1: Field Orientation
        AiPrompt("o1", "Please point the camera towards your field.", "कृपया कॅमेरा तुमच्या शेताकडे वळवा.", "कृपया कैमरा अपने खेत की ओर करें।", QuestionType.INFO, SurveyStage.FIELD_ORIENTATION, nextPromptId = "o2"),
        AiPrompt("o2", "Walk slowly along one row so I can see the full crop.", "कृपया एका ओळीने हळू चाला जेणेकरून मला संपूर्ण पीक दिसेल.", "कृपया एक पंक्ति के साथ धीरे-धीरे चलें ताकि मैं पूरी फसल देख सकूं।", QuestionType.CAPTURE_PHOTO, SurveyStage.FIELD_ORIENTATION),

        // Stage 2: AI Damage Detection (Dynamic based on observation)
        AiPrompt("d_chlorosis", "I can see yellowing on the leaves. When did you first notice this?", "मला पानांवर पिवळसरपणा दिसतोय. तुम्हाला हे पहिल्यांदा कधी दिसलं?", "मुझे पत्तियों पर पीलापन दिखाई दे रहा है। आपने इसे पहली बार कब देखा?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION),
        AiPrompt("d_disease", "Please bring the camera closer to one of the damaged leaves.", "कृपया कॅमेरा खराब झालेल्या पानांपैकी एकाजवळ आणा.", "कृपया कैमरे को क्षतिग्रस्त पत्तियों में से किसी एक के करीब लाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION),
        AiPrompt("d_lodging", "Many plants appear to be leaning. Did strong wind hit this area recently?", "अनेक झाडे कललेली दिसतात. अलीकडे या भागात जोरदार वारे आले होते का?", "कई पौधे झुके हुए दिखाई दे रहे हैं। क्या हाल ही में इस क्षेत्र में तेज हवा चली थी?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION),
        AiPrompt("d_flood", "Can you show me the soil near the roots? I want to check for waterlogging.", "तुम्ही मला मुळांजवळची माती दाखवू शकता का? मला पाणी साचले आहे का ते पाहायचे आहे.", "क्या आप मुझे जड़ों के पास की मिट्टी दिखा सकते हैं? मैं जलभराव की जांच करना चाहता हूं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION),

        // Stage 2: Disaster-specific Flow Questions
        // FLOOD
        AiPrompt("f1", "Walk slowly through the field and record a video to show water accumulation.", "शेतात हळू हळू चालत व्हिडिओ काढा जेणेकरून साचलेले पाणी दिसेल.", "खेत में धीरे-धीरे चलते हुए एक वीडियो रिकॉर्ड करें ताकि जमा पानी दिखाई दे।", QuestionType.CAPTURE_VIDEO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD, nextPromptId = "f2"),
        AiPrompt("f2", "Was water above the base of the plants at any point?", "पाणी झाडांच्या मुळाच्या वर होतं का?", "क्या किसी बिंदु पर पानी पौधों के आधार से ऊपर था?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD, nextPromptId = "f3"),
        AiPrompt("f3", "For how many days was the field flooded?", "शेत किती दिवस पाण्याखाली होतं?", "खेत में कितने दिनों तक पानी भरा रहा?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD),

        // HAILSTORM
        AiPrompt("h1", "Show me one of the most damaged leaves up close.", "मला सर्वात जास्त खराब झालेलं पान जवळून दाखवा.", "मुझे सबसे क्षतिग्रस्त पत्तियों में से एक को करीब से दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM, nextPromptId = "h2"),
        AiPrompt("h2", "Can you show the crop heads or panicles?", "तुम्ही पिकाची कणसं दाखवू शकता का?", "क्या आप फसल के सिरे या पैनिकल दिखा सकते हैं?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM, nextPromptId = "h3"),
        AiPrompt("h3", "How large were the hailstones?", "गारा किती मोठ्या होत्या?", "ओले कितने बड़े थे?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM),

        // DROUGHT
        AiPrompt("dr1", "Record a close-up video showing the soil cracks and dryness.", "जमिनीतील भेगा आणि कोरडेपणा दाखवणारा जवळून व्हिडिओ काढा.", "मिट्टी की दरारें और सूखापन दिखाते हुए एक क्लोज-अप वीडियो रिकॉर्ड करें।", QuestionType.CAPTURE_VIDEO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT, nextPromptId = "dr2"),
        AiPrompt("dr2", "Did you try to irrigate? Show me the water source if possible.", "तुम्ही पाणी द्यायचा प्रयत्न केला का? शक्य असल्यास पाण्याचा स्रोत दाखवा.", "क्या आपने सिंचाई करने की कोशिश की? यदि संभव हो तो मुझे जल स्रोत दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT, nextPromptId = "dr3"),
        AiPrompt("dr3", "What growth stage is the crop at?", "पीक वाढीच्या कोणत्या अवस्थेत आहे?", "फसल विकास के किस चरण में है?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT),

        // PEST_ATTACK
        AiPrompt("p1", "Record a video inspecting the underside of the leaves.", "पानांच्या खाली कॅमेरा दाखवत व्हिडिओ काढा.", "पत्तियों के नीचे कैमरे को दिखाते हुए एक वीडियो रिकॉर्ड करें।", QuestionType.CAPTURE_VIDEO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK, nextPromptId = "p2"),
        AiPrompt("p2", "Can you see the pest itself? Bring the camera very close.", "तुम्हाला कीड दिसतेय का? कॅमेरा अगदी जवळ आणा.", "क्या आप कीट को स्वयं देख सकते हैं? कैमरे को बहुत करीब लाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK, nextPromptId = "p3"),
        AiPrompt("p3", "Walk one row slowly so I can count how many plants are affected.", "एका ओळीने हळू चाला जेणेकरून मी किती झाडे बाधित आहेत ते मोजू शकेन.", "एक पंक्ति में धीरे-धीरे चलें ताकि मैं गिन सकूं कि कितने पौधे प्रभावित हैं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK),

        // DISEASE
        AiPrompt("di1", "Show me one healthy plant and one sick plant side by side.", "मला एक चांगलं आणि एक खराब रोप शेजारी-शेजारी दाखवा.", "मुझे एक स्वस्थ पौधा और एक बीमार पौधा अगल-बगल दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE, nextPromptId = "di2"),
        AiPrompt("di2", "Can you break open a damaged stem and show me the inside colour?", "तुम्ही एक खराब झालेलं खोड तोडून मला आतला रंग दाखवू शकता का?", "क्या आप एक क्षतिग्रस्त तने को तोड़कर मुझे अंदर का रंग दिखा सकते हैं?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE, nextPromptId = "di3"),
        AiPrompt("di3", "Which symptom appeared first?", "कोणतं लक्षण पहिलं दिसलं?", "कौन सा लक्षण पहले दिखाई दिया?", QuestionType.VERBAL_CONFIRM, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE),

        // CYCLONE
        AiPrompt("c1", "Record a panoramic video showing the fallen crops.", "संपूर्ण पडलेलं पीक दाखवत व्हिडिओ काढा.", "गिरे हुए पूरे फसल को दिखाते हुए एक पैनोरमिक वीडियो रिकॉर्ड करें।", QuestionType.CAPTURE_VIDEO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.CYCLONE, nextPromptId = "c2"),
        AiPrompt("c2", "Show me roots of one fallen plant — are they pulled out or stem broken?", "मला एका पडलेल्या झाडाची मुळं दाखवा — ती उपटली आहेत की खोड मोडलंय?", "मुझे एक गिरे हुए पौधे की जड़ें दिखाएं — क्या वे उखड़ी हुई हैं या तना टूटा हुआ है?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.CYCLONE),

        // Stage 3: Farmer Confirmation
        AiPrompt("q1", "Which calamity damaged your crop?", "कोणत्या आपत्तीमुळे तुमच्या पिकाचं नुकसान झालं?", "किस आपदा ने आपकी फसल को नुकसान पहुँचाया?", QuestionType.VERBAL_CONFIRM, SurveyStage.FARMER_CONFIRMATION, nextPromptId = "q2"),
        AiPrompt("q2", "When did the damage occur?", "नुकसान कधी झालं?", "नुकसान कब हुआ?", QuestionType.VERBAL_CONFIRM, SurveyStage.FARMER_CONFIRMATION, nextPromptId = "q3"),
        AiPrompt("q3", "Approximately what percentage of your crop is damaged?", "तुमच्या पिकाचं अंदाजे किती टक्के नुकसान झालंय?", "आपकी फसल का लगभग कितना प्रतिशत क्षतिग्रस्त है?", QuestionType.VERBAL_CONFIRM, SurveyStage.FARMER_CONFIRMATION, nextPromptId = "q4"),
        AiPrompt("q4", "Is any part of the crop still harvestable?", "पिकाचा काही भाग अजूनही काढता येईल असा आहे का?", "क्या फसल का कोई हिस्सा अभी भी काटने योग्य है?", QuestionType.VERBAL_CONFIRM, SurveyStage.FARMER_CONFIRMATION)
    )

    fun getInitialPrompt(): AiPrompt {
        return prompts.first { it.id == "o1" }
    }

    fun determineNextQuestion(
        currentPromptId: String,
        disasterType: DisasterType,
        observations: List<AiObservation>,
        farmerAnswers: Map<String, Any>,
        confidenceScore: Int
    ): AiPrompt? {
        val current = prompts.find { it.id == currentPromptId } ?: return null

        // 1. Check if we have gathered enough information dynamically to skip
        if (confidenceScore > 85 && current.stage == SurveyStage.AI_DAMAGE_DETECTION) {
             // Skip to farmer confirmation if confident
             return prompts.find { it.id == "q1" }
        }

        // 2. Static Next Prompt Check
        if (current.nextPromptId != null) {
            val next = prompts.find { it.id == current.nextPromptId }
            if (next != null) return next
        }

        // 3. Dynamic Triggers based on latest Observation
        if (current.stage == SurveyStage.FIELD_ORIENTATION && current.id == "o2") {
            val latestObs = observations.maxByOrNull { it.timestamp }
            if (latestObs != null) {
                return when (latestObs.type) {
                    "yellow_leaves" -> prompts.find { it.id == "d_chlorosis" }
                    "disease_spots" -> prompts.find { it.id == "d_disease" }
                    "lodging" -> prompts.find { it.id == "d_lodging" }
                    "waterlogging" -> prompts.find { it.id == "d_flood" }
                    else -> getFirstPromptForDisaster(disasterType)
                }
            } else {
                return getFirstPromptForDisaster(disasterType)
            }
        }

        // 4. Move to Stage 3
        if (current.stage == SurveyStage.AI_DAMAGE_DETECTION) {
            return prompts.find { it.id == "q1" }
        }

        return null // End of Survey
    }

    private fun getFirstPromptForDisaster(disaster: DisasterType): AiPrompt {
        return prompts.firstOrNull { it.stage == SurveyStage.AI_DAMAGE_DETECTION && it.requiredDisaster == disaster }
            ?: prompts.first { it.id == "q1" }
    }
}
