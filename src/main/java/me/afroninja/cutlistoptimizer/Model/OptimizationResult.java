package me.afroninja.cutlistoptimizer.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizationResult {
    private List<StockSheet> usedSheets = new ArrayList<>();
    private List<Panel> unplacedPanels = new ArrayList<>();
    private Map<Rectangle, Panel> placedPanels = new HashMap<>();
    private List<Rectangle> rectangles = new ArrayList<>();
    private double usedArea = 0.0;
    private double totalArea = 0.0;
    private int totalCuts = 0;
    private double wastePercentage = 0.0;
    private Object customData; // Field to store additional data like localBestResult

    // Getters and Setters
    public List<StockSheet> getUsedSheets() {
        return usedSheets;
    }

    public List<Panel> getUnplacedPanels() {
        return unplacedPanels;
    }

    public Map<Rectangle, Panel> getPlacedPanels() {
        return placedPanels;
    }

    public List<Rectangle> getRectangles() {
        return rectangles;
    }

    public double getUsedArea() {
        return usedArea;
    }

    public void setUsedArea(double usedArea) {
        this.usedArea = usedArea;
    }

    public double getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(double totalArea) {
        this.totalArea = totalArea;
    }

    public int getTotalCuts() {
        return totalCuts;
    }

    public void setTotalCuts(int totalCuts) {
        this.totalCuts = totalCuts;
    }

    public double getWastePercentage() {
        return wastePercentage;
    }

    public void setWastePercentage(double wastePercentage) {
        this.wastePercentage = wastePercentage;
    }

    public Object getCustomData() {
        return customData;
    }

    public void setCustomData(Object customData) {
        this.customData = customData;
    }
}