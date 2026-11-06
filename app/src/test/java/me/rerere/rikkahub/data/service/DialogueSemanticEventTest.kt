package me.rerere.rikkahub.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialogueSemanticEventTest {
    private fun response(feature: String, value: String = "0.8"): String {
        val brain = DriveEngine.DRIVE_EVENT_BRAIN_FEATURES.keys.joinToString(",") {
            "\"$it\":" + if (it == feature) value else "0.0"
        }
        return "{\"confidence\":0.9,\"intensity\":0.6,\"brain\":{$brain}}"
    }

    @Test
    fun fatigueAndAffectionProduceDifferentDrives() {
        val tired = DialogueSemanticEvent.parse(response("energy_cost"), "我今天很累")!!
        val affection = DialogueSemanticEvent.parse(response("closeness_pull"), "我今天想你")!!
        assertEquals("fatigue", tired.primaryDrive)
        assertEquals("attachment", affection.primaryDrive)
        assertEquals(listOf("我今天很累"), tired.evidence)
    }

    @Test
    fun invalidOrIncompleteAnalysisFallsBack() {
        assertNull(DialogueSemanticEvent.parse("{}", "text"))
        assertNull(DialogueSemanticEvent.parse(response("energy_cost", "2.0"), "text"))
        assertNull(DialogueSemanticEvent.parse(response("energy_cost", "\"0.8\""), "text"))
        assertNull(DialogueSemanticEvent.parse("not json", "text"))
    }

    @Test
    fun codeFenceIsAcceptedWithoutAcceptingSurroundingProse() {
        assertEquals("fatigue", DialogueSemanticEvent.parse(
            "```json\n${response("energy_cost")}\n```", "text",
        )!!.primaryDrive)
        assertNull(DialogueSemanticEvent.parse("analysis: ${response("energy_cost")}", "text"))
    }
}