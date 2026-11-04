package com.farmverse.farmverse_chatbot;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LanguageSupport {

    private LanguageSupport() {
    }

    private static final Pattern LATIN_WORD = Pattern.compile("[a-z]+");

    private static final Set<String> TELUGU_ROMAN = Set.of(
            "enduku", "endhuku", "ela", "emiti", "evaru", "ekkada", "eppudu", "entha", "enni",
            "cheyali", "cheyyali", "cheyyandi", "cheppandi", "cheppu", "vestaru", "vestharu",
            "veyali", "veyyali", "vastundi", "vachu", "ravali", "kavali", "kavalante",
            "undi", "untundi", "untayi", "ledu", "ledhu", "meeru", "nenu", "naku", "maku", "manam",
            "panta", "pantalu", "pandinchali", "pandinchadam", "polam", "polalu", "nela", "nelalo",
            "neeru", "neellu", "vyavasayam", "raithu", "raithulu", "eruvulu", "erubaalu",
            "purugulu", "purugu", "godhuma", "mirapa", "senaga", "baaga", "bagundi", "ikkada",
            "akkada", "kosam", "gurinchi", "vaadali", "vadali", "avuthundi", "avutundi", "ayye",
            "dhenivalla", "deenivalla", "mundu", "tarvata", "tharvatha", "cheyochu", "chesukovachu"
    );

    private static final Set<String> HINDI_ROMAN = Set.of(
            "kaise", "kaisa", "kaisi", "kya", "kyun", "kyon", "kyu", "kab", "kahan", "kaun",
            "kitna", "kitni", "kitne", "hai", "hain", "hota", "hoti", "hote", "karna", "karein",
            "karte", "kisan", "kisaan", "kheti", "fasal", "fasalen", "fasalein", "paani", "mitti",
            "khad", "beej", "sinchai", "bimari", "keede", "upay", "tarika", "tareeka", "jaankari",
            "batao", "bataiye", "batayein", "chahiye", "sakte", "sakta", "sakti", "lagaye",
            "lagana", "ugaye", "ugana", "ugate", "mein", "aur", "nahi", "nahin", "liye", "saal",
            "zameen", "jamin", "gehun", "chawal", "makka", "sarso", "pyaz", "aalu", "tamatar"
    );

    private static final Set<String> ENGLISH_WORDS = Set.of(
            "the", "is", "are", "what", "how", "why", "when", "where", "which", "who", "can", "could",
            "should", "do", "does", "did", "to", "of", "for", "in", "on", "my", "your", "i", "a", "an",
            "and", "or", "with", "from", "about", "please", "tell", "me", "best", "grow", "much", "many"
    );

    public static String detect(String text, String hint) {
        int telugu = 0;
        int devanagari = 0;

        if (text != null) {
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                Character.UnicodeScript script = Character.UnicodeScript.of(cp);
                if (script == Character.UnicodeScript.TELUGU) {
                    telugu++;
                } else if (script == Character.UnicodeScript.DEVANAGARI) {
                    devanagari++;
                }
            }
        }

        if (telugu > 0 && telugu >= devanagari) {
            return "te";
        }
        if (devanagari > 0) {
            return "hi";
        }

        int romanTe = 0;
        int romanHi = 0;
        int english = 0;
        if (text != null) {
            Matcher m = LATIN_WORD.matcher(text.toLowerCase(Locale.ROOT));
            while (m.find()) {
                String w = m.group();
                if (TELUGU_ROMAN.contains(w)) {
                    romanTe++;
                } else if (HINDI_ROMAN.contains(w)) {
                    romanHi++;
                } else if (ENGLISH_WORDS.contains(w)) {
                    english++;
                }
            }
        }

        if (romanTe > 0 && romanTe >= romanHi) {
            return "te";
        }
        if (romanHi > 0) {
            return "hi";
        }
        if (english > 0) {
            return "en";
        }
        return normalize(hint);
    }

    public static boolean isRoman(String text) {
        if (text == null) {
            return true;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.TELUGU || script == Character.UnicodeScript.DEVANAGARI) {
                return false;
            }
        }
        return true;
    }

    public static boolean writtenIn(String text, String lang, boolean roman) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int letters = 0;
        int telugu = 0;
        int devanagari = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetter(cp)) {
                continue;
            }
            letters++;
            Character.UnicodeScript script = Character.UnicodeScript.of(cp);
            if (script == Character.UnicodeScript.TELUGU) {
                telugu++;
            } else if (script == Character.UnicodeScript.DEVANAGARI) {
                devanagari++;
            }
        }
        if (letters == 0) {
            return false;
        }
        if ("en".equals(lang) || roman) {
            return telugu == 0 && devanagari == 0;
        }
        int target = "te".equals(lang) ? telugu : devanagari;
        return target >= 0.4 * letters;
    }

    public static String normalize(String code) {
        if (code == null) {
            return "en";
        }
        String c = code.trim().toLowerCase(Locale.ROOT);
        if (c.startsWith("te")) {
            return "te";
        }
        if (c.startsWith("hi")) {
            return "hi";
        }
        return "en";
    }

    public static boolean modelMatches(String installed, String wanted) {
        if (installed == null || wanted == null) {
            return false;
        }
        String i = installed.trim().toLowerCase(Locale.ROOT);
        String w = wanted.trim().toLowerCase(Locale.ROOT);
        if (i.equals(w) || i.equals(w + ":latest")) {
            return true;
        }
        return !w.contains(":") && i.startsWith(w + ":");
    }

    public static String promptName(String lang) {
        switch (lang) {
            case "te":
                return "Telugu (తెలుగు)";
            case "hi":
                return "Hindi (हिन्दी)";
            default:
                return "English";
        }
    }
}
