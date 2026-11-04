package com.farmverse.farmverse_chatbot.agro;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class Translit {

    private Translit() {
    }

    private static final Map<Character, String> TELUGU = new HashMap<>();
    private static final Map<Character, String> DEVANAGARI = new HashMap<>();

    private static void put(Map<Character, String> map, String chars, String skeleton) {
        for (char c : chars.toCharArray()) {
            map.put(c, skeleton);
        }
    }

    static {
        put(TELUGU, "కఖ", "k");
        put(TELUGU, "గఘ", "g");
        put(TELUGU, "చఛ", "c");
        put(TELUGU, "జఝ", "j");
        put(TELUGU, "టఠతథ", "t");
        put(TELUGU, "డఢదధ", "d");
        put(TELUGU, "ఙఞణన", "n");
        put(TELUGU, "పఫ", "p");
        put(TELUGU, "బభ", "b");
        put(TELUGU, "మ", "m");
        put(TELUGU, "రఱ", "r");
        put(TELUGU, "లళ", "l");
        put(TELUGU, "వ", "v");
        put(TELUGU, "శషస", "s");

        put(DEVANAGARI, "कख", "k");
        put(DEVANAGARI, "गघ", "g");
        put(DEVANAGARI, "चछ", "c");
        put(DEVANAGARI, "जझ", "j");
        put(DEVANAGARI, "टठतथ", "t");
        put(DEVANAGARI, "डढदध", "d");
        put(DEVANAGARI, "णन", "n");
        put(DEVANAGARI, "पफ", "p");
        put(DEVANAGARI, "बभ", "b");
        put(DEVANAGARI, "म", "m");
        put(DEVANAGARI, "र", "r");
        put(DEVANAGARI, "ल", "l");
        put(DEVANAGARI, "व", "v");
        put(DEVANAGARI, "शषस", "s");
    }

    public static boolean isScript(String word) {
        return word.codePoints().anyMatch(cp -> {
            Character.UnicodeScript s = Character.UnicodeScript.of(cp);
            return s == Character.UnicodeScript.TELUGU || s == Character.UnicodeScript.DEVANAGARI;
        });
    }

    public static String skeleton(String word) {
        StringBuilder sb = new StringBuilder();
        if (isScript(word)) {
            for (char c : word.toCharArray()) {
                String s = TELUGU.get(c);
                if (s == null) {
                    s = DEVANAGARI.get(c);
                }
                if (s != null) {
                    sb.append(s);
                }
            }
        } else {
            String w = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            w = w.replace("sh", "s").replace("ch", "c").replace("th", "t").replace("dh", "d").replace("kh", "k")
                    .replace("gh", "g").replace("ph", "p").replace("bh", "b").replace("jh", "j").replace("ck", "k")
                    .replace("w", "v").replace("q", "k").replace("z", "j").replace("x", "ks");
            for (char c : w.toCharArray()) {
                if ("aeiouyh".indexOf(c) < 0) {
                    sb.append(c);
                }
            }
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sb.length(); i++) {
            if (i == 0 || sb.charAt(i) != sb.charAt(i - 1)) {
                out.append(sb.charAt(i));
            }
        }
        return out.toString();
    }
}
