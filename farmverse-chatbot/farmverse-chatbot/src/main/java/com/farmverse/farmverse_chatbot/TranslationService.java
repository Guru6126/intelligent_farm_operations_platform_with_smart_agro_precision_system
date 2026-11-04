package com.farmverse.farmverse_chatbot;

import org.springframework.stereotype.Service;

@Service
public class TranslationService {

    private final OllamaService ollama;

    public TranslationService(OllamaService ollama) {
        this.ollama = ollama;
    }

    public String toEnglish(String text, String lang) {
        String prompt = """
                Translate the following farming question from %s into English.
                The text may be written in the native script or in English letters
                (romanized, for example "Farmers year ki two crops enduku vestaru?").
                Output ONLY the English translation. No notes, no quotes.

                %s
                """.formatted(LanguageSupport.promptName(lang), text);
        return clean(ollama.askMultilingual(prompt));
    }

    private static String styleRule(String lang, boolean roman) {
        String name = LanguageSupport.promptName(lang);
        if (roman) {
            return "Write the reply in " + name + " using English letters only (the way people text in India, "
                    + "for example \"Farmers year lo two crops vestaru because it helps them increase income\"). "
                    + "Keep common English farming words such as crops, water, fertilizer, soil, seeds, income, "
                    + "production and season as they are. Do not use Telugu or Devanagari script.";
        }
        return "Write the reply ONLY in " + name + ", using its native script. "
                + "Never write " + name + " words in English letters.";
    }

    public String fromEnglish(String text, String lang, boolean roman) {
        String prompt = """
                Translate the following farming answer from English into %s.
                %s
                Keep numbers, units, list formatting and line breaks.
                Output ONLY the translation. No notes.

                %s
                """.formatted(LanguageSupport.promptName(lang), styleRule(lang, roman), text);
        return clean(ollama.askMultilingual(prompt));
    }

    public String answerIn(String question, String lang, boolean roman) {
        return answerIn(question, lang, roman, "");
    }

    public String answerIn(String question, String lang, boolean roman, String context) {
        String prompt = """
                You are the FarmVerse Nexus agriculture assistant.

                %s

                Instructions:
                1. Answer the farmer's question directly in 2 to 5 short, practical sentences.
                2. Give the real reasons or steps. Do not answer a different question.
                3. If the question is unrelated to agriculture, politely say you help
                   with agriculture.
                4. Do not claim the answer came from a dataset.
                5. Do not invent scientific facts or guarantee crop yields.
                6. For pesticide dosage or high-risk advice, recommend local
                   agricultural experts and the product label.
                %s
                Farmer's question:
                %s
                """.formatted(styleRule(lang, roman), contextBlock(context), question);
        return clean(ollama.askMultilingual(prompt));
    }

    public static String contextBlock(String context) {
        if (context == null || context.isBlank()) {
            return "";
        }
        return "7. Use the reference notes below if they are relevant to the question. They come from the\n"
                + "   FarmVerse database. Ignore any note that is about a different crop or topic.\n\n"
                + "Reference notes:\n" + context + "\n";
    }

    public static String formatContext(java.util.List<AgricultureDataService.Match> matches) {
        StringBuilder sb = new StringBuilder();
        int n = 1;
        for (AgricultureDataService.Match m : matches) {
            sb.append(n++).append(". Q: ").append(m.question()).append("\n   A: ").append(m.answer()).append("\n");
        }
        return sb.toString();
    }

    public String ensureLanguage(String text, String lang, boolean roman) {
        if (LanguageSupport.writtenIn(text, lang, roman)) {
            return text;
        }
        String prompt = """
                Rewrite the following farming text completely in %s.
                %s
                Keep the meaning, numbers, units and line breaks. Output ONLY the rewritten text.

                %s
                """.formatted(LanguageSupport.promptName(lang), styleRule(lang, roman), text);
        String fixed = clean(ollama.askMultilingual(prompt));
        return LanguageSupport.writtenIn(fixed, lang, roman) ? fixed : null;
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        String out = s.trim();
        if (out.startsWith("```")) {
            out = out.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        while (out.length() >= 2 && out.startsWith("\"") && out.endsWith("\"")) {
            out = out.substring(1, out.length() - 1).trim();
        }
        return out;
    }
}
