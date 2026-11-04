package com.farmverse.farmverse_chatbot.agro;

import java.time.LocalDate;

public final class FarmerData {

    private FarmerData() {
    }

    public record Farmer(String id, String name) {
    }

    public record FarmCrop(String farmName, String location, String area, String soil,
                           String cropName, String variety, String season, String stage,
                           String sowing, String expected) {
    }

    public record Harvest(String farmName, String cropName, LocalDate date, String quantity, String unit, String revenue) {
    }

    public record FarmerTotal(String name, int harvests, java.math.BigDecimal quantity) {
    }

    public record CropTotal(String crop, String unit, java.math.BigDecimal quantity, int harvests, int farmers) {
    }

    public record Counts(int farms, int crops, int growing, int harvests, boolean hasStage) {
    }

    public record Summary(int farms, int crops, int harvests, LocalDate lastHarvest) {
    }
}
