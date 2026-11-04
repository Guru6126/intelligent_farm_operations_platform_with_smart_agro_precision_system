package com.farmverse.farmverse_chatbot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class AgricultureDataService {

    private record Entry(String question, String answer,
                         Set<String> questionWords, Set<String> allWords,
                         Set<String> crops, Set<String> questionCrops) {
    }

    public record Match(String question, String answer,
                        double score, String mode) {
    }

    private static final int EMBED_BATCH_SIZE = 32;
    private static final int EMBED_MAX_CHARS = 3000;

    private static final Set<String> GENERIC_WORDS = Set.of(
            "farmer", "farmers", "farming", "farm", "farms", "crop", "crops", "plant", "plants",
            "agriculture", "agricultural", "grow", "growing", "grown", "year", "years", "time",
            "use", "used", "using", "good", "important", "make", "two", "one", "different",
            "field", "fields", "land", "type", "types", "kind", "kinds"
    );

    private static final double CONFIDENT_SEMANTIC = 0.80;

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were",
            "to", "of", "in", "on", "for", "and", "or",
            "by", "with", "from", "it", "this", "that",
            "you", "your", "can", "could", "should",
            "how", "what", "why", "when", "where",
            "do", "does", "did", "be", "as", "into",
            "their", "they", "them", "we", "our",
            "i", "me", "my", "will", "would", "has",
            "have", "had", "about", "please", "help",
            "want", "need", "give", "tell", "explain",
            "way", "ways", "best", "some", "much"
    );

    private static final Map<String, String> CROPS = Map.ofEntries(
            Map.entry("rice", "rice"), Map.entry("paddy", "rice"),
            Map.entry("maize", "maize"), Map.entry("corn", "maize"),
            Map.entry("wheat", "wheat"), Map.entry("barley", "barley"),
            Map.entry("cotton", "cotton"), Map.entry("sugarcane", "sugarcane"),
            Map.entry("potato", "potato"), Map.entry("potatoes", "potato"),
            Map.entry("tomato", "tomato"), Map.entry("tomatoes", "tomato"),
            Map.entry("onion", "onion"), Map.entry("onions", "onion"),
            Map.entry("banana", "banana"), Map.entry("bananas", "banana"),
            Map.entry("mango", "mango"), Map.entry("mangoes", "mango"),
            Map.entry("groundnut", "groundnut"), Map.entry("peanut", "groundnut"),
            Map.entry("soybean", "soybean"), Map.entry("soyabean", "soybean"),
            Map.entry("chilli", "chilli"), Map.entry("chili", "chilli"),
            Map.entry("millet", "millet"), Map.entry("sorghum", "sorghum"),
            Map.entry("coffee", "coffee"), Map.entry("tea", "tea"),
            Map.entry("coconut", "coconut"), Map.entry("cassava", "cassava"),
            Map.entry("sunflower", "sunflower"), Map.entry("mustard", "mustard"),
            Map.entry("turmeric", "turmeric"), Map.entry("ginger", "ginger"),
            Map.entry("apple", "apple"), Map.entry("grapes", "grape"),
            Map.entry("grape", "grape"), Map.entry("jute", "jute"),
            Map.entry("lentil", "lentil"), Map.entry("lentils", "lentil")
    );

    private final OllamaService ollamaService;
    private final JdbcTemplate jdbcTemplate;

    private final List<Entry> entries = new ArrayList<>();

    private volatile double[][] vectors = null;

    private final double threshold;
    private final double translatedThreshold;
    private final double ragMinScore;

    public AgricultureDataService(
            OllamaService ollamaService,
            JdbcTemplate jdbcTemplate,
            @Value("${farmverse.match-threshold:0.65}") double threshold,
            @Value("${farmverse.translated-match-threshold:0.72}") double translatedThreshold,
            @Value("${farmverse.rag-min-score:0.45}") double ragMinScore) {
        this.ollamaService = ollamaService;
        this.jdbcTemplate = jdbcTemplate;
        this.ragMinScore = ragMinScore;
        this.threshold = threshold;
        this.translatedThreshold = translatedThreshold;
    }

    @PostConstruct
    public void loadData() {
        loadDatabase();
        System.out.println("Agriculture Q&A loaded from MySQL (farmverse_db.agriculture_qa): " + entries.size());

        Thread thread = new Thread(this::buildEmbeddings, "embedding-builder");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadDatabase() {
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList("SELECT question, answer FROM agriculture_qa ORDER BY id");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read the MySQL table agriculture_qa (database farmverse_db). Check that MySQL is "
                            + "running, spring.datasource.* in application.properties is correct, and that "
                            + "database/farmverse_setup.sql was run.", e);
        }

        for (Map<String, Object> row : rows) {
            String question = row.get("question") == null ? "" : row.get("question").toString().replace("\uFEFF", "").trim();
            String answer = row.get("answer") == null ? "" : row.get("answer").toString().trim();
            if (question.isBlank() || answer.isBlank()) {
                continue;
            }
            entries.add(new Entry(question, answer,
                    getContentWords(question), getContentWords(question + " " + answer),
                    findCrops(question + " " + answer), findCrops(question)));
        }

        if (entries.isEmpty()) {
            throw new IllegalStateException("The agriculture_qa table is empty. Run database/farmverse_setup.sql "
                    + "in MySQL (Workbench or the mysql command line) to create and fill it.");
        }
    }

    private void buildEmbeddings() {
        try {
            double[][] result = new double[entries.size()][];

            for (int start = 0; start < entries.size(); start += EMBED_BATCH_SIZE) {
                int end = Math.min(start + EMBED_BATCH_SIZE, entries.size());

                List<String> texts = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    Entry e = entries.get(i);
                    texts.add(truncate(e.question() + " " + e.answer()));
                }

                List<double[]> batch = ollamaService.embedBatch(texts);
                for (int i = 0; i < batch.size(); i++) {
                    result[start + i] = normalize(batch.get(i));
                }

                System.out.println("Embeddings generated: " + end
                        + "/" + entries.size());
            }

            vectors = result;
            System.out.println("Agriculture semantic search is ready.");

        } catch (Exception e) {
            System.err.println("Semantic search unavailable ("
                    + e.getMessage() + "). Using keyword search only. "
                    + "Start Ollama and pull 'nomic-embed-text', then restart.");
        }
    }

    public int recordCount() {
        return entries.size();
    }

    public boolean isSemanticReady() {
        return vectors != null;
    }

    public Optional<Match> findAnswer(String userQuestion) {
        return findAnswer(userQuestion, false);
    }

    public Optional<Match> findAnswer(String userQuestion, boolean translated) {
        double limit = translated ? translatedThreshold : threshold;
        List<Match> ranked = rank(userQuestion);
        Match best = ranked.isEmpty() ? null : ranked.get(0);

        System.out.printf(Locale.ROOT, "Q: \"%s\" | mode=%s | best=%.3f (need %.2f) | matched: %s%n",
                userQuestion, vectors != null ? "semantic" : "keyword", best == null ? -1.0 : best.score(), limit,
                best == null ? "NONE" : best.question());

        return best == null || best.score() < limit ? Optional.empty() : Optional.of(best);
    }

    public List<Match> relatedEntries(String userQuestion, int limit) {
        List<Match> out = new ArrayList<>();
        for (Match m : rank(userQuestion)) {

            double floor = "keyword".equals(m.mode()) ? Math.min(ragMinScore, 0.15) : ragMinScore;
            if (m.score() >= floor && out.size() < limit) {
                out.add(m);
            }
        }
        return out;
    }

    private List<Match> rank(String userQuestion) {
        List<Match> out = new ArrayList<>();
        if (userQuestion == null || userQuestion.isBlank()) {
            return out;
        }

        Set<String> queryWords = getContentWords(userQuestion);
        Set<String> queryCrops = findCrops(userQuestion);

        double[][] vecs = vectors;
        double[] queryVector = null;
        if (vecs != null) {
            try {
                queryVector = normalize(ollamaService.embedBatch(List.of(truncate(userQuestion))).get(0));
            } catch (Exception e) {
                queryVector = null;
            }
        }
        boolean semantic = queryVector != null;

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);

            if (sameQuestion(userQuestion, entry.question())) {
                out.add(new Match(entry.question(), entry.answer(), 1.0, "exact"));
                continue;
            }

            double semanticOnly = semantic ? dot(queryVector, vecs[i]) : 0.0;
            boolean confident = semantic && semanticOnly >= CONFIDENT_SEMANTIC;

            if (!queryCrops.isEmpty() && Collections.disjoint(queryCrops, entry.questionCrops())) {
                boolean generalEntry = entry.questionCrops().isEmpty()
                        && !Collections.disjoint(queryCrops, entry.crops());
                if (!(generalEntry && confident)) {
                    continue;
                }
            }

            if (countSpecific(queryWords, entry.questionWords()) == 0 && !confident) {
                continue;
            }

            int commonAll = countCommon(queryWords, entry.allWords());
            int commonQuestion = countCommon(queryWords, entry.questionWords());
            double denom = Math.max(1, queryWords.size());
            double lexical = (0.7 * (commonQuestion / denom)) + (0.3 * (commonAll / denom));

            double score;
            if (semantic) {
                score = (0.80 * semanticOnly) + (0.20 * lexical);
            } else {

                boolean enough = queryWords.size() <= 1 ? commonAll >= 1 : commonAll >= 2;
                score = enough ? lexical : 0.0;
            }
            if (score > 0.0) {
                out.add(new Match(entry.question(), entry.answer(), score, semantic ? "semantic" : "keyword"));
            }
        }

        out.sort((x, y) -> Double.compare(y.score(), x.score()));
        return out;
    }

    private static boolean sameQuestion(String a, String b) {
        return normalizeQuestion(a).equals(normalizeQuestion(b));
    }

    private static String normalizeQuestion(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static int countSpecific(Set<String> query, Set<String> record) {
        int count = 0;
        for (String word : query) {
            if (!GENERIC_WORDS.contains(word) && record.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private static int countCommon(Set<String> query, Set<String> record) {
        int count = 0;
        for (String word : query) {
            if (record.contains(word)) {
                count++;
            }
        }
        return count;
    }

    private static Set<String> getContentWords(String text) {
        Set<String> words = new HashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group();
            if (!STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    private static Set<String> findCrops(String text) {
        Set<String> crops = new HashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String crop = CROPS.get(matcher.group());
            if (crop != null) {
                crops.add(crop);
            }
        }
        return crops;
    }

    private static String truncate(String text) {
        return text.length() <= EMBED_MAX_CHARS
                ? text : text.substring(0, EMBED_MAX_CHARS);
    }

    private static double[] normalize(double[] v) {
        double sum = 0.0;
        for (double x : v) {
            sum += x * x;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return v;
        }
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            out[i] = v[i] / norm;
        }
        return out;
    }

    private static double dot(double[] a, double[] b) {
        if (a.length != b.length) {
            return -1.0;
        }
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}
