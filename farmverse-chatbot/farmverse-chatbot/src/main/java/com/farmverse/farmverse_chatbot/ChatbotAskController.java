package com.farmverse.farmverse_chatbot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotAskController {

    private static final String DATASET_SOURCE = "FarmVerse agriculture database (MySQL)";
    private static final String RECORDS_SOURCE = "FarmVerse farm records (agroprecision database)";
    private static final String AI_SOURCE = "Local AI (Ollama)";
    private static final String RAG_SOURCE = "Local AI (Ollama) + database context (RAG)";
    private static final int RAG_ENTRIES = 3;

    private final AgricultureDataService agricultureDataService;
    private final OllamaService ollamaService;
    private final TranslationService translationService;
    private final FarmerRecordsService farmerRecords;

    public ChatbotAskController(
            AgricultureDataService agricultureDataService,
            OllamaService ollamaService,
            TranslationService translationService,
            FarmerRecordsService farmerRecords) {

        this.farmerRecords = farmerRecords;
        this.agricultureDataService = agricultureDataService;
        this.ollamaService = ollamaService;
        this.translationService = translationService;
    }

    @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> request) {

        String question = request.get("question");

        if (question == null || question.isBlank()) {
            return reply("", "Please enter an agriculture question.",
                    "FarmVerse Nexus Chatbot", "en", null);
        }

        question = question.trim();
        String lang = LanguageSupport.detect(question, request.get("language"));

        boolean roman = LanguageSupport.isRoman(question);

        Optional<com.farmverse.farmverse_chatbot.agro.FarmerChat.Result> records =
                farmerRecords.answer(question, lang, roman);
        if (records.isPresent()) {
            return reply(question, records.get().answer(), RECORDS_SOURCE, lang, null);
        }

        return "en".equals(lang) ? askEnglish(question) : askOtherLanguage(question, lang, roman);
    }

    @GetMapping("/selftest")
    public List<Map<String, Object>> selftest() {
        String[][] cases = {
                {"What is crop rotation?", "en", "false"},
                {"పంట మార్పిడి అంటే ఏమిటి?", "te", "false"},
                {"फसल चक्र क्या है?", "hi", "false"},
                {"Farmers year ki two crops enduku vestaru?", "te", "true"}
        };
        List<Map<String, Object>> report = new ArrayList<>();
        for (String[] c : cases) {
            Map<String, String> result = ask(Map.of("question", c[0]));
            String answer = result.getOrDefault("answer", "");
            boolean roman = Boolean.parseBoolean(c[2]);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", c[0]);
            row.put("expectedLanguage", c[1] + (roman ? " (English letters)" : ""));
            row.put("languageOk", LanguageSupport.writtenIn(answer, c[1], roman));
            row.put("source", result.get("source"));
            row.put("note", result.get("note"));
            row.put("answer", answer.length() > 300 ? answer.substring(0, 300) + "..." : answer);
            report.add(row);
        }
        return report;
    }

    private Map<String, String> askEnglish(String question) {

        Optional<AgricultureDataService.Match> match =
                agricultureDataService.findAnswer(question);

        if (match.isPresent()) {
            return reply(question, cleanAnswer(match.get().answer()),
                    DATASET_SOURCE, "en", null);
        }

        List<AgricultureDataService.Match> related =
                agricultureDataService.relatedEntries(question, RAG_ENTRIES);

        String prompt = """
                You are the FarmVerse Nexus agriculture assistant.

                Answer the farmer's question in simple English.

                Instructions:
                1. Give a clear, helpful agriculture-related answer.
                2. If the question is unrelated to agriculture,
                   politely explain that you help with agriculture.
                3. Do not claim that you retrieved information
                   from the dataset.
                4. Do not invent scientific facts or guarantee
                   crop yields.
                5. For pesticide dosage or high-risk farming advice,
                   recommend checking local agricultural experts
                   and product label instructions.
                6. Keep the answer concise and practical.
                %s
                Farmer's question:
                %s

                Answer:
                """.formatted(TranslationService.contextBlock(TranslationService.formatContext(related)), question);

        try {
            String aiAnswer = cleanAnswer(ollamaService.askMultilingual(prompt));
            if (!aiAnswer.isBlank()) {
                return reply(question, aiAnswer, related.isEmpty() ? AI_SOURCE : RAG_SOURCE, "en", null);
            }
        } catch (Exception exception) {
            System.err.println("Ollama AI fallback failed: " + exception.getMessage());
        }

        return unavailable(question, "en");
    }

    private Map<String, String> askOtherLanguage(String question, String lang, boolean roman) {

        String missingModelNote = ollamaService.isMultilingualModelInstalled() ? null
                : "Telugu/Hindi model is not installed, so this answer may be wrong. Run: ollama pull "
                + ollamaService.multilingualModelName();

        try {

            String englishQuestion = translationService.toEnglish(question, lang);
            System.out.println("Translated question (" + lang + "): " + englishQuestion);

            Optional<AgricultureDataService.Match> match = englishQuestion.isBlank()
                    ? Optional.empty()
                    : agricultureDataService.findAnswer(englishQuestion, true);

            if (match.isPresent()) {
                String englishAnswer = cleanAnswer(match.get().answer());

                try {
                    String translated = translationService.ensureLanguage(
                            translationService.fromEnglish(englishAnswer, lang, roman), lang, roman);
                    if (translated != null && !translated.isBlank()) {
                        return reply(question, translated, DATASET_SOURCE, lang, missingModelNote);
                    }
                } catch (Exception e) {
                    System.err.println("Answer translation failed: " + e.getMessage());
                }
                return reply(question, englishAnswer, DATASET_SOURCE, "en",
                        "Could not translate this answer, so it is shown in English."
                                + (missingModelNote == null ? "" : " " + missingModelNote));
            }

            List<AgricultureDataService.Match> related = englishQuestion.isBlank() ? List.of()
                    : agricultureDataService.relatedEntries(englishQuestion, RAG_ENTRIES);
            String aiAnswer = cleanAnswer(translationService.answerIn(question, lang, roman,
                    TranslationService.formatContext(related)));
            if (!aiAnswer.isBlank()) {
                String checked = translationService.ensureLanguage(aiAnswer, lang, roman);
                if (checked != null) {
                    return reply(question, checked, related.isEmpty() ? AI_SOURCE : RAG_SOURCE, lang, missingModelNote);
                }

                return reply(question, aiAnswer, related.isEmpty() ? AI_SOURCE : RAG_SOURCE, "en",
                        "Could not write this answer in " + LanguageSupport.promptName(lang)
                                + ". Install a stronger model: ollama pull "
                                + ollamaService.multilingualModelName());
            }
        } catch (Exception exception) {
            System.err.println("Multilingual answer failed: " + exception.getMessage());
        }

        return unavailable(question, lang);
    }

    private Map<String, String> unavailable(String question, String lang) {
        return reply(question,
                "Sorry, the dataset has no sufficiently relevant answer "
                        + "and the AI service is currently unavailable. "
                        + "Please try again.",
                "Fallback unavailable", lang, null);
    }

    private Map<String, String> reply(String question, String answer,
                                      String source, String language, String note) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("answer", answer);
        body.put("source", source);
        body.put("sourceType", RECORDS_SOURCE.equals(source) ? "farm-records"
                : DATASET_SOURCE.equals(source) ? "database"
                : RAG_SOURCE.equals(source) ? "ollama-rag"
                : AI_SOURCE.equals(source) ? "ollama" : "none");
        body.put("language", language);
        if (note != null) {
            body.put("note", note);
        }
        return body;
    }

    private String cleanAnswer(String answer) {
        if (answer == null) {
            return "";
        }

        String cleaned = answer.trim();

        while (cleaned.length() >= 2
                && cleaned.startsWith("\"")
                && cleaned.endsWith("\"")) {

            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        return cleaned;
    }
}
