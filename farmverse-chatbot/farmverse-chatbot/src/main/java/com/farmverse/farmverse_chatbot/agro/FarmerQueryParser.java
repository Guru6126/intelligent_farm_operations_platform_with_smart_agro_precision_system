package com.farmverse.farmverse_chatbot.agro;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FarmerQueryParser {

    private FarmerQueryParser() {
    }

    private static final Pattern TOKEN = Pattern.compile("[\\p{L}\\p{M}\\p{N}']+");

    private static final Set<String> HARVEST = Set.of(
            "harvest", "harvests", "harvested", "harvesting", "yield", "yields", "produce", "production",
            "kotha", "kothalu", "dhigubadi", "digubadi", "pandina", "katai", "paidavar", "paidawar", "upaj",
            "కోత", "కోతలు", "దిగుబడి", "పండించ", "కోసిన", "కోశ", "కోసా", "कटाई", "पैदावार", "उपज", "फसल कटाई");
    private static final Set<String> CROP = Set.of(
            "crop", "crops", "panta", "pantalu", "fasal", "fasalein", "fasalen",
            "పంట", "పంటలు", "సాగు", "फसल", "फसलें", "फसलों");
    private static final Set<String> FARM = Set.of(
            "farm", "farms", "land", "polam", "polalu", "khet", "kheti", "జాగా", "పొలం", "పొలాలు", "खेत", "खेतों");
    private static final Set<String> PLURAL = Set.of("farmers", "users", "raithulu", "rythulu", "kisanulu", "రైతులు", "రైతుల",
            "కర్షకులు", "किसानों", "उपयोगकर्ताओं");
    private static final Set<String> ALL_WORDS = Set.of("all", "every", "each", "everyone", "everybody", "whole", "entire",
            "andaru", "andari", "andariki", "anni", "అందరు", "అందరి", "అందరూ", "అన్ని", "सभी", "सारे", "सबके");
    private static final Set<String> WHO = Set.of("who", "evaru", "evarevaru", "ఎవరు", "ఎవరెవరు", "कौन", "kaun");
    private static final Set<String> TOTAL_WORDS = Set.of("total", "sum", "overall", "altogether", "combined", "aggregate",
            "motham", "మొత్తం", "కలిపి", "कुल", "kul");
    private static final Set<String> QUANTITY = Set.of("quantity", "quantities", "kg", "kgs", "weight", "amount", "tons",
            "tonnes", "quintals", "పరిమాణం", "బరువు", "मात्रा", "वज़न", "parimanam");
    private static final Set<String> SYSTEM_WORDS = Set.of("system", "database", "mysql", "db", "records", "registered");
    private static final Set<String> LIST_VERBS = Set.of("list", "show", "display", "which", "who", "names", "name",
            "enumerate", "chupinchu", "chupinchandi", "cheppandi", "జాబితా", "చూపించు", "చూపించండి", "పేర్లు", "సूची",
            "सूची", "दिखाओ", "दिखाइए", "नाम", "dikhao", "dikhaiye", "evaru", "ye", "ఏ");
    private static final Set<String> COUNT_WORDS = Set.of("count", "number", "many", "ఎంత", "కितने", "कितने", "kitne", "entha",
            "enni", "ఎన్ని");
    private static final Set<String> NEGATIVE = Set.of("not", "never", "without", "haven't", "hasn't", "havent", "hasnt",
            "yet", "pending", "inka", "ఇంకా", "లేదు", "లేని", "नहीं", "अभी", "ledu");
    private static final Set<String> REGISTERED = Set.of("registered", "registration", "register", "enrolled", "enrolment",
            "exist", "exists", "namodu", "నమోదు", "నమోదైన", "पंजीकृत", "पंजीकरण");
    private static final Set<String> GENERIC_QUESTION = Set.of("use", "used", "using", "can", "could", "should", "manage",
            "control", "monitor", "help", "helps", "tools", "ways", "methods", "prevent", "improve",
            "grow", "growing", "apply", "gather", "reduce", "protect", "increase");
    private static final Set<String> LIST_NOUNS = Set.of("list", "names", "name", "జాబితా", "పేర్లు", "सूची", "नाम");
    private static final Set<String> SCOPE = Set.of("registered", "growing", "currently", "total", "records", "record",
            "recorded", "system", "database", "db", "mysql", "stored", "entered", "ప్రస్తుతం", "నమోదైన", "మొత్తం",
            "कुल", "पंजीकृत", "वर्तमान", "motham", "namodu");
    private static final Set<String> COUNT_BLOCK = Set.of("should", "need", "needs", "per", "acre", "acres", "each",
            "plant", "sow", "require", "required", "use", "apply", "season", "kharif", "rabi", "ideal", "best",
            "recommended", "optimum", "hectare", "hectares", "cuttings", "seeds");
    private static final Set<String> FARMER_NOUN = Set.of("farmer", "farmers", "raithu", "rythu", "kisan", "రైతు", "రైతులు",
            "किसान");
    private static final Set<String> HIGH = Set.of("highest", "most", "top", "maximum", "max", "largest", "biggest",
            "greatest", "best", "ఎక్కువ", "అత్యధిక", "सबसे", "अधिकतम", "ekkuva");
    private static final Set<String> LOW = Set.of("lowest", "least", "minimum", "min", "smallest", "fewest", "తక్కువ",
            "అత్యల్ప", "न्यूनतम", "takkuva");
    private static final Set<String> RECORD = Set.of(
            "records", "record", "history", "details", "detail", "data", "report", "summary", "profile",
            "వివరాలు", "చరిత్ర", "రికార్డులు", "विवरण", "इतिहास", "रिकॉर्ड", "vivaralu", "vivarana", "jaankari");

    private static final String[] LAST_MONTH = {
            "last month", "previous month", "past month", "last months", "gata nela", "poyina nela", "pichle mahine",
            "pichhle mahine", "pichle mahina", "గత నెల", "పోయిన నెల", "పోయిన నెలలో", "క్రితం నెల",
            "पिछले महीने", "पिछले माह", "पिछला महीना", "बीते महीने"};
    private static final String[] THIS_MONTH = {
            "this month", "current month", "ee nela", "is mahine", "ఈ నెల", "इस महीने", "इस माह"};

    private static final Set<String> STOP = new LinkedHashSet<>(List.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "do", "does", "did", "what", "which", "who",
            "whom", "how", "when", "where", "why", "can", "could", "should", "would", "will", "shall", "may", "of",
            "for", "in", "on", "at", "to", "from", "by", "with", "about", "and", "or", "not", "no", "please",
            "tell", "me", "show", "give", "list", "get", "fetch", "find", "display", "check", "my", "your", "his",
            "her", "their", "our", "i", "he", "she", "it", "they", "we", "you", "all", "any", "each", "every",
            "last", "previous", "past", "this", "that", "these", "those", "month", "months", "year", "week",
            "day", "today", "now", "much", "many", "total", "quantity", "quantities", "date", "dates", "name",
            "has", "have", "had", "grow", "grew", "grown", "growing", "plant", "planted", "records", "record",
            "details", "detail", "history", "data", "report", "summary", "profile", "info", "information",
            "farmer", "farmers", "user", "users", "farm", "farms", "crop", "crops", "harvest", "harvests",
            "harvested", "harvesting", "yield", "yields", "produce", "production", "land", "field", "fields",
            "named", "called", "registered", "registration", "register", "system", "all", "provide", "kind", "type",
            "season", "kharif", "rabi", "zaid", "there", "here", "any", "some", "if", "then", "than", "so",
            "provide", "retrieve", "view", "see", "need", "want", "look", "search", "send", "hello", "hi", "hey",
            "thanks", "thank", "kg", "kgs", "quintal", "quintals", "ton", "tons", "tonnes", "acre", "acres", "database", "db",

            "ki", "ku", "lo", "yokka", "gurinchi", "cheppu", "cheppandi", "chupinchu", "chupinchandi", "enti",
            "entha", "enni", "ela", "evaru", "ekkada", "eppudu", "ee", "aa", "ayina", "unnayi", "undi", "ledu",
            "ledhu", "kotha", "kothalu", "pandina", "panta", "pantalu", "polam", "polalu", "nela", "gata",
            "poyina", "vivaralu", "ka", "ke", "ki", "ko", "ne", "se", "mein", "me", "hai", "hain", "kya", "kitna",
            "kitni", "kitne", "batao", "bataiye", "dikhao", "dikhaiye", "pichle", "pichhle", "mahine", "mahina",
            "fasal", "fasalen", "khet", "kheti", "katai", "kisan", "raithu", "rythu", "vivaran", "jaankari"));

    private static final Set<String> NOT_A_PERSON = Set.of(
            "rice", "paddy", "wheat", "maize", "corn", "cotton", "sugarcane", "tomato", "potato", "onion", "chilli",
            "chili", "pepper", "groundnut", "peanut", "soybean", "soya", "millet", "sorghum", "jowar", "bajra",
            "ragi", "barley", "gram", "chickpea", "pulses", "lentil", "mustard", "sunflower", "banana", "mango",
            "grapes", "coconut", "tea", "coffee", "jute", "turmeric", "ginger", "garlic", "cabbage", "brinjal",
            "okra", "beans", "peas", "vegetables", "fruits", "grain", "grains", "seeds", "soil", "water", "fertilizer",
            "pesticide", "irrigation", "weather", "rain", "market", "price", "prices", "summer", "winter", "monsoon",
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october",
            "november", "december", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "telugu", "hindi", "english", "india", "andhra", "telangana", "pradesh");

    private static final Pattern AFTER_PREP = Pattern.compile("(?:\\bof|\\bfor|\\bby|\\bfrom|\\bfarmer|\\buser|\\bkisan)\\s+([\\p{L}][\\p{L}'.-]*(?:\\s+[\\p{L}][\\p{L}'.-]*){0,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern NAMED = Pattern.compile("(?:\\bnamed|\\bcalled|\\bname\\s+is|\\bname\\s*:)\\s+([\\p{L}][\\p{L}'.-]*(?:\\s+[\\p{L}][\\p{L}'.-]*){0,2})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CAPITAL_SEQUENCE = Pattern.compile("\\b[A-Z][\\p{L}'.-]*(?:\\s+[A-Z][\\p{L}'.-]*)*");
    private static final Pattern POSSESSIVE = Pattern.compile("([\\p{L}][\\p{L}-]{1,30})'s\\b", Pattern.UNICODE_CASE);
    private static final Pattern NAME_THEN_KI = Pattern.compile("([\\p{L}][\\p{L}-]{1,30})\\s+(?:ki|ku|ka|ke|yokka|gurinchi)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static FarmerQuery parse(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        String lower = question.toLowerCase(Locale.ROOT);
        List<String> tokens = tokens(lower);

        boolean harvest = any(tokens, lower, HARVEST);
        boolean crop = any(tokens, lower, CROP);
        boolean farm = any(tokens, lower, FARM);
        boolean record = any(tokens, lower, RECORD);

        FarmerQuery.Period period = FarmerQuery.Period.ALL;
        for (String p : LAST_MONTH) {
            if (lower.contains(p)) {
                period = FarmerQuery.Period.LAST_MONTH;
            }
        }
        if (period == FarmerQuery.Period.ALL) {
            for (String p : THIS_MONTH) {
                if (lower.contains(p)) {
                    period = FarmerQuery.Period.THIS_MONTH;
                }
            }
        }

        boolean reg = any(tokens, lower, REGISTERED);
        boolean qty = any(tokens, lower, QUANTITY);
        boolean plural = any(tokens, lower, PLURAL);
        boolean everyone = any(tokens, lower, ALL_WORDS) || any(tokens, lower, WHO);
        boolean total = any(tokens, lower, TOTAL_WORDS);
        boolean listVerb = any(tokens, lower, LIST_VERBS);
        boolean negative = any(tokens, lower, NEGATIVE);
        boolean howTo = lower.contains("how to") || lower.contains("how do") || lower.contains("how can")
                || lower.contains("when should") || lower.contains("when to") || lower.contains("best way")
                || lower.contains("best time");

        boolean countAsk = any(tokens, lower, COUNT_WORDS) || lower.contains("how many") || lower.contains("number of")
                || lower.contains("count of");
        boolean scoped = any(tokens, lower, SCOPE) && !any(tokens, lower, COUNT_BLOCK);
        boolean farmerNoun = any(tokens, lower, FARMER_NOUN);
        if (!howTo && countAsk && scoped && !plural) {
            FarmerQuery.Topic c = null;
            boolean harvestRecords = harvest || lower.contains("harvest");
            if (harvestRecords && (record || lower.contains("harvests") || lower.contains("harvest record"))) {
                c = FarmerQuery.Topic.HARVEST_COUNT;
            } else if (crop) {
                c = FarmerQuery.Topic.CROP_COUNT;
            } else if (farm) {
                c = FarmerQuery.Topic.FARM_COUNT;
            }
            if (c != null) {
                return new FarmerQuery(c, period, List.of(), null, false, List.of());
            }
        }
        if (!howTo && farmerNoun && (any(tokens, lower, HIGH) || any(tokens, lower, LOW))
                && (harvest || any(tokens, lower, QUANTITY) || tokens.contains("top") || tokens.contains("best"))
                && !any(tokens, lower, GENERIC_QUESTION)) {
            return new FarmerQuery(any(tokens, lower, HIGH) ? FarmerQuery.Topic.TOP_FARMER : FarmerQuery.Topic.LOW_FARMER,
                    period, List.of(), null, false, List.of());
        }
        boolean generic = any(tokens, lower, GENERIC_QUESTION);
        if (!howTo && !generic) {
            FarmerQuery.Topic all = null;
            if (plural && any(tokens, lower, COUNT_WORDS)) {
                all = FarmerQuery.Topic.FARMER_COUNT;
            } else if (total && (harvest || qty) && (plural || everyone || any(tokens, lower, SYSTEM_WORDS) || leftover(tokens) == 0)) {
                all = FarmerQuery.Topic.TOTAL_HARVEST;
            } else if (harvest && (plural && (listVerb || everyone) || any(tokens, lower, WHO))) {
                all = negative ? FarmerQuery.Topic.FARMERS_NOT_HARVESTED : FarmerQuery.Topic.FARMERS_HARVESTED;
            } else if (plural && (any(tokens, lower, SYSTEM_WORDS) || reg || everyone || any(tokens, lower, LIST_NOUNS))
                    && !crop && !farm && !harvest) {
                all = FarmerQuery.Topic.FARMER_LIST;
            }
            if (all != null) {
                return new FarmerQuery(all, period, List.of(), null, false, List.of());
            }
        }

        boolean bareName = !howTo && tokens.size() <= 3;
        if (bareName && !(harvest || crop || farm || record || reg || period != FarmerQuery.Period.ALL)) {
            return null;
        }

        FarmerQuery.Topic topic = reg && !harvest && !crop && !farm ? FarmerQuery.Topic.REGISTRATION
                : harvest || period != FarmerQuery.Period.ALL ? FarmerQuery.Topic.HARVEST
                : crop ? FarmerQuery.Topic.CROPS
                : farm ? FarmerQuery.Topic.FARMS
                : FarmerQuery.Topic.OVERVIEW;

        Set<String> words = new LinkedHashSet<>();
        for (String t : tokens) {
            String w = t.replaceAll("^'+|'+$", "").replace("'s", "");
            if (w.length() >= 3 && !STOP.contains(w) && !NOT_A_PERSON.contains(w) && !w.chars().allMatch(Character::isDigit)) {
                words.add(w);
            }
            if (words.size() >= 8) {
                break;
            }
        }

        boolean recordCue = !howTo && (record || reg || period != FarmerQuery.Period.ALL);
        List<String> name = extractName(question, recordCue);
        if (bareName && !(harvest || crop || farm || record || reg || period != FarmerQuery.Period.ALL)) {
            name = List.of();
        }
        String unknown = name.isEmpty() ? null : String.join(" ", name);
        List<String> nameWords = new ArrayList<>();
        for (String n : name) {
            nameWords.add(n.toLowerCase(Locale.ROOT));
        }

        return new FarmerQuery(topic, period, new ArrayList<>(words), unknown, unknown != null, nameWords);
    }

    private static List<String> extractName(String question, boolean recordCue) {
        Matcher named = NAMED.matcher(question);
        if (named.find()) {
            List<String> n = trimName(splitWords(named.group(1)), false);
            if (!n.isEmpty()) {
                return n;
            }
        }
        Matcher cap = CAPITAL_SEQUENCE.matcher(question);
        while (cap.find()) {
            List<String> n = trimName(splitWords(cap.group()), true);
            if (!n.isEmpty() && (recordCue || cap.start() > 0 && personCue(question, cap.start()))) {
                return n;
            }
        }
        Matcher m = AFTER_PREP.matcher(question);
        while (m.find()) {
            List<String> n = trimName(splitWords(m.group(1)), false);
            if (!n.isEmpty() && (Character.isUpperCase(n.get(0).charAt(0)) && m.start(1) > 0 || recordCue)) {
                return n;
            }
        }
        m = POSSESSIVE.matcher(question);
        while (m.find()) {
            List<String> n = trimName(splitWords(m.group(1)), false);
            if (!n.isEmpty() && (Character.isUpperCase(n.get(0).charAt(0)) && m.start(1) > 0 || recordCue)) {
                return n;
            }
        }
        m = NAME_THEN_KI.matcher(question);
        while (m.find()) {
            List<String> n = trimName(splitWords(m.group(1)), false);
            if (!n.isEmpty() && recordCue) {
                return n;
            }
        }
        return List.of();
    }

    private static final Set<String> BEFORE_NAME = Set.of("of", "for", "by", "named", "called", "farmer", "user",
            "did", "does", "do", "has", "have", "had", "is", "are", "was", "about", "from", "with", "kisan", "raithu");

    private static int leftover(List<String> tokens) {
        int n = 0;
        for (String t : tokens) {
            String w = t.replaceAll("^'+|'+$", "").replace("'s", "");
            if (w.length() >= 3 && !STOP.contains(w) && !NOT_A_PERSON.contains(w) && !w.chars().allMatch(Character::isDigit)) {
                n++;
            }
        }
        return n;
    }

    private static boolean personCue(String question, int start) {
        String before = question.substring(0, start).trim().toLowerCase(Locale.ROOT);
        int cut = before.lastIndexOf(' ');
        String prev = cut < 0 ? before : before.substring(cut + 1);
        return BEFORE_NAME.contains(prev);
    }

    private static List<String> splitWords(String text) {
        List<String> out = new ArrayList<>();
        for (String w : text.trim().split("\\s+")) {
            String c = w.replaceAll("^[.'-]+|[.'-]+$", "").replace("'s", "");
            if (!c.isEmpty()) {
                out.add(c);
            }
        }
        return out;
    }

    private static List<String> trimName(List<String> words, boolean capitalOnly) {
        List<String> out = new ArrayList<>();
        for (String w : words) {
            String lw = w.toLowerCase(Locale.ROOT);
            boolean bad = w.length() < 2 || STOP.contains(lw) || NOT_A_PERSON.contains(lw)
                    || (capitalOnly && !Character.isUpperCase(w.charAt(0)));
            if (bad) {
                if (out.isEmpty()) {
                    continue;
                }
                break;
            }
            out.add(w);
            if (out.size() == 3) {
                break;
            }
        }
        if (out.size() == 1 && out.get(0).length() < 3) {
            return List.of();
        }
        return out;
    }

    static List<String> tokens(String lower) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(lower);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    private static boolean any(List<String> tokens, String lower, Set<String> vocabulary) {
        for (String t : tokens) {
            if (vocabulary.contains(t)) {
                return true;
            }
        }
        for (String v : vocabulary) {
            boolean ascii = v.chars().allMatch(c -> c < 128);
            if ((v.indexOf(' ') > 0 || !ascii) && lower.contains(v)) {
                return true;
            }
        }
        return false;
    }
}
