package com.farmverse.farmverse_chatbot.agro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.farmverse.farmverse_chatbot.agro.FarmerData.FarmCrop;
import com.farmverse.farmverse_chatbot.agro.FarmerData.Harvest;

public final class FarmerReplies {

    private FarmerReplies() {
    }

    private static int v(String lang, boolean roman) {
        return switch (lang == null ? "en" : lang) {
            case "te" -> roman ? 3 : 1;
            case "hi" -> roman ? 4 : 2;
            default -> 0;
        };
    }

    private static String[] t(String en, String te, String hi, String teRoman, String hiRoman) {
        return new String[] {en, te, hi, teRoman, hiRoman};
    }

    private static final String[] NOT_FOUND = t("User not found.", "వినియోగదారు కనుగొనబడలేదు.", "उपयोगकर्ता नहीं मिला।",
            "User dorakaledu.", "User nahi mila.");
    private static final String[] NO_FARMS = t("{n} has no farms registered yet.", "{n} కి ఇంకా పొలాలు నమోదు కాలేదు.",
            "{n} का कोई खेत अभी दर्ज नहीं है।", "{n} ki inka polalu register avvaledu.", "{n} ka koi khet abhi darj nahi hai.");
    private static final String[] NO_CROPS = t("{n} has {k} farm(s), but no crops are recorded yet.",
            "{n} కి {k} పొలం(లు) ఉన్నాయి, కానీ ఇంకా పంటలు నమోదు కాలేదు.",
            "{n} के {k} खेत हैं, लेकिन अभी कोई फसल दर्ज नहीं है।",
            "{n} ki {k} polam(lu) unnayi, kani inka pantalu register avvaledu.",
            "{n} ke {k} khet hain, lekin abhi koi fasal darj nahi hai.");
    private static final String[] NO_HARVEST_PERIOD = t("No harvest records for {n} in {p}.",
            "{p} లో {n} కి కోత రికార్డులు లేవు.", "{p} में {n} का कोई कटाई रिकॉर्ड नहीं है।",
            "{p} lo {n} ki harvest records levu.", "{p} mein {n} ka koi harvest record nahi hai.");
    private static final String[] NO_HARVEST_ALL = t("{n} has no harvest records yet.",
            "{n} కి ఇంకా కోత రికార్డులు లేవు.", "{n} का अभी कोई कटाई रिकॉर्ड नहीं है।",
            "{n} ki inka harvest records levu.", "{n} ka abhi koi harvest record nahi hai.");
    private static final String[] HARVEST_PERIOD = t("{n} - harvests in {p}:", "{n} - {p} లో కోతలు:",
            "{n} - {p} में कटाई:", "{n} - {p} lo harvest:", "{n} - {p} mein harvest:");
    private static final String[] HARVEST_ALL = t("{n} - harvest history:", "{n} - కోత చరిత్ర:",
            "{n} - कटाई का इतिहास:", "{n} - harvest history:", "{n} - harvest history:");
    private static final String[] TOTAL = t("Total", "మొత్తం", "कुल", "Motham", "Kul");
    private static final String[] CROPS_HEADER = t("{n} - farms and crops:", "{n} - పొలాలు మరియు పంటలు:",
            "{n} - खेत और फसलें:", "{n} - polalu mariyu pantalu:", "{n} - khet aur fasalen:");
    private static final String[] FARM_WORD = t("Farm", "పొలం", "खेत", "Polam", "Khet");
    private static final String[] NO_CROP_ON_FARM = t("no crops yet", "ఇంకా పంటలు లేవు", "अभी कोई फसल नहीं",
            "inka pantalu levu", "abhi koi fasal nahi");
    private static final String[] LAST_MONTH = t("last month ({m})", "గత నెల ({m})", "पिछला महीना ({m})",
            "gata nela ({m})", "pichhle mahine ({m})");
    private static final String[] THIS_MONTH = t("this month ({m})", "ఈ నెల ({m})", "इस महीने ({m})",
            "ee nela ({m})", "is mahine ({m})");
    private static final String[] MULTIPLE = t("More than one farmer matches: {l}. Please give the full name.",
            "ఒకటి కంటే ఎక్కువ రైతులు సరిపోతున్నారు: {l}. దయచేసి పూర్తి పేరు చెప్పండి.",
            "एक से अधिक किसान मिले: {l}। कृपया पूरा नाम बताएं।",
            "Okati kante ekkuva raithulu match avtunnaru: {l}. Dayachesi full name cheppandi.",
            "Ek se zyada kisan mile: {l}. Kripya poora naam batayein.");
    private static final String[] DB_DOWN = t(
            "The farmer records database (agroprecision) is not reachable right now. Please check that MySQL is running and the agro.datasource settings in application.properties.",
            "రైతు రికార్డుల డేటాబేస్ (agroprecision) ఇప్పుడు అందుబాటులో లేదు. MySQL నడుస్తోందో, application.properties లోని agro.datasource సెట్టింగ్‌లు సరైనవో చూడండి.",
            "किसान रिकॉर्ड डेटाबेस (agroprecision) अभी उपलब्ध नहीं है। कृपया जांचें कि MySQL चल रहा है और application.properties में agro.datasource सेटिंग सही हैं।",
            "Raithu records database (agroprecision) ippudu andubatulo ledu. MySQL run avtunda, application.properties lo agro.datasource settings sariga unnaya check cheyandi.",
            "Kisan records database (agroprecision) abhi uplabdh nahi hai. Kripya check karein ki MySQL chal raha hai aur application.properties mein agro.datasource settings sahi hain.");
    private static final String[] SCHEMA_PROBLEM = t(
            "The farmer records tables could not be read. Open /api/chatbot/db-check for the exact problem.",
            "రైతు రికార్డుల టేబుల్స్ చదవలేకపోయాను. వివరాల కోసం /api/chatbot/db-check చూడండి.",
            "किसान रिकॉर्ड टेबल पढ़े नहीं जा सके। विवरण के लिए /api/chatbot/db-check देखें।",
            "Raithu records tables chadavalekapoyanu. Vivaralu kosam /api/chatbot/db-check chudandi.",
            "Kisan records tables padhe nahi ja sake. Vivaran ke liye /api/chatbot/db-check dekhein.");

    private static final String[][] MONTHS = {
            {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
                    "November", "December"},
            {"జనవరి", "ఫిబ్రవరి", "మార్చి", "ఏప్రిల్", "మే", "జూన్", "జూలై", "ఆగస్టు", "సెప్టెంబర్", "అక్టోబర్",
                    "నవంబర్", "డిసెంబర్"},
            {"जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर",
                    "दिसंबर"}
    };

    public static String userNotFound(String lang, boolean roman) {
        return NOT_FOUND[v(lang, roman)];
    }

    private static final String[] NOT_FOUND_NAMED = t(
            "User not found. Registered name checked: {n}.",
            "వినియోగదారు కనుగొనబడలేదు. తనిఖీ చేసిన పేరు: {n}.",
            "उपयोगकर्ता नहीं मिला। जांचा गया नाम: {n}।",
            "User dorakaledu. Check chesina peru: {n}.",
            "User nahi mila. Jaancha gaya naam: {n}.");
    private static final String[] REGISTERED = t("{n} is registered in the system.",
            "{n} వ్యవస్థలో నమోదై ఉన్నారు.", "{n} सिस्टम में पंजीकृत हैं।",
            "{n} system lo register ayyaru.", "{n} system mein panjikrit hain.");
    private static final String[] LIST_HEADER = t("Registered farmers ({k}):", "నమోదైన రైతులు ({k}):",
            "पंजीकृत किसान ({k}):", "Register ayina raithulu ({k}):", "Panjikrit kisan ({k}):");
    private static final String[] LIST_EMPTY = t("No farmers are registered yet.", "ఇంకా రైతులు నమోదు కాలేదు.",
            "अभी कोई किसान पंजीकृत नहीं है।", "Inka raithulu register avvaledu.", "Abhi koi kisan panjikrit nahi hai.");
    private static final String[] COUNT = t("{k} farmers are registered in the system.", "{k} మంది రైతులు నమోదై ఉన్నారు.",
            "{k} किसान पंजीकृत हैं।", "{k} mandi raithulu register ayyaru.", "{k} kisan panjikrit hain.");
    private static final String[] HARVESTED_HEADER = t("Farmers who have harvested{p} ({k}):",
            "కోత కోసిన రైతులు{p} ({k}):", "कटाई कर चुके किसान{p} ({k}):",
            "Harvest chesina raithulu{p} ({k}):", "Harvest kar chuke kisan{p} ({k}):");
    private static final String[] HARVESTED_EMPTY = t("No farmer has harvest records{p}.",
            "{p} ఏ రైతుకూ కోత రికార్డులు లేవు.", "{p} किसी किसान का कटाई रिकॉर्ड नहीं है।",
            "{p} e raithuki harvest records levu.", "{p} kisi kisan ka harvest record nahi hai.");
    private static final String[] HARVEST_WORD = t("harvest(s)", "కోతలు", "कटाई", "harvests", "harvests");
    private static final String[] NOT_HARVESTED_HEADER = t("Farmers with no harvest records{p} ({k}):",
            "కోత రికార్డులు లేని రైతులు{p} ({k}):", "बिना कटाई रिकॉर्ड वाले किसान{p} ({k}):",
            "Harvest records leni raithulu{p} ({k}):", "Bina harvest record wale kisan{p} ({k}):");
    private static final String[] NOT_HARVESTED_EMPTY = t("Every registered farmer has harvest records{p}.",
            "నమోదైన ప్రతి రైతుకు కోత రికార్డులు ఉన్నాయి{p}.", "हर पंजीकृत किसान का कटाई रिकॉर्ड है{p}।",
            "Prathi raithuki harvest records unnayi{p}.", "Har panjikrit kisan ka harvest record hai{p}.");
    private static final String[] TOTAL_HEADER = t("Total harvest quantity from all farmers{p}: {t}",
            "అన్ని రైతుల మొత్తం కోత పరిమాణం{p}: {t}", "सभी किसानों की कुल कटाई मात्रा{p}: {t}",
            "Andaru raithula motham harvest quantity{p}: {t}", "Sabhi kisanon ki kul harvest matra{p}: {t}");
    private static final String[] TOTAL_EMPTY = t("No harvest records found{p}.", "కోత రికార్డులు లేవు{p}.",
            "कोई कटाई रिकॉर्ड नहीं मिला{p}।", "Harvest records levu{p}.", "Koi harvest record nahi mila{p}.");
    private static final String[] OTHER = t("not linked to a farm or crop", "పొలం/పంటకు లింక్ కానివి",
            "खेत/फसल से जुड़े नहीं", "polam/panta ki link kanivi", "khet/fasal se jude nahi");
    private static final String[] REVENUE = t("revenue", "ఆదాయం", "आय", "aadayam", "aay");

    private static final String[] FARM_COUNT = t("{k} farms are registered in the system.",
            "వ్యవస్థలో {k} పొలాలు నమోదై ఉన్నాయి.", "सिस्टम में {k} खेत पंजीकृत हैं।",
            "System lo {k} polalu register ayyayi.", "System mein {k} khet panjikrit hain.");
    private static final String[] CROP_COUNT = t("{g} crops are currently growing (total crops recorded: {k}).",
            "ప్రస్తుతం {g} పంటలు పెరుగుతున్నాయి (మొత్తం నమోదైన పంటలు: {k}).",
            "अभी {g} फसलें उग रही हैं (कुल दर्ज फसलें: {k})।",
            "Ippudu {g} pantalu perugutunnayi (motham namodaina pantalu: {k}).",
            "Abhi {g} fasalen ug rahi hain (kul darj fasalen: {k}).");
    private static final String[] CROP_COUNT_ALL = t("{k} crops are recorded in the system.",
            "వ్యవస్థలో {k} పంటలు నమోదై ఉన్నాయి.", "सिस्टम में {k} फसलें दर्ज हैं।",
            "System lo {k} pantalu namodai unnayi.", "System mein {k} fasalen darj hain.");
    private static final String[] HARVEST_COUNT = t("{k} harvest records are stored in the system.",
            "వ్యవస్థలో {k} కోత రికార్డులు ఉన్నాయి.", "सिस्टम में {k} कटाई रिकॉर्ड हैं।",
            "System lo {k} harvest records unnayi.", "System mein {k} harvest record hain.");
    private static final String[] TOP_HEADER = t("Highest harvest: {n} - {q}", "అత్యధిక కోత: {n} - {q}",
            "सबसे ज़्यादा कटाई: {n} - {q}", "Athyadhika harvest: {n} - {q}", "Sabse zyada harvest: {n} - {q}");
    private static final String[] LOW_HEADER = t("Lowest harvest: {n} - {q}", "అత్యల్ప కోత: {n} - {q}",
            "सबसे कम कटाई: {n} - {q}", "Athyalpa harvest: {n} - {q}", "Sabse kam harvest: {n} - {q}");

    public static String farmCount(FarmerData.Counts c, String lang, boolean roman) {
        return FARM_COUNT[v(lang, roman)].replace("{k}", String.valueOf(c.farms()));
    }

    public static String cropCount(FarmerData.Counts c, String lang, boolean roman) {
        int i = v(lang, roman);
        return c.hasStage()
                ? CROP_COUNT[i].replace("{g}", String.valueOf(c.growing())).replace("{k}", String.valueOf(c.crops()))
                : CROP_COUNT_ALL[i].replace("{k}", String.valueOf(c.crops()));
    }

    public static String harvestCount(FarmerData.Counts c, String lang, boolean roman) {
        return HARVEST_COUNT[v(lang, roman)].replace("{k}", String.valueOf(c.harvests()));
    }

    public static String ranking(List<FarmerData.FarmerTotal> sorted, boolean top, String unit, String periodLabel,
                                 String lang, boolean roman) {
        int i = v(lang, roman);
        if (sorted.isEmpty()) {
            return TOTAL_EMPTY[i].replace("{p}", suffix(periodLabel));
        }
        FarmerData.FarmerTotal first = sorted.get(0);
        String u = unit == null ? "" : " " + unit;
        StringBuilder sb = new StringBuilder((top ? TOP_HEADER : LOW_HEADER)[i].replace("{n}", first.name())
                .replace("{q}", FarmerRepository.formatNumber(first.quantity()) + u + suffix(periodLabel)));
        int n = 1;
        for (FarmerData.FarmerTotal r : sorted) {
            sb.append('\n').append(n++).append(". ").append(r.name()).append(" - ")
                    .append(FarmerRepository.formatNumber(r.quantity())).append(u);
        }
        return sb.toString();
    }

    public static String userNotFound(String name, String lang, boolean roman) {
        return name == null || name.isBlank() ? NOT_FOUND[v(lang, roman)]
                : NOT_FOUND_NAMED[v(lang, roman)].replace("{n}", name);
    }

    public static String registered(String name, String lang, boolean roman) {
        return REGISTERED[v(lang, roman)].replace("{n}", name);
    }

    public static String farmerList(List<String> names, String lang, boolean roman) {
        int i = v(lang, roman);
        if (names.isEmpty()) {
            return LIST_EMPTY[i];
        }
        StringBuilder sb = new StringBuilder(LIST_HEADER[i].replace("{k}", String.valueOf(names.size())));
        int n = 1;
        for (String name : names) {
            sb.append('\n').append(n++).append(". ").append(name);
        }
        return sb.toString();
    }

    public static String farmerCount(int count, String lang, boolean roman) {
        return COUNT[v(lang, roman)].replace("{k}", String.valueOf(count));
    }

    private static String suffix(String periodLabel) {
        return periodLabel == null ? "" : " - " + periodLabel;
    }

    public static String harvestedFarmers(List<FarmerData.FarmerTotal> rows, String unit, String periodLabel, String lang,
                                          boolean roman) {
        int i = v(lang, roman);
        if (rows.isEmpty()) {
            return HARVESTED_EMPTY[i].replace("{p}", periodLabel == null ? "" : periodLabel).trim();
        }
        StringBuilder sb = new StringBuilder(HARVESTED_HEADER[i].replace("{p}", suffix(periodLabel))
                .replace("{k}", String.valueOf(rows.size())));
        int n = 1;
        for (FarmerData.FarmerTotal r : rows) {
            sb.append('\n').append(n++).append(". ").append(r.name()).append(" - ").append(r.harvests()).append(' ')
                    .append(HARVEST_WORD[i]).append(" - ").append(FarmerRepository.formatNumber(r.quantity()))
                    .append(unit == null ? "" : " " + unit);
        }
        return sb.toString();
    }

    public static String notHarvested(List<String> names, String periodLabel, String lang, boolean roman) {
        int i = v(lang, roman);
        if (names.isEmpty()) {
            return NOT_HARVESTED_EMPTY[i].replace("{p}", suffix(periodLabel));
        }
        StringBuilder sb = new StringBuilder(NOT_HARVESTED_HEADER[i].replace("{p}", suffix(periodLabel))
                .replace("{k}", String.valueOf(names.size())));
        int n = 1;
        for (String name : names) {
            sb.append('\n').append(n++).append(". ").append(name);
        }
        return sb.toString();
    }

    public static String totals(List<FarmerData.CropTotal> rows, java.math.BigDecimal direct, String periodLabel, String lang,
                                boolean roman) {
        int i = v(lang, roman);
        java.math.BigDecimal linked = java.math.BigDecimal.ZERO;
        for (FarmerData.CropTotal r : rows) {
            if (r.quantity() != null) {
                linked = linked.add(r.quantity());
            }
        }
        java.math.BigDecimal unlinked = direct == null ? java.math.BigDecimal.ZERO : direct.subtract(linked);
        if (rows.isEmpty() && unlinked.signum() <= 0) {
            return TOTAL_EMPTY[i].replace("{p}", suffix(periodLabel));
        }
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        java.util.Set<String> units = new java.util.LinkedHashSet<>();
        for (FarmerData.CropTotal r : rows) {
            if (r.quantity() != null) {
                sum = sum.add(r.quantity());
            }
            units.add(r.unit() == null ? "" : r.unit());
        }
        if (unlinked.signum() > 0) {
            sum = sum.add(unlinked);
        }
        String unit = units.size() == 1 ? units.iterator().next() : null;
        StringBuilder sb = new StringBuilder(TOTAL_HEADER[i].replace("{p}", suffix(periodLabel)).replace("{t}",
                unit == null ? FarmerRepository.formatNumber(sum) + " (mixed units)"
                        : FarmerRepository.formatNumber(sum) + (unit.isEmpty() ? "" : " " + unit)));
        for (FarmerData.CropTotal r : rows) {
            sb.append("\n   - ").append(r.crop()).append(": ").append(FarmerRepository.formatNumber(r.quantity()))
                    .append(r.unit() == null || r.unit().isEmpty() ? "" : " " + r.unit());
        }
        if (unlinked.signum() > 0) {
            sb.append("\n   - ").append(OTHER[i]).append(": ").append(FarmerRepository.formatNumber(unlinked))
                    .append(unit == null || unit.isEmpty() ? "" : " " + unit);
        }
        return sb.toString();
    }

    public static String databaseDown(String lang, boolean roman) {
        return DB_DOWN[v(lang, roman)];
    }

    public static String schemaProblem(String lang, boolean roman) {
        return SCHEMA_PROBLEM[v(lang, roman)];
    }

    public static String multiple(List<String> names, String lang, boolean roman) {
        return MULTIPLE[v(lang, roman)].replace("{l}", String.join(", ", names));
    }

    public static String noFarms(String name, String lang, boolean roman) {
        return NO_FARMS[v(lang, roman)].replace("{n}", name);
    }

    public static String noCrops(String name, int farms, String lang, boolean roman) {
        return NO_CROPS[v(lang, roman)].replace("{n}", name).replace("{k}", String.valueOf(farms));
    }

    public static String periodLabel(FarmerQuery.Period period, LocalDate month, String lang, boolean roman) {
        int i = v(lang, roman);
        String m = MONTHS[i == 1 ? 1 : i == 2 ? 2 : 0][month.getMonthValue() - 1] + " " + month.getYear();
        return (period == FarmerQuery.Period.LAST_MONTH ? LAST_MONTH : THIS_MONTH)[i].replace("{m}", m);
    }

    public static String harvests(String name, List<Harvest> rows, String periodLabel, String lang, boolean roman) {
        int i = v(lang, roman);
        if (rows.isEmpty()) {
            return periodLabel == null
                    ? NO_HARVEST_ALL[i].replace("{n}", name)
                    : NO_HARVEST_PERIOD[i].replace("{n}", name).replace("{p}", periodLabel);
        }
        StringBuilder sb = new StringBuilder(periodLabel == null
                ? HARVEST_ALL[i].replace("{n}", name)
                : HARVEST_PERIOD[i].replace("{n}", name).replace("{p}", periodLabel));
        BigDecimal total = BigDecimal.ZERO;
        boolean totalOk = true;
        String unit = null;
        int n = 1;
        for (Harvest h : rows) {
            sb.append('\n').append(n++).append(". ").append(nz(h.cropName()));
            if (h.farmName() != null) {
                sb.append(" (").append(h.farmName()).append(')');
            }
            sb.append(" - ").append(h.date() == null ? "-" : h.date().toString());
            sb.append(" - ").append(h.quantity() == null ? "-" : h.quantity());
            if (h.unit() != null) {
                sb.append(' ').append(h.unit());
            }
            if (h.revenue() != null) {
                sb.append(" - ").append(REVENUE[i]).append(" ₹").append(h.revenue());
            }
            try {
                total = total.add(new BigDecimal(h.quantity().replace(",", "")));
                if (unit != null && !unit.equalsIgnoreCase(String.valueOf(h.unit()))) {
                    totalOk = false;
                }
                unit = h.unit();
            } catch (RuntimeException e) {
                totalOk = false;
            }
        }
        if (totalOk && rows.size() > 1) {
            BigDecimal t = total.stripTrailingZeros();
            if (t.scale() < 0) {
                t = t.setScale(0);
            }
            sb.append('\n').append(TOTAL[i]).append(": ")
                    .append(String.format(Locale.US, t.scale() == 0 ? "%,d" : "%,.2f",
                            t.scale() == 0 ? (Object) t.toBigInteger() : (Object) t))
                    .append(unit == null ? "" : " " + unit);
        }
        return sb.toString();
    }

    public static String farmsAndCrops(String name, List<FarmCrop> rows, String lang, boolean roman) {
        int i = v(lang, roman);
        StringBuilder sb = new StringBuilder(CROPS_HEADER[i].replace("{n}", name));
        String lastFarm = null;
        for (FarmCrop r : rows) {
            String farmKey = r.farmName() + "|" + r.location();
            if (!farmKey.equals(lastFarm)) {
                lastFarm = farmKey;
                sb.append('\n').append(FARM_WORD[i]).append(": ").append(nz(r.farmName()));
                List<String> extra = new java.util.ArrayList<>();
                if (r.location() != null) {
                    extra.add(r.location());
                }
                if (r.area() != null) {
                    extra.add(r.area());
                }
                if (r.soil() != null) {
                    extra.add(r.soil());
                }
                if (!extra.isEmpty()) {
                    sb.append(" (").append(String.join(", ", extra)).append(')');
                }
            }
            if (r.cropName() == null) {
                sb.append("\n   - ").append(NO_CROP_ON_FARM[i]);
            } else {
                List<String> bits = new java.util.ArrayList<>();
                if (r.variety() != null) {
                    bits.add(r.variety());
                }
                if (r.season() != null) {
                    bits.add(r.season());
                }
                if (r.stage() != null) {
                    bits.add(r.stage());
                }
                sb.append("\n   - ").append(r.cropName());
                if (!bits.isEmpty()) {
                    sb.append(" (").append(String.join(", ", bits)).append(')');
                }
            }
        }
        return sb.toString();
    }

    private static String nz(String s) {
        return s == null ? "-" : s;
    }
}
