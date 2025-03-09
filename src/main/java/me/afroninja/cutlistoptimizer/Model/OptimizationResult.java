package me.afroninja.cutlistoptimizer.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizationResult {
    private List<StockSheet> usedSheets;
    private List<Panel> unplacedPanels;
    private Map<Rectangle, Panel> placedPanels;
    private List<Rectangle> rectangles;
    private double usedArea;
    private double totalArea;
    private int totalCuts;
    private double wastePercentage;
    private Object customData;

    public OptimizationResult() {
        this.usedSheets = new ArrayList<>();
        this.unplacedPanels = new ArrayList<>();
        this.placedPanels = new HashMap<>();
        this.rectangles = new ArrayList<>();
        this.usedArea = 0.0;
        this.totalArea = 0.0;
        this.totalCuts = 0;
        this.wastePercentage = 0.0;
    }

    // Getters and setters
    public List<StockSheet> getUsedSheets() { return usedSheets; }
    public void setUsedSheets(List<StockSheet> usedSheets) { this.usedSheets = usedSheets; }
    public List<Panel> getUnplacedPanels() { return unplacedPanels; }
    public void setUnplacedPanels(List<Panel> unplacedPanels) { this.unplacedPanels = unplacedPanels; }
    public Map<Rectangle, Panel> getPlacedPanels() { return placedPanels; }
    public void setPlacedPanels(Map<Rectangle, Panel> placedPanels) { this.placedPanels = placedPanels; }
    public List<Rectangle> getRectangles() { return rectangles; }
    public void setRectangles(List<Rectangle> rectangles) { this.rectangles = rectangles; }
    public double getUsedArea() { return usedArea; }
    public void setUsedArea(double usedArea) { this.usedArea = usedArea; }
    public double getTotalArea() { return totalArea; }
    public void setTotalArea(double totalArea) { this.totalArea = totalArea; }
    public int getTotalCuts() { return totalCuts; }
    public void setTotalCuts(int totalCuts) { this.totalCuts = totalCuts; }
    public double getWastePercentage() { return wastePercentage; }
    public void setWastePercentage(double wastePercentage) { this.wastePercentage = wastePercentage; }
    public Object getCustomData() { return customData; }
    public void setCustomData(Object customData) { this.customData = customData; }
}