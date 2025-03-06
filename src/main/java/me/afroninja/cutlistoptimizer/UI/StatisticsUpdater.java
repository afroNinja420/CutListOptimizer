package me.afroninja.cutlistoptimizer.UI;

import javafx.scene.control.TextArea;
import me.afroninja.cutlistoptimizer.Model.Rectangle;
import me.afroninja.cutlistoptimizer.Model.StockSheet;

import java.util.List;

public class StatisticsUpdater {
    public static void updateStatistics(TextArea statistics, List<StockSheet> usedSheets, List<me.afroninja.cutlistoptimizer.Model.Panel> unplacedPanels,
                                        int currentSheetIndex, int totalPanels, double usedArea, double totalArea, int totalCuts) {
        StringBuilder stats = new StringBuilder();
        stats.append("Global Statistics\n").append(new Separator().getText()).append("\n");
        stats.append(String.format("Used stock sheets: %d\n", usedSheets.size()));
        stats.append(String.format("Total used area: %.0f %s\n", usedArea, "%"));
        stats.append(String.format("Total wasted area: %.0f %s\n", (totalArea - usedArea) / totalArea * 100, "%"));
        stats.append(String.format("Total cuts: %d\n", totalCuts));
        stats.append(String.format("Total cut length: %.0f\n", totalCuts * 0.13));
        stats.append(String.format("Cut / blade / kerf thickness: %.2f\n", 0.13));
        stats.append("Optimization priority: Least wasted area, minimum cuts\n\n");

        stats.append("Sheet Statistics\n").append(new Separator().getText()).append("\n");
        if (!usedSheets.isEmpty()) {
            StockSheet sheet = usedSheets.get(currentSheetIndex);
            stats.append(String.format("Stock sheet: %d x %d %s %d\n", (int)sheet.getLength(), (int)sheet.getWidth(), "sheet", currentSheetIndex + 1));
            stats.append(String.format("Used area: %.0f %s\n", usedArea / usedSheets.size(), "%"));
            stats.append(String.format("Wasted area: %.0f %s\n", ((sheet.getLength() * sheet.getWidth() - usedArea / usedSheets.size()) / (sheet.getLength() * sheet.getWidth()) * 100), "%"));
            stats.append(String.format("Cut length: %.0f\n", totalCuts * 0.13 / usedSheets.size()));
            stats.append(String.format("Panels: %d\n", totalPanels - unplacedPanels.size()));
            stats.append(String.format("Wasted panels: %d\n", unplacedPanels.size()));
        }

        stats.append("Cuts\n").append(new Separator().getText()).append("\n");
        stats.append("#\tCut\tResult\n");
        // Placeholder cuts based on rectangles (to be refined)
        for (int i = 0; i < Main.getInstance().bestResult.get("Guillotine").getRectangles().size(); i++) {
            Rectangle rect = Main.getInstance().bestResult.get("Guillotine").getRectangles().get(i);
            stats.append(String.format("%d\t%d x %d\t%s\n", i + 1, (int)(rect.width / 5), (int)(rect.height / 5), "part " + i));
        }

        if (!unplacedPanels.isEmpty()) {
            stats.append("Unable to Fit\n").append(new Separator().getText()).append("\n");
            stats.append("Label\tQty\n");
            for (me.afroninja.cutlistoptimizer.Model.Panel panel : unplacedPanels) {
                stats.append(String.format("%s\t%d\n", panel.getLabel(), panel.getQuantity()));
            }
        }

        statistics.setText(stats.toString());
    }
}