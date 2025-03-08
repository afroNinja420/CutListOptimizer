package me.afroninja.cutlistoptimizer.UI;

import javafx.scene.control.TextArea;
import javafx.scene.control.Separator;
import me.afroninja.cutlistoptimizer.Model.*;

import java.util.List;

public class StatisticsUpdater {
    public static void updateStatistics(TextArea statistics, List<StockSheet> usedSheets, List<Panel> unplacedPanels,
                                        int currentSheetIndex, int totalPanels, double usedArea, double totalArea,
                                        int totalCuts) {
        statistics.clear();
        statistics.appendText("Global Statistics\n");
        statistics.appendText(new Separator().toString() + "\n");
        statistics.appendText(String.format("Total Panels: %d\n", totalPanels));
        statistics.appendText(String.format("Unplaced Panels: %d\n", unplacedPanels.size()));
        statistics.appendText(String.format("Total Area: %.1f sq units\n", totalArea));
        statistics.appendText(String.format("Used Area: %.1f sq units\n", usedArea));
        statistics.appendText(String.format("Waste Percentage: %.1f%%\n", (totalArea - usedArea) / totalArea * 100));
        statistics.appendText(String.format("Total Cuts: %d\n", totalCuts));

        if (!usedSheets.isEmpty()) {
            statistics.appendText("\nSheet Statistics\n");
            statistics.appendText(new Separator().toString() + "\n");
            StockSheet currentSheet = usedSheets.get(currentSheetIndex);
            statistics.appendText(String.format("Sheet %d of %d\n", currentSheetIndex + 1, usedSheets.size()));
            statistics.appendText(String.format("Length: %.1f, Width: %.1f\n", currentSheet.getLength(), currentSheet.getWidth()));
            statistics.appendText(String.format("Area: %.1f sq units\n", currentSheet.getLength() * currentSheet.getWidth()));

            statistics.appendText("\nCuts\n");
            statistics.appendText(new Separator().toString() + "\n");
            // Assuming rectangles are available from the optimization result (not directly accessible here)
            // This would require passing the OptimizationResult or rectangles list
        }
    }
}