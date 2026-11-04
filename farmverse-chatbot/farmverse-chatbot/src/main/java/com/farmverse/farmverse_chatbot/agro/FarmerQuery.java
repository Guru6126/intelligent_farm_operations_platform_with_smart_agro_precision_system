package com.farmverse.farmverse_chatbot.agro;

import java.util.List;

public record FarmerQuery(Topic topic, Period period, List<String> words, String unknownName, boolean explicit,
                          List<String> nameWords) {

    public enum Topic {
        HARVEST, CROPS, FARMS, OVERVIEW, REGISTRATION,
        FARMER_LIST, FARMERS_HARVESTED, FARMERS_NOT_HARVESTED, TOTAL_HARVEST, FARMER_COUNT,
        FARM_COUNT, CROP_COUNT, HARVEST_COUNT, TOP_FARMER, LOW_FARMER
    }

    public enum Period { LAST_MONTH, THIS_MONTH, ALL }

    public boolean allFarmers() {
        return switch (topic) {
            case FARMER_LIST, FARMERS_HARVESTED, FARMERS_NOT_HARVESTED, TOTAL_HARVEST, FARMER_COUNT,
        FARM_COUNT, CROP_COUNT, HARVEST_COUNT, TOP_FARMER, LOW_FARMER -> true;
            default -> false;
        };
    }
}
