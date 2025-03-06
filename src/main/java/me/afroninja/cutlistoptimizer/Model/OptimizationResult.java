package me.afroninja.cutlistoptimizer.Model;

import java.util.*;

public class OptimizationResult {
    private List<StockSheet> usedSheets = new ArrayList<>();
    private List<Panel> unplacedPanels = new ArrayList<>();
    private Map<Rectangle, Panel> placedPanels = new HashMap<>();
    private List<Rectangle> rectangles = new ArrayList<>();
    private double usedArea;
    private double totalArea;
    private int totalCuts;
    private double wastePercentage;

    public List<StockSheet> getUsedSheets() { return usedSheets; }
    public List<Panel> getUnplacedPanels() { return unplacedPanels; }
    public Map<Rectangle, Panel> getPlacedPanels() { return placedPanels; }
    public List<Rectangle> getRectangles() { return rectangles; }
    public double getUsedArea() { return usedArea; }
    public double getTotalArea() { return totalArea; }
    public int getTotalCuts() { return totalCuts; }
    public double getWastePercentage() { return wastePercentage; }

    public void setUsedArea(double usedArea) { this.usedArea = usedArea; }
    public void setTotalArea(double totalArea) { this.totalArea = totalArea; }
    public void setTotalCuts(int totalCuts) { this.totalCuts = totalCuts; }
    public void setWastePercentage(double wastePercentage) { this.wastePercentage = wastePercentage; }
}