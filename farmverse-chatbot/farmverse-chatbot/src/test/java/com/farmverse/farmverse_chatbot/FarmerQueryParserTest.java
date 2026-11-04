package com.farmverse.farmverse_chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.farmverse.farmverse_chatbot.agro.FarmerQuery;
import com.farmverse.farmverse_chatbot.agro.FarmerQueryParser;
import com.farmverse.farmverse_chatbot.agro.FarmerReplies;

class FarmerQueryParserTest {

    @Test
    void lastMonthHarvestOfAFarmer() {
        FarmerQuery q = FarmerQueryParser.parse("Show last month harvest of Kumaran");
        assertNotNull(q);
        assertEquals(FarmerQuery.Topic.HARVEST, q.topic());
        assertEquals(FarmerQuery.Period.LAST_MONTH, q.period());
        assertTrue(q.words().contains("kumaran"));
    }

    @Test
    void unknownCapitalisedNameIsFlagged() {
        FarmerQuery q = FarmerQueryParser.parse("Show last month harvest of Ramesh");
        assertNotNull(q);
        assertEquals("Ramesh", q.unknownName());
    }

    @Test
    void generalQuestionsAreNotFarmerQuestions() {
        for (String s : new String[] {"How can I grow rice?", "When should I harvest wheat?",
                "What is crop rotation?", "Best fertilizer for good yield in cotton"}) {
            FarmerQuery q = FarmerQueryParser.parse(s);

            assertTrue(q == null || q.unknownName() == null, s);
        }
    }

    @Test
    void teluguAndHindiPeriodWordsAreUnderstood() {
        assertEquals(FarmerQuery.Period.LAST_MONTH,
                FarmerQueryParser.parse("Kumaran ki gata nela harvest cheppu").period());
        assertEquals(FarmerQuery.Period.LAST_MONTH,
                FarmerQueryParser.parse("Kumaran ka pichle mahine ka harvest batao").period());
    }

    @Test
    void notAFarmRecordQuestion() {
        assertNull(FarmerQueryParser.parse("Hello there"));
    }

    @Test
    void userNotFoundIsExactInEnglish() {
        assertEquals("User not found.", FarmerReplies.userNotFound("en", false));
    }

    @Test
    void fullNameIsExtracted() {
        FarmerQuery q = FarmerQueryParser.parse("Show the details of a farmer named Rahul Sharma.");
        assertNotNull(q);
        assertEquals("Rahul Sharma", q.unknownName());
    }

    @Test
    void allFarmerQuestionsAreRecognised() {
        assertEquals(FarmerQuery.Topic.FARMERS_HARVESTED,
                FarmerQueryParser.parse("Which farmers have harvested their crops?").topic());
        assertEquals(FarmerQuery.Topic.TOTAL_HARVEST,
                FarmerQueryParser.parse("Give me the total harvest quantity from all farmers.").topic());
        assertEquals(FarmerQuery.Topic.FARMER_LIST,
                FarmerQueryParser.parse("Show all farmers registered in the system").topic());
    }

    @Test
    void howToQuestionsAreNotNames() {
        FarmerQuery q = FarmerQueryParser.parse("How to register for crop insurance?");
        assertTrue(q == null || q.unknownName() == null);
    }

    @Test
    void namedNotFoundMessage() {
        assertEquals("User not found. Registered name checked: Rahul Sharma.",
                FarmerReplies.userNotFound("Rahul Sharma", "en", false));
    }
}
