package io.sc.eppCordova.lossclaim.domain

import io.sc.eppCordova.lossclaim.domain.model.AiPrompt
import io.sc.eppCordova.lossclaim.domain.model.DisasterType
import io.sc.eppCordova.lossclaim.domain.model.QuestionType
import io.sc.eppCordova.lossclaim.domain.model.SurveyStage

class ConditionalFlowEngine {

    // All available prompts defined by the PDF
    val prompts = listOf(
        // Stage 1: Field Orientation
        AiPrompt("o1", "Please point the camera towards your field.", "कृपया कॅमेरा तुमच्या शेताकडे वळवा.", "कृपया कैमरा अपने खेत की ओर करें।", QuestionType.INFO, SurveyStage.FIELD_ORIENTATION, nextPromptId = "o2"),
        AiPrompt("o2", "Walk slowly along one row so I can see the full crop.", "कृपया एका ओळीने हळू चाला जेणेकरून मला संपूर्ण पीक दिसेल.", "कृपया एक पंक्ति के साथ धीरे-धीरे चलें ताकि मैं पूरी फसल देख सकूं।", QuestionType.CAPTURE_PHOTO, SurveyStage.FIELD_ORIENTATION),

        // Stage 2: AI Damage Detection (Dynamic based on observation)
        AiPrompt("d_chlorosis", "I can see yellowing on the leaves. When did you first notice this?", "मला पानांवर पिवळसरपणा दिसतोय. तुम्ही हे पहिल्यांदा कधी पाहिलं?", "मुझे पत्तियों पर पीलापन दिखाई दे रहा है। आपने इसे पहली बार कब देखा?", QuestionType.OPTIONS, SurveyStage.AI_DAMAGE_DETECTION, options = listOf("Last 3 days", "Last week", "More than a week ago")),
        AiPrompt("d_disease", "Please bring the camera closer to one of the damaged leaves.", "कृपया कॅमेरा खराब झालेल्या पानांपैकी एकाजवळ आणा.", "कृपया कैमरे को क्षतिग्रस्त पत्तियों में से किसी एक के करीब लाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION),
        AiPrompt("d_lodging", "Many plants appear to be leaning. Did strong wind hit this area recently?", "अनेक झाडे कललेली दिसतात. अलीकडे या भागात जोरदार वारे आले होते का?", "कई पौधे झुके हुए दिखाई दे रहे हैं। क्या हाल ही में इस क्षेत्र में तेज हवा चली थी?", QuestionType.YES_NO, SurveyStage.AI_DAMAGE_DETECTION),
        AiPrompt("d_flood", "Can you show me the soil near the roots? I want to check for waterlogging.", "तुम्ही मला मुळांजवळची माती दाखवू शकता का? मला पाणी साचले आहे का ते तपासायचे आहे.", "क्या आप मुझे जड़ों के पास की मिट्टी दिखा सकते हैं? मैं जलभराव की जांच करना चाहता हूं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION),

        // Stage 2: Disaster-specific Flow Questions
        // FLOOD
        AiPrompt("f1", "Show me the lowest area of your field — where water would collect.", "मला तुमच्या शेतातील सर्वात सखल भाग दाखवा — जिथे पाणी साचेल.", "मुझे अपने खेत का सबसे निचला क्षेत्र दिखाएं — जहाँ पानी इकट्ठा होगा।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD, nextPromptId = "f2"),
        AiPrompt("f2", "Was water above the base of the plants at any point?", "कोणत्याही वेळी पाणी झाडांच्या मुळाच्या वर होते का?", "क्या किसी बिंदु पर पानी पौधों के आधार से ऊपर था?", QuestionType.YES_NO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD, nextPromptId = "f3"),
        AiPrompt("f3", "For how many days was the field flooded?", "शेत किती दिवस पाण्याखाली होते?", "खेत में कितने दिनों तक पानी भरा रहा?", QuestionType.OPTIONS, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.FLOOD, options = listOf("1-2 days", "3-5 days", "More than 5 days")),

        // HAILSTORM
        AiPrompt("h1", "Show me one of the most damaged leaves up close.", "मला सर्वात जास्त खराब झालेल्या पानांपैकी एक जवळून दाखवा.", "मुझे सबसे क्षतिग्रस्त पत्तियों में से एक को करीब से दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM, nextPromptId = "h2"),
        AiPrompt("h2", "Can you show the crop heads or panicles?", "तुम्ही पिकाची कणसे किंवा लोंब्या दाखवू शकता का?", "क्या आप फसल के सिरे या पैनिकल दिखा सकते हैं?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM, nextPromptId = "h3"),
        AiPrompt("h3", "How large were the hailstones?", "गारा किती मोठ्या होत्या?", "ओले कितने बड़े थे?", QuestionType.OPTIONS, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.HAILSTORM, options = listOf("Pea", "Marble", "Golf ball", "Larger")),

        // DROUGHT
        AiPrompt("dr1", "Show me the soil between two rows — I want to see how dry it is.", "मला दोन ओळींमधील माती दाखवा — मला पाहायचे आहे ती किती कोरडी आहे.", "मुझे दो पंक्तियों के बीच की मिट्टी दिखाएं — मैं देखना चाहता हूं कि यह कितनी सूखी है।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT, nextPromptId = "dr2"),
        AiPrompt("dr2", "Did you try to irrigate? Show me the water source if possible.", "तुम्ही सिंचनाचा प्रयत्न केला का? शक्य असल्यास मला पाण्याचा स्रोत दाखवा.", "क्या आपने सिंचाई करने की कोशिश की? यदि संभव हो तो मुझे जल स्रोत दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT, nextPromptId = "dr3"),
        AiPrompt("dr3", "What growth stage is the crop at?", "पीक वाढीच्या कोणत्या अवस्थेत आहे?", "फसल विकास के किस चरण में है?", QuestionType.OPTIONS, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DROUGHT, options = listOf("Germination", "Vegetative", "Flowering", "Maturity")),

        // PEST_ATTACK
        AiPrompt("p1", "Show me the underside of a few leaves — pests often hide there.", "मला काही पानांची खालची बाजू दाखवा — कीटक अनेकदा तिथे लपलेले असतात.", "मुझे कुछ पत्तियों के नीचे का भाग दिखाएं — कीट अक्सर वहीं छिपे रहते हैं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK, nextPromptId = "p2"),
        AiPrompt("p2", "Can you see the pest itself? Bring the camera very close.", "तुम्हाला कीटक दिसतोय का? कॅमेरा अगदी जवळ आणा.", "क्या आप कीट को स्वयं देख सकते हैं? कैमरे को बहुत करीब लाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK, nextPromptId = "p3"),
        AiPrompt("p3", "Walk one row slowly so I can count how many plants are affected.", "एका ओळीने हळू चाला जेणेकरून मी किती झाडे बाधित आहेत ते मोजू शकेन.", "एक पंक्ति में धीरे-धीरे चलें ताकि मैं गिन सकूं कि कितने पौधे प्रभावित हैं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.PEST_ATTACK),

        // DISEASE
        AiPrompt("di1", "Show me one healthy plant and one sick plant side by side.", "मला एक निरोगी आणि एक आजारी रोप शेजारी-शेजारी दाखवा.", "मुझे एक स्वस्थ पौधा और एक बीमार पौधा अगल-बगल दिखाएं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE, nextPromptId = "di2"),
        AiPrompt("di2", "Can you break open a damaged stem and show me the inside colour?", "तुम्ही एक खराब झालेले खोड तोडून मला आतला रंग दाखवू शकता का?", "क्या आप एक क्षतिग्रस्त तने को तोड़कर मुझे अंदर का रंग दिखा सकते हैं?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE, nextPromptId = "di3"),
        AiPrompt("di3", "Which symptom appeared first?", "कोणते लक्षण पहिले दिसले?", "कौन सा लक्षण पहले दिखाई दिया?", QuestionType.OPTIONS, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.DISEASE, options = listOf("Yellowing", "Wilting", "Spots")),

        // CYCLONE
        AiPrompt("c1", "Pan the camera across the whole field so I can count fallen plants.", "संपूर्ण शेतावर कॅमेरा फिरवा जेणेकरून मी पडलेली झाडे मोजू शकेन.", "पूरे खेत में कैमरे को घुमाएं ताकि मैं गिरे हुए पौधों की गिनती कर सकूं।", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.CYCLONE, nextPromptId = "c2"),
        AiPrompt("c2", "Show me roots of one fallen plant — are they pulled out or stem broken?", "मला एका पडलेल्या झाडाची मुळे दाखवा — ती उपटली गेली आहेत की खोड मोडले आहे?", "मुझे एक गिरे हुए पौधे की जड़ें दिखाएं — क्या वे उखड़ी हुई हैं या तना टूटा हुआ है?", QuestionType.CAPTURE_PHOTO, SurveyStage.AI_DAMAGE_DETECTION, DisasterType.CYCLONE),

        // Stage 3: Farmer Confirmation
        AiPrompt("q1", "Which calamity damaged your crop?", "कोणत्या आपत्तीने तुमच्या पिकाचे नुकसान झाले?", "किस आपदा ने आपकी फसल को नुकसान पहुँचाया?", QuestionType.OPTIONS, SurveyStage.FARMER_CONFIRMATION, options = listOf("Flood", "Hailstorm", "Drought", "Pest", "Disease", "Cyclone"), nextPromptId = "q2"),
        AiPrompt("q2", "When did the damage occur?", "नुकसान कधी झाले?", "नुकसान कब हुआ?", QuestionType.OPTIONS, SurveyStage.FARMER_CONFIRMATION, options = listOf("Today", "Last 3 days", "Last week", "Last 15 days"), nextPromptId = "q3"),
        AiPrompt("q3", "Approximately what percentage of your crop is damaged?", "तुमच्या पिकाचे अंदाजे किती टक्के नुकसान झाले आहे?", "आपकी फसल का लगभग कितना प्रतिशत क्षतिग्रस्त है?", QuestionType.SLIDER, SurveyStage.FARMER_CONFIRMATION, nextPromptId = "q4"),
        AiPrompt("q4", "Is any part of the crop still harvestable?", "पिकाचा काही भाग अजूनही काढणीयोग्य आहे का?", "क्या फसल का कोई हिस्सा अभी भी काटने योग्य है?", QuestionType.OPTIONS, SurveyStage.FARMER_CONFIRMATION, options = listOf("Yes", "No", "Partially"), nextPromptId = "q5"),
        AiPrompt("q5", "Did you apply any pesticide or fertiliser in the past 15 days?", "तुम्ही गेल्या १५ दिवसात कोणतेही कीटकनाशक किंवा खत वापरले आहे का?", "क्या आपने पिछले 15 दिनों में कोई कीटनाशक या उर्वरक लगाया है?", QuestionType.YES_NO, SurveyStage.FARMER_CONFIRMATION)
    )

    fun getInitialPrompt(): AiPrompt {
        return prompts.first { it.id == "o1" }
    }

    fun getNextPrompt(currentPromptId: String, currentDisaster: DisasterType, observation: String? = null): AiPrompt? {
        val current = prompts.find { it.id == currentPromptId } ?: return null

        // Static next prompt logic
        if (current.nextPromptId != null) {
            val next = prompts.find { it.id == current.nextPromptId }
            if (next != null) return next
        }

        // Dynamic transition logic
        if (current.stage == SurveyStage.FIELD_ORIENTATION && current.id == "o2") {
            // After field orientation, if we have an observation, we trigger specific AI damage detection
            if (observation != null) {
                return when (observation) {
                    "yellow_leaves" -> prompts.find { it.id == "d_chlorosis" }
                    "damaged_leaves" -> prompts.find { it.id == "d_disease" }
                    "lodged_crops" -> prompts.find { it.id == "d_lodging" }
                    "waterlogging" -> prompts.find { it.id == "d_flood" }
                    else -> getFirstPromptForDisaster(currentDisaster)
                }
            } else {
                return getFirstPromptForDisaster(currentDisaster)
            }
        }

        // Move to Stage 3 after Stage 2 specific questions end
        if (current.stage == SurveyStage.AI_DAMAGE_DETECTION && current.nextPromptId == null) {
            return prompts.find { it.id == "q1" }
        }
        
        // Stage 3 conditional logic for Q5
        if (currentPromptId == "q4") {
            return if (currentDisaster == DisasterType.PEST_ATTACK || currentDisaster == DisasterType.DISEASE) {
                prompts.find { it.id == "q5" }
            } else {
                null // End of flow
            }
        }

        return null
    }

    private fun getFirstPromptForDisaster(disaster: DisasterType): AiPrompt {
        return prompts.firstOrNull { it.stage == SurveyStage.AI_DAMAGE_DETECTION && it.requiredDisaster == disaster }
            ?: prompts.first { it.id == "q1" } // Default to farmer confirmation if no specific flow
    }
}