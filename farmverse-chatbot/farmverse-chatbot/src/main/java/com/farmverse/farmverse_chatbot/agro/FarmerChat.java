package com.farmverse.farmverse_chatbot.agro;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.farmverse.farmverse_chatbot.agro.FarmerData.Farmer;
import com.farmverse.farmverse_chatbot.agro.FarmerData.Summary;

public class FarmerChat {

    @FunctionalInterface
    public interface RepositoryProvider {
        FarmerRepository get() throws SQLException;
    }

    private final RepositoryProvider repository;
    private final Clock clock;

    public FarmerChat(RepositoryProvider repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public record Result(String answer, String outcome) {
    }

    public static class SchemaProblem extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SchemaProblem(String message) {
            super(message);
        }
    }

    private static final Map<String, String> CROP_ALIASES = new LinkedHashMap<>();

    static {
        CROP_ALIASES.put("వరి", "rice");
        CROP_ALIASES.put("ధాన్యం", "rice");
        CROP_ALIASES.put("paddy", "rice");
        CROP_ALIASES.put("పత్తి", "cotton");
        CROP_ALIASES.put("మొక్కజొన్న", "maize");
        CROP_ALIASES.put("corn", "maize");
        CROP_ALIASES.put("టమాటా", "tomato");
        CROP_ALIASES.put("టమోటా", "tomato");
        CROP_ALIASES.put("మిరప", "chilli");
        CROP_ALIASES.put("chili", "chilli");
        CROP_ALIASES.put("వేరుశనగ", "groundnut");
        CROP_ALIASES.put("చెరకు", "sugarcane");
        CROP_ALIASES.put("గోధుమ", "wheat");
        CROP_ALIASES.put("ఉల్లి", "onion");
        CROP_ALIASES.put("బంగాళాదుంప", "potato");
        CROP_ALIASES.put("चावल", "rice");
        CROP_ALIASES.put("धान", "rice");
        CROP_ALIASES.put("कपास", "cotton");
        CROP_ALIASES.put("मक्का", "maize");
        CROP_ALIASES.put("टमाटर", "tomato");
        CROP_ALIASES.put("मिर्च", "chilli");
        CROP_ALIASES.put("गेहूं", "wheat");
        CROP_ALIASES.put("प्याज", "onion");
        CROP_ALIASES.put("आलू", "potato");
    }

    public Optional<Result> answer(String question, String lang, boolean roman) {
        FarmerQuery q = FarmerQueryParser.parse(question);
        try {
            return answer(q, question, lang, roman);
        } catch (RuntimeException e) {
            e.printStackTrace();
            boolean must = q != null && (q.allFarmers() || q.explicit());
            return must
                    ? Optional.of(new Result("Could not read the farm records: " + e.getClass().getSimpleName() + ": "
                            + e.getMessage(), "ERROR"))
                    : Optional.empty();
        }
    }

    private Optional<Result> answer(FarmerQuery q, String question, String lang, boolean roman) {
        if (q == null) {
            return Optional.empty();
        }
        boolean script = Translit.isScript(question);
        boolean namePossible = !q.words().isEmpty() || !q.nameWords().isEmpty() || script;
        if (!q.allFarmers() && !namePossible) {
            return Optional.empty();
        }

        boolean mustAnswer = q.allFarmers() || q.explicit();
        FarmerRepository repo;
        try {
            repo = repository.get();
        } catch (SchemaProblem e) {
            System.err.println("[farmer-records] " + e.getMessage());
            return mustAnswer
                    ? Optional.of(new Result(FarmerReplies.schemaProblem(lang, roman), "SCHEMA_PROBLEM"))
                    : Optional.empty();
        } catch (SQLException e) {
            System.err.println("[farmer-records] database error: " + e.getMessage());
            return mustAnswer
                    ? Optional.of(new Result(FarmerReplies.databaseDown(lang, roman), "DB_DOWN"))
                    : Optional.empty();
        }

        try {
            String crop = cropFilter(repo, question);
            if (q.allFarmers()) {
                return Optional.of(everyone(repo, q, crop, lang, roman));
            }

            List<Farmer> matches = find(repo, q, question, script);
            if (matches.isEmpty()) {
                String asked = q.unknownName();
                if (asked != null && crop == null && !isCropName(repo, asked)) {
                    return Optional.of(new Result(FarmerReplies.userNotFound(asked, lang, roman), "USER_NOT_FOUND"));
                }
                return Optional.empty();
            }
            if (matches.size() > 1) {
                List<String> names = new ArrayList<>();
                matches.forEach(f -> names.add(f.name()));
                return Optional.of(new Result(FarmerReplies.multiple(names, lang, roman), "AMBIGUOUS"));
            }
            return Optional.of(records(repo, matches.get(0), q, crop, lang, roman));
        } catch (SQLException e) {
            System.err.println("[farmer-records] database error: " + e.getMessage());
            return Optional.of(new Result(FarmerReplies.databaseDown(lang, roman), "DB_DOWN"));
        }
    }

    private List<Farmer> find(FarmerRepository repo, FarmerQuery q, String question, boolean script)
            throws SQLException {
        List<Farmer> collected = new ArrayList<>();
        if (!script) {
            List<String> raw = FarmerQueryParser.tokens(question.toLowerCase(Locale.ROOT));
            List<Farmer> glued = NameMatcher.joinedExact(repo.allFarmers(), raw);
            if (glued.size() == 1) {
                return glued;
            }
        }
        if (!q.nameWords().isEmpty()) {
            for (Farmer f : repo.findByWords(q.nameWords())) {
                List<String> parts = FarmerQueryParser.tokens(f.name().toLowerCase(Locale.ROOT));
                if (parts.containsAll(q.nameWords())) {
                    collected.add(f);
                }
            }
        } else if (!q.words().isEmpty()) {
            Map<String, Integer> score = new LinkedHashMap<>();
            Map<String, Farmer> byId = new LinkedHashMap<>();
            for (Farmer f : repo.findByWords(q.words())) {
                List<String> parts = FarmerQueryParser.tokens(f.name().toLowerCase(Locale.ROOT));
                int hits = 0;
                for (String w : q.words()) {
                    if (parts.contains(w)) {
                        hits++;
                    }
                }
                if (hits > 0) {
                    score.put(f.id(), hits);
                    byId.put(f.id(), f);
                }
            }
            int best = score.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            score.forEach((id, hits) -> {
                if (hits == best) {
                    collected.add(byId.get(id));
                }
            });
        }
        List<Farmer> found = new ArrayList<>(collected);
        found = preferExact(repo, q, found);
        if (found.isEmpty() && script) {
            found.addAll(NameMatcher.find(repo.allFarmers(), question));
        }
        if (found.isEmpty() && !script) {
            List<String> spoken = new ArrayList<>(q.nameWords().isEmpty() ? q.words() : q.nameWords());
            if (q.unknownName() != null) {
                spoken.addAll(FarmerQueryParser.tokens(q.unknownName().toLowerCase(Locale.ROOT)));
            }
            found.addAll(NameMatcher.findSpoken(repo.allFarmers(), spoken));
        }
        return found;
    }

    private List<Farmer> preferExact(FarmerRepository repo, FarmerQuery q, List<Farmer> found) throws SQLException {
        if (found.size() < 2) {
            return found;
        }
        List<String> askedWords = !q.nameWords().isEmpty() ? q.nameWords() : q.words();
        if (!askedWords.isEmpty()) {
            String asked = String.join(" ", askedWords);
            List<Farmer> exact = new ArrayList<>();
            for (Farmer f : found) {
                if (String.join(" ", FarmerQueryParser.tokens(f.name().toLowerCase(Locale.ROOT))).equals(asked)) {
                    exact.add(f);
                }
            }
            if (!exact.isEmpty()) {
                found = exact;
            }
        }
        if (found.size() > 1) {
            Set<String> names = new HashSet<>();
            found.forEach(f -> names.add(f.name().trim().toLowerCase(Locale.ROOT)));
            if (names.size() == 1) {
                Farmer best = found.get(0);
                int bestScore = -1;
                for (Farmer f : found) {
                    Summary sm = repo.summary(f.id());
                    int score = sm.farms() + sm.crops() + sm.harvests();
                    if (score > bestScore) {
                        bestScore = score;
                        best = f;
                    }
                }
                return new ArrayList<>(List.of(best));
            }
        }
        return found;
    }

    private Result everyone(FarmerRepository repo, FarmerQuery q, String crop, String lang, boolean roman)
            throws SQLException {
        LocalDate[] range = range(q.period());
        LocalDate from = range[0];
        LocalDate to = range[1];
        String label = from == null ? null : FarmerReplies.periodLabel(q.period(), from, lang, roman);

        switch (q.topic()) {
            case FARMER_LIST -> {
                List<String> names = new ArrayList<>();
                repo.allFarmers().forEach(f -> names.add(f.name()));
                return new Result(FarmerReplies.farmerList(names, lang, roman), names.isEmpty() ? "NO_FARMERS" : "RECORDS");
            }
            case FARMER_COUNT -> {
                return new Result(FarmerReplies.farmerCount(repo.allFarmers().size(), lang, roman), "RECORDS");
            }
            case FARM_COUNT -> {
                return new Result(FarmerReplies.farmCount(repo.counts(), lang, roman), "RECORDS");
            }
            case CROP_COUNT -> {
                return new Result(FarmerReplies.cropCount(repo.counts(), lang, roman), "RECORDS");
            }
            case HARVEST_COUNT -> {
                return new Result(FarmerReplies.harvestCount(repo.counts(), lang, roman), "RECORDS");
            }
            case TOP_FARMER, LOW_FARMER -> {
                boolean top = q.topic() == FarmerQuery.Topic.TOP_FARMER;
                var rows = new ArrayList<>(repo.farmersWithHarvests(from, to, crop));
                rows.sort((a, b) -> top ? b.quantity().compareTo(a.quantity()) : a.quantity().compareTo(b.quantity()));
                return new Result(FarmerReplies.ranking(rows, top, repo.schema().defaultUnit(), label, lang, roman),
                        rows.isEmpty() ? "NO_HARVESTS" : "RECORDS");
            }
            case FARMERS_HARVESTED -> {
                var rows = repo.farmersWithHarvests(from, to, crop);
                return new Result(FarmerReplies.harvestedFarmers(rows, repo.schema().defaultUnit(), label, lang, roman),
                        rows.isEmpty() ? "NO_HARVESTS" : "RECORDS");
            }
            case FARMERS_NOT_HARVESTED -> {
                Set<String> done = new HashSet<>();
                repo.farmersWithHarvests(from, to, crop).forEach(r -> done.add(r.name()));
                List<String> names = new ArrayList<>();
                repo.allFarmers().forEach(f -> {
                    if (!done.contains(f.name())) {
                        names.add(f.name());
                    }
                });
                return new Result(FarmerReplies.notHarvested(names, label, lang, roman), "RECORDS");
            }
            default -> {
                var rows = repo.totals(from, to, crop);
                java.math.BigDecimal direct = crop == null ? repo.directTotal(from, to) : null;
                return new Result(FarmerReplies.totals(rows, direct, label, lang, roman),
                        rows.isEmpty() && (direct == null || direct.signum() <= 0) ? "NO_HARVESTS" : "RECORDS");
            }
        }
    }

    private LocalDate[] range(FarmerQuery.Period period) {
        LocalDate firstThis = LocalDate.now(clock).withDayOfMonth(1);
        return switch (period) {
            case LAST_MONTH -> new LocalDate[] {firstThis.minusMonths(1), firstThis};
            case THIS_MONTH -> new LocalDate[] {firstThis, firstThis.plusMonths(1)};
            default -> new LocalDate[] {null, null};
        };
    }

    private Result records(FarmerRepository repo, Farmer farmer, FarmerQuery q, String crop, String lang,
                           boolean roman) throws SQLException {
        String name = farmer.name();
        Summary s = repo.summary(farmer.id());

        if (q.topic() == FarmerQuery.Topic.REGISTRATION) {
            return new Result(FarmerReplies.registered(name, lang, roman), "REGISTERED");
        }
        if (s.farms() == 0) {
            return new Result(FarmerReplies.noFarms(name, lang, roman), "NO_FARMS");
        }

        switch (q.topic()) {
            case CROPS, FARMS -> {
                if (q.topic() == FarmerQuery.Topic.CROPS && s.crops() == 0) {
                    return new Result(FarmerReplies.noCrops(name, s.farms(), lang, roman), "NO_CROPS");
                }
                return new Result(FarmerReplies.farmsAndCrops(name, repo.farmsAndCrops(farmer.id()), lang, roman),
                        "RECORDS");
            }
            case OVERVIEW -> {
                if (s.crops() == 0) {
                    return new Result(FarmerReplies.noCrops(name, s.farms(), lang, roman), "NO_CROPS");
                }
                return new Result(FarmerReplies.farmsAndCrops(name, repo.farmsAndCrops(farmer.id()), lang, roman)
                        + "\n\n" + FarmerReplies.harvests(name, repo.harvests(farmer.id(), null, null, null), null,
                        lang, roman), "RECORDS");
            }
            default -> {
                if (s.crops() == 0) {
                    return new Result(FarmerReplies.noCrops(name, s.farms(), lang, roman), "NO_CROPS");
                }
                LocalDate[] range = range(q.period());
                String label = range[0] == null ? null : FarmerReplies.periodLabel(q.period(), range[0], lang, roman);
                var rows = repo.harvests(farmer.id(), range[0], range[1], crop);
                return new Result(FarmerReplies.harvests(name, rows, label, lang, roman),
                        rows.isEmpty() ? "NO_HARVESTS" : "RECORDS");
            }
        }
    }

    private String cropFilter(FarmerRepository repo, String question) throws SQLException {
        String lower = question.toLowerCase(Locale.ROOT);
        List<String> crops = repo.cropNames();
        for (String c : crops) {
            if (Pattern.compile("\\b" + Pattern.quote(c.toLowerCase(Locale.ROOT)) + "(?:s|es)?\\b").matcher(lower).find()) {
                return c;
            }
        }
        for (Map.Entry<String, String> a : CROP_ALIASES.entrySet()) {
            if (lower.contains(a.getKey())) {
                for (String c : crops) {
                    if (c.equalsIgnoreCase(a.getValue())) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    private boolean isCropName(FarmerRepository repo, String word) throws SQLException {
        for (String c : repo.cropNames()) {
            if (c.equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }
}
