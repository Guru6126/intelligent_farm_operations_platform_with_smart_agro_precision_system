package com.farmverse.farmverse_chatbot.agro;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FarmerSelfTest {

    private FarmerSelfTest() {
    }

    private static final String[][] CASES = {
            {"Show last month harvest of Ramesh", "en", "false", "USER_NOT_FOUND", "Registered name checked: Ramesh."},
            {"What is Ramesh's harvest history?", "en", "false", "USER_NOT_FOUND", "Ramesh"},
            {"Show the details of a farmer named Rahul Sharma.", "en", "false", "USER_NOT_FOUND",
                    "User not found. Registered name checked: Rahul Sharma."},
            {"Is Suresh Kumar registered in the system?", "en", "false", "USER_NOT_FOUND",
                    "Registered name checked: Suresh Kumar."},
            {"Is Arjun Rao registered in the system?", "en", "false", "REGISTERED", "Arjun Rao is registered"},
            {"Priya Sharma last month harvest", "en", "false", "NO_HARVESTS", "No harvest records for Priya Sharma"},
            {"Arjun Rao harvest records", "en", "false", "NO_CROPS", "no crops are recorded"},
            {"Kumaran last month harvest crops, dates and quantities", "en", "false", "RECORDS", "1,550.50 kg"},
            {"What crops did Kumaran grow?", "en", "false", "RECORDS", "Rice"},
            {"Show Geetha Rani farm and crop details", "en", "false", "RECORDS", "Cotton"},
            {"Which farmers have harvested their crops?", "en", "false", "RECORDS", "Kumaran"},
            {"Which farmers have not harvested yet?", "en", "false", "RECORDS", "Arjun Rao"},
            {"Give me the total harvest quantity from all farmers.", "en", "false", "RECORDS", "kg"},
            {"What is the total cotton harvest from all farmers last month?", "en", "false", "RECORDS", "450 kg"},
            {"Show all farmers registered in the system", "en", "false", "RECORDS", "Registered farmers"},
            {"List all farmers from MySQL database", "en", "false", "RECORDS", "Geetha Rani"},
            {"How many farmers are registered?", "en", "false", "RECORDS", "farmers are registered"},
            {"Kumaran ki gata nela harvest details cheppu", "te", "true", "RECORDS", "Motham"},
            {"Kumaran ka pichle mahine ka harvest batao", "hi", "true", "RECORDS", "Kul"},
            {"గీతా రాణి ఏ పంట సాగు చేస్తుంది", "te", "false", "RECORDS", "Cotton"},
            {"గీతా రాణి ఎంత పత్తి పండించింది?", "te", "false", "RECORDS", "450"},
            {"అర్జున్ రావు పంట వివరాలు చెప్పండి", "te", "false", "NO_CROPS", "Arjun Rao"},
            {"कुमारन ने पिछले महीने कितनी कटाई की", "hi", "false", "RECORDS", "1,550.50"},
            {"ఏ రైతులు పంట కోశారు?", "te", "false", "RECORDS", "Kumaran"},
            {"Kumar harvest history", "en", "false", "RECORDS", "Ravi Kumar"},
            {"geetha rani harvest details", "en", "false", "RECORDS", "Cotton"},
            {"geeta rani farm details", "en", "false", "RECORDS", "Rani Cotton Farm"},
            {"gita rani ela cotton harvest", "en", "false", "RECORDS", "450"},
            {"Geetha Raani crop details", "en", "false", "RECORDS", "Cotton"},
            {"kumar an last month harvest", "en", "false", "RECORDS", "1,550.50 kg"},
            {"kumarin last month harvest", "en", "false", "RECORDS", "1,550.50 kg"},
            {"arjun row crop details", "en", "false", "NO_CROPS", "Arjun Rao"},
            {"priya sarma last month harvest", "en", "false", "NO_HARVESTS", "Priya Sharma"},
            {"ravi kumaar harvest history", "en", "false", "RECORDS", "Maize"},
            {"Rahul Sharma harvest details", "en", "false", "USER_NOT_FOUND", "Rahul Sharma"},
            {"Show the details of a farmer named Suresh Kumar", "en", "false", "USER_NOT_FOUND", "Suresh Kumar"},
            {"Geetha", "en", "false", "RECORDS", "Geetha Rani"},
            {"geetha", "en", "false", "RECORDS", "Rani Cotton Farm"},
            {"Geetha harvest details", "en", "false", "RECORDS", "450 kg"},
            {"Hello", "en", "false", "NONE", ""},
            {"Rice", "en", "false", "NONE", ""},
            {"How many farms are registered?", "en", "false", "RECORDS", "5 farms are registered"},
            {"How many crops are growing?", "en", "false", "RECORDS", "crops are currently growing"},
            {"Highest harvest farmer", "en", "false", "RECORDS", "Highest harvest: Kumaran - 2,450.50 kg"},
            {"Lowest harvest farmer", "en", "false", "RECORDS", "Lowest harvest: Geetha Rani"},
            {"Number of harvest records", "en", "false", "RECORDS", "5 harvest records"},
            {"How many cuttings of cassava should a farmer plant in one hectare?", "en", "false", "NONE", ""},
            {"how many farms were there in United States in 1935", "en", "false", "NONE", ""},
            {"Arjun Rao's farm details", "en", "false", "RECORDS", "Rao Fields"},
            {"Total harvest quantity", "en", "false", "RECORDS", "Total harvest quantity from all farmers"},
            {"Kumaran total harvest", "en", "false", "RECORDS", "Kumaran"},
            {"How can I grow rice?", "en", "false", "NONE", ""},
            {"When should I harvest wheat?", "en", "false", "NONE", ""},
            {"What is crop rotation?", "en", "false", "NONE", ""},
            {"How do farmers harvest cotton without damage?", "en", "false", "NONE", ""},
            {"How to register for crop insurance?", "en", "false", "NONE", ""},
            {"Best fertilizer for good yield in cotton", "en", "false", "NONE", ""}
    };

    public static List<Map<String, Object>> run(FarmerChat chat) {
        List<Map<String, Object>> report = new ArrayList<>();
        for (String[] c : CASES) {
            Optional<FarmerChat.Result> r = chat.answer(c[0], c[1], Boolean.parseBoolean(c[2]));
            String outcome = r.map(FarmerChat.Result::outcome).orElse("NONE");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("question", c[0]);
            row.put("expected", c[3]);
            row.put("actual", outcome);
            String text = r.map(FarmerChat.Result::answer).orElse("");
            row.put("pass", outcome.equals(c[3]) && text.contains(c[4]));
            row.put("answer", r.map(FarmerChat.Result::answer).orElse("(not a farmer-records question: goes to the normal Q&A)"));
            report.add(row);
        }
        return report;
    }
}
