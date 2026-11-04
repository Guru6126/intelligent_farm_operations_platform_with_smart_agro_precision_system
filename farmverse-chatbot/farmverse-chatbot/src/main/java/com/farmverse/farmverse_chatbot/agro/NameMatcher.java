package com.farmverse.farmverse_chatbot.agro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.farmverse.farmverse_chatbot.agro.FarmerData.Farmer;

public final class NameMatcher {

    private NameMatcher() {
    }

    public static List<Farmer> joinedExact(List<Farmer> all, List<String> tokens) {
        List<Farmer> out = new ArrayList<>();
        for (int i = 0; i + 1 < tokens.size(); i++) {
            String joined = (tokens.get(i) + tokens.get(i + 1)).toLowerCase(Locale.ROOT);
            for (Farmer f : all) {
                if (f.name().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}]", "").equals(joined) && !out.contains(f)) {
                    out.add(f);
                }
            }
        }
        return out;
    }

    public static List<Farmer> findSpoken(List<Farmer> all, List<String> tokens) {
        List<String> orig = new ArrayList<>();
        for (String t : tokens) {
            String l = t.toLowerCase(Locale.ROOT);
            if (l.length() >= 2 && !orig.contains(l)) {
                orig.add(l);
            }
        }
        if (orig.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> all2 = new ArrayList<>(orig);
        for (int i = 0; i + 1 < orig.size(); i++) {
            all2.add(orig.get(i) + orig.get(i + 1));
        }

        List<Farmer> best = new ArrayList<>();
        int bestScore = 0;
        for (Farmer f : all) {
            List<String> parts = new ArrayList<>();
            Matcher nm = TOKEN.matcher(f.name().toLowerCase(Locale.ROOT));
            while (nm.find()) {
                parts.add(nm.group());
            }
            String whole = String.join("", parts);
            int total = 0;
            int covered = 0;
            boolean strong = false;
            for (String part : parts) {
                int top = 0;
                for (String q : orig) {
                    top = Math.max(top, strength(q, part));
                }
                if (top > 0) {
                    covered++;
                    total += top;
                    strong |= top == 2;
                }
            }
            if (covered < parts.size()) {
                int w = 0;
                for (String q : all2) {
                    w = Math.max(w, strength(q, whole));
                }
                if (w == 2) {
                    covered = parts.size();
                    total = 2 * parts.size();
                    strong = true;
                }
            }
            boolean ok = orig.size() == 1 ? strong && total >= 2 : covered == parts.size() && strong && total >= 2;
            if (ok && total > bestScore) {
                bestScore = total;
                best.clear();
                best.add(f);
            } else if (ok && total == bestScore) {
                best.add(f);
            }
        }
        return best;
    }

    private static int strength(String q, String name) {
        if (q.equals(name)) {
            return 2;
        }
        String a = Translit.skeleton(q);
        String b = Translit.skeleton(name);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b) && a.length() >= 2) {
            return 2;
        }
        if (q.length() >= 5 && name.length() >= 5 && distance(q, name) <= 1) {
            return 2;
        }
        if (Math.abs(a.length() - b.length()) == 1 && (a.startsWith(b) || b.startsWith(a))) {
            return 1;
        }
        return 0;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                cur[j] = Math.min(Math.min(cur[j - 1], prev[j]) + 1,
                        prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
            prev = cur;
        }
        return prev[b.length()];
    }

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{M}]+");

    public static List<Farmer> find(List<Farmer> all, String question) {
        List<String> query = new ArrayList<>();
        Matcher m = TOKEN.matcher(question);
        while (m.find()) {
            String sk = Translit.skeleton(m.group());
            if (!sk.isEmpty()) {
                query.add(sk);
            }
        }

        List<Farmer> best = new ArrayList<>();
        int bestScore = 0;
        for (Farmer f : all) {
            List<String> parts = new ArrayList<>();
            Matcher nm = TOKEN.matcher(f.name().toLowerCase(Locale.ROOT));
            while (nm.find()) {
                String sk = Translit.skeleton(nm.group());
                if (!sk.isEmpty()) {
                    parts.add(sk);
                }
            }
            int score = 0;
            for (String name : parts) {
                int top = 0;
                for (String q : query) {
                    top = Math.max(top, pair(q, name));
                }
                score += top;
            }
            if (score > bestScore) {
                bestScore = score;
                best.clear();
                best.add(f);
            } else if (score == bestScore && score > 0) {
                best.add(f);
            }
        }
        return bestScore >= 2 ? best : new ArrayList<>();
    }

    private static int pair(String q, String name) {
        if (q.equals(name) && q.length() >= 2) {
            return 2;
        }
        if (name.length() >= 1 && Math.abs(q.length() - name.length()) == 1
                && (q.startsWith(name) || name.startsWith(q)) && Math.min(q.length(), name.length()) >= 1) {
            return 1;
        }
        return 0;
    }
}
