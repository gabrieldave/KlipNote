package com.blackeye.klipnote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiStudyPromptsTest {

    @Test
    fun everyActionHasCompleteBilingualInstructions() {
        assertEquals(16, AiStudyMode.entries.size)
        for (mode in AiStudyMode.entries) {
            for (spanish in listOf(true, false)) {
                val prompt = AiStudyPrompts.intentFor(mode, spanish)
                assertTrue("Missing content for $mode language=$spanish", prompt.length > 95)
                assertFalse("Blank instructions for $mode", prompt.isBlank())
            }
        }
    }

    @Test
    fun allSixteenModesOmitTranscriptBodyAndReferToAttachedTxt() {
        val marker = "UNIQUE_TRANSCRIPT_BODY_MARKER_XYZ_98765"
        for (mode in AiStudyMode.entries) {
            for (spanish in listOf(true, false)) {
                val prompt = AiStudyPrompts.composePrompt(
                    es = spanish,
                    languageRule = if (spanish) "Idioma: español." else "Language: English.",
                    mode = mode,
                    sourceTitleOrUrl = "Mi fuente"
                )
                assertFalse(
                    "$mode/$spanish must not embed transcript markers",
                    prompt.contains("<<<TRANSCRIPT_START>>>") ||
                        prompt.contains("<<<TRANSCRIPT_END>>>") ||
                        prompt.contains(marker)
                )
                assertFalse(
                    "$mode/$spanish must not paste a fake transcript body",
                    prompt.contains(marker)
                )
                if (spanish) {
                    assertTrue(
                        "$mode ES must mention attached .txt",
                        prompt.contains("archivo .txt adjunto") ||
                            prompt.contains(".txt adjunto")
                    )
                    assertTrue(
                        "$mode ES must mention KlipNote/Texto",
                        prompt.contains("KlipNote/Texto")
                    )
                } else {
                    assertTrue(
                        "$mode EN must mention attached .txt",
                        prompt.contains("attached .txt", ignoreCase = true)
                    )
                    assertTrue(
                        "$mode EN must mention KlipNote/Texto",
                        prompt.contains("KlipNote/Texto")
                    )
                }
                assertTrue(
                    "$mode may mention title/source only",
                    prompt.contains("Mi fuente")
                )
            }
        }
    }

    @Test
    fun infographicAsksForActualImageOrCompleteSvg() {
        val spanish = AiStudyPrompts.intentFor(AiStudyMode.INFOGRAPHIC, true)
        val english = AiStudyPrompts.intentFor(AiStudyMode.INFOGRAPHIC, false)
        assertTrue(spanish.contains("INFOGRAFÍA VISUAL TERMINADA"))
        assertTrue(spanish.contains("SVG COMPLETO"))
        assertTrue(english.contains("FINISHED VISUAL INFOGRAPHIC"))
        assertTrue(english.contains("COMPLETE 1080"))
    }

    @Test
    fun websiteAndInteractiveActionsRequestRunnableHtml() {
        for (mode in listOf(
            AiStudyMode.WEBSITE_HTML,
            AiStudyMode.INTERACTIVE_APP,
            AiStudyMode.DATA_DASHBOARD,
            AiStudyMode.SLIDES,
            AiStudyMode.TIMELINE,
            AiStudyMode.FLASHCARDS,
            AiStudyMode.PODCAST,
            AiStudyMode.CHECKLIST
        )) {
            assertTrue(
                "$mode must request an HTML artifact",
                AiStudyPrompts.intentFor(mode, true).contains("HTML")
            )
            assertTrue(
                "$mode must request an English HTML artifact",
                AiStudyPrompts.intentFor(mode, false).contains("HTML")
            )
            assertTrue(mode.isCreative())
        }
    }

    @Test
    fun masterPromptBuilderIsTheOnlyTextFirstPromptCreationAction() {
        assertFalse(AiStudyMode.BUILD_PROMPT.isCreative())
        assertTrue(
            AiStudyPrompts.intentFor(AiStudyMode.BUILD_PROMPT, true)
                .contains("PROMPT MAESTRO")
        )
        assertTrue(
            AiStudyPrompts.intentFor(AiStudyMode.INFOGRAPHIC, true)
                .contains("NUNCA entregues un prompt")
        )
    }

    @Test
    fun diagramsRequestCompleteMermaidOrSvgArtifacts() {
        val spanish = AiStudyPrompts.intentFor(AiStudyMode.DIAGRAMS, true)
        val english = AiStudyPrompts.intentFor(AiStudyMode.DIAGRAMS, false)
        assertTrue(spanish.contains("Mermaid"))
        assertTrue(spanish.contains("SVG"))
        assertTrue(english.contains("Mermaid"))
        assertTrue(english.contains("SVG"))
        assertTrue(AiStudyMode.DIAGRAMS.isCreative())
    }

    @Test
    fun creativeAndPrimaryCountsMatchUiContract() {
        val primary = AiStudyMode.entries.filter { !it.isCreative() }
        val creative = AiStudyMode.entries.filter { it.isCreative() }
        assertEquals(6, primary.size)
        assertEquals(10, creative.size)
        assertEquals(
            setOf(
                AiStudyMode.SUMMARY,
                AiStudyMode.BRIEF,
                AiStudyMode.DEEP_RESEARCH,
                AiStudyMode.BUILD_PROMPT,
                AiStudyMode.STUDY_QUIZ,
                AiStudyMode.CLEAN_SCRIPT
            ),
            primary.toSet()
        )
    }

    @Test
    fun sendWithAiPackCombinesFourDeliverablesAndReferencesAttachedTxt() {
        assertEquals(
            listOf(
                AiStudyMode.BRIEF,
                AiStudyMode.INFOGRAPHIC,
                AiStudyMode.WEBSITE_HTML,
                AiStudyMode.SLIDES
            ),
            AiStudyPrompts.SEND_WITH_AI_MODES
        )
        val title = "Demo KlipNote 1.9.3"
        for (spanish in listOf(true, false)) {
            val prompt = AiStudyPrompts.composeSendWithAiPack(
                es = spanish,
                languageRule = if (spanish) "Idioma: español." else "Language: English.",
                sourceTitleOrUrl = title
            )
            for (n in 1..4) {
                assertTrue(
                    "Pack (es=$spanish) must include section ### $n)",
                    prompt.contains("### $n)")
                )
            }
            for (mode in AiStudyPrompts.SEND_WITH_AI_MODES) {
                assertTrue(
                    "Pack (es=$spanish) must reuse intentFor($mode)",
                    prompt.contains(AiStudyPrompts.intentFor(mode, spanish))
                )
            }
            assertTrue(
                "Pack (es=$spanish) must mention KlipNote/Texto",
                prompt.contains("KlipNote/Texto")
            )
            assertTrue(
                "Pack (es=$spanish) must include source title",
                prompt.contains(title)
            )
            assertFalse(
                "Pack (es=$spanish) must never embed <<<TRANSCRIPT_START>>>",
                prompt.contains("<<<TRANSCRIPT_START>>>") ||
                    prompt.contains("<<<TRANSCRIPT_END>>>")
            )
        }
    }
}
