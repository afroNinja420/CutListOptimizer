package me.afroninja.cutlistoptimizer.Optimization;

import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import me.afroninja.cutlistoptimizer.Main;
import me.afroninja.cutlistoptimizer.Model.*;
import me.afroninja.cutlistoptimizer.UI.LoadingOverlay;

import java.util.*;

@FunctionalInterface
interface BiFunction<T, U, R> {
    R apply(T t, U u);
}

public class Optimizer {
    private static volatile boolean stopOptimization = false;

    public static void optimize(ObservableList<Panel> panels, ObservableList<StockSheet> stockSheets,
                                TextField thicknessInput, Canvas canvas, TextArea statistics,
                                List<StockSheet> usedSheets, List<Panel> unplacedPanels,
                                Map<String, OptimizationResult> bestResult, double usedArea, double totalArea,
                                int totalCuts, int currentSheetIndex, LoadingOverlay loadingOverlay,
                                Runnable updateStatistics) {
        Task<OptimizationResult> optimizationTask = new Task<>() {
            @Override
            protected OptimizationResult call() throws Exception {
                double cutThickness = 0.0;
                try {
                    cutThickness = Double.parseDouble(thicknessInput.getText().trim());
                } catch (NumberFormatException e) {
                    System.err.println("Invalid Cut Thickness. Using 0.0.");
                }

                List<Panel> panelsToOptimize = new ArrayList<>();
                for (Panel panel : panels) {
                    for (int i = 0; i < panel.getQuantity(); i++) {
                        panelsToOptimize.add(new Panel(panel.getLength(), panel.getWidth(), 1, panel.getLabel()));
                    }
                }

                List<StockSheet> availableSheets = new ArrayList<>();
                for (StockSheet sheet : stockSheets) {
                    for (int i = 0; i < sheet.getQuantity(); i++) {
                        availableSheets.add(new StockSheet(sheet.getLength(), sheet.getWidth(), 1, sheet.getLabel()));
                    }
                }

                panelsToOptimize.sort((p1, p2) -> Double.compare(p2.getLength() * p2.getWidth(), p1.getLength() * p1.getWidth()));
                usedSheets.clear();
                unplacedPanels.clear();
                bestResult.clear();

                Map<String, BiFunction<List<Panel>, List<StockSheet>, OptimizationResult>> algorithms = new HashMap<>();
                algorithms.put("FFDH", Optimizer::ffdhAlgorithm);
                algorithms.put("BFDH", Optimizer::bfdhAlgorithm);
                algorithms.put("Guillotine", Optimizer::guillotineAlgorithm);

                OptimizationResult best = null;
                int totalSteps = algorithms.size() * panelsToOptimize.size();
                int currentStep = 0;

                for (Map.Entry<String, BiFunction<List<Panel>, List<StockSheet>, OptimizationResult>> entry : algorithms.entrySet()) {
                    if (stopOptimization) break;

                    OptimizationResult result = entry.getValue().apply(new ArrayList<>(panelsToOptimize), new ArrayList<>(availableSheets));
                    bestResult.put(entry.getKey(), result);
                    currentStep += panelsToOptimize.size();

                    if (best == null || result.getWastePercentage() < best.getWastePercentage() ||
                            (result.getWastePercentage() == best.getWastePercentage() && result.getTotalCuts() < best.getTotalCuts())) {
                        best = result;
                    }

                    updateProgress(currentStep, totalSteps);
                    updateMessage(String.format("Searching for best solution - %.0f%%", best.getWastePercentage()));
                    Thread.sleep(50); // Simulate processing time
                }

                if (best != null) {
                    usedSheets.clear();
                    usedSheets.addAll(best.getUsedSheets());
                    unplacedPanels.clear();
                    unplacedPanels.addAll(best.getUnplacedPanels());
                    usedArea = best.getUsedArea();
                    totalArea = best.getTotalArea();
                    totalCuts = best.getTotalCuts();

                    // Draw the best result
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                    Map<String, javafx.scene.paint.Color> panelColorMap = new HashMap<>();
                    int colorIndex = 0;
                    String[] colors = {"0xFFB6C1", "0x90EE90", "0xFFFFE0", "0xD8BFD8", "0xFFA07A"};
                    double canvasPadding = 10.0;
                    double scaleFactor = 5.0;

                    List<Rectangle> rectangles = best.getRectangles();
                    for (int i = 0; i < usedSheets.size(); i++) {
                        StockSheet sheet = usedSheets.get(i);
                        double sheetLength = sheet.getLength() * scaleFactor;
                        double sheetWidth = sheet.getWidth() * scaleFactor;
                        double yPos = i == 0 ? canvasPadding : rectangles.get(rectangles.size() - 1).y + sheetWidth + 20 * scaleFactor;
                        gc.setStroke(javafx.scene.paint.Color.GRAY);
                        gc.setLineWidth(2);
                        gc.strokeRect(canvasPadding, yPos, sheetLength, sheetWidth);

                        for (Rectangle rect : rectangles) {
                            if (rect.y >= yPos && rect.y < yPos + sheetWidth) {
                                Panel panel = best.getPlacedPanels().get(rect);
                                if (panel != null) {
                                    String sizeKey = panel.getLength() + "x" + panel.getWidth();
                                    javafx.scene.paint.Color panelColor = panelColorMap.computeIfAbsent(sizeKey,
                                            k -> javafx.scene.paint.Color.web(colors[colorIndex++ % colors.length]));
                                    gc.setFill(panelColor);
                                    gc.fillRect(rect.x, rect.y, rect.width, rect.height);
                                    gc.setStroke(javafx.scene.paint.Color.BLACK);
                                    gc.setLineWidth(2);
                                    gc.strokeRect(rect.x, rect.y, rect.width, rect.height);

                                    double labelX = rect.x + (rect.width / 2) - (gc.getFont().getSize() * panel.getLabel().length() / 4);
                                    double labelY = rect.y + (rect.height / 2) + (gc.getFont().getSize() / 3);
                                    gc.setFill(javafx.scene.paint.Color.BLACK);
                                    gc.fillText(panel.getLabel(), labelX, labelY);
                                }
                            }
                        }
                    }
                }

                return best;
            }
        };

        optimizationTask.setOnSucceeded(event -> {
            OptimizationResult best = optimizationTask.getValue();
            if (best != null) {
                updateStatistics.run();
            }
            loadingOverlay.hideLoadingOverlay();
        });

        optimizationTask.setOnFailed(event -> {
            System.err.println("Optimization failed: " + optimizationTask.getException());
            loadingOverlay.hideLoadingOverlay();
        });

        optimizationTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                loadingOverlay.hideLoadingOverlay();
            }
        });

        loadingOverlay.getProgressBar().progressProperty().bind(optimizationTask.progressProperty());
        loadingOverlay.getProgressLabel().textProperty().bind(optimizationTask.messageProperty());
        loadingOverlay.getStopButton().setOnAction(e -> {
            stopOptimization = true;
            loadingOverlay.hideLoadingOverlay();
        });

        new Thread(optimizationTask).start();
    }

    private static OptimizationResult ffdhAlgorithm(List<Panel> panels, List<StockSheet> sheets) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = Double.parseDouble(Main.getInstance().thicknessInput.getText().trim());
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.usedSheets.add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.usedSheets.get(0).getLength() * scaleFactor, result.usedSheets.get(0).getWidth() * scaleFactor));
        }

        panels.sort((p1, p2) -> Double.compare(p2.getWidth(), p1.getWidth()));

        for (Panel panel : panels) {
            boolean placed = false;
            double minLength = panel.getLength() + cutThickness;
            double minWidth = panel.getWidth() + cutThickness;
            double[] dims = {minLength, minWidth};

            for (int rot = 0; rot < 2 && !placed; rot++) {
                double panelLength = dims[rot] * scaleFactor;
                double panelWidth = dims[1 - rot] * scaleFactor;

                for (int i = 0; i < rectangles.size(); i++) {
                    Rectangle rect = rectangles.get(i);
                    if (rect.width >= panelLength && rect.height >= panelWidth) {
                        result.placedPanels.put(new Rectangle(rect.x, rect.y, panelLength, panelWidth), panel);
                        result.usedArea += panel.getLength() * panel.getWidth();
                        result.totalCuts += 2;

                        if (rect.width > panelLength && rect.height > panelWidth) {
                            rectangles.add(new Rectangle(rect.x + panelLength, rect.y, rect.width - panelLength, panelWidth));
                            rectangles.add(new Rectangle(rect.x, rect.y + panelWidth, panelLength, rect.height - panelWidth));
                        } else if (rect.width > panelLength) {
                            rectangles.set(i, new Rectangle(rect.x + panelLength, rect.y, rect.width - panelLength, rect.height));
                        } else if (rect.height > panelWidth) {
                            rectangles.set(i, new Rectangle(rect.x, rect.y + panelWidth, rect.width, rect.height - panelWidth));
                        } else {
                            rectangles.remove(i);
                        }
                        placed = true;
                        break;
                    }
                }

                if (!placed && !sheets.isEmpty()) {
                    result.usedSheets.add(sheets.remove(0));
                    rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).y + result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                            result.usedSheets.get(result.usedSheets.size() - 1).getLength() * scaleFactor, result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor));
                    i--;
                }
            }
            if (!placed) result.unplacedPanels.add(panel);
        }

        for (StockSheet sheet : result.usedSheets) {
            result.totalArea += sheet.getLength() * sheet.getWidth();
        }
        result.rectangles = rectangles;
        result.setWastePercentage((result.totalArea - result.usedArea) / result.totalArea * 100);
        return result;
    }

    private static OptimizationResult bfdhAlgorithm(List<Panel> panels, List<StockSheet> sheets) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = Double.parseDouble(Main.getInstance().thicknessInput.getText().trim());
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.usedSheets.add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.usedSheets.get(0).getLength() * scaleFactor, result.usedSheets.get(0).getWidth() * scaleFactor));
        }

        panels.sort((p1, p2) -> Double.compare(p2.getWidth(), p1.getWidth()));

        for (Panel panel : panels) {
            boolean placed = false;
            double minLength = panel.getLength() + cutThickness;
            double minWidth = panel.getWidth() + cutThickness;
            double[] dims = {minLength, minWidth};

            Rectangle bestRect = null;
            double bestFit = Double.MAX_VALUE;

            for (int rot = 0; rot < 2 && !placed; rot++) {
                double panelLength = dims[rot] * scaleFactor;
                double panelWidth = dims[1 - rot] * scaleFactor;

                for (Rectangle rect : rectangles) {
                    if (rect.width >= panelLength && rect.height >= panelWidth) {
                        double fit = Math.abs(rect.width - panelLength) + Math.abs(rect.height - panelWidth);
                        if (fit < bestFit) {
                            bestFit = fit;
                            bestRect = rect;
                        }
                    }
                }

                if (bestRect != null) {
                    int index = rectangles.indexOf(bestRect);
                    result.placedPanels.put(new Rectangle(bestRect.x, bestRect.y, dims[rot] * scaleFactor, dims[1 - rot] * scaleFactor), panel);
                    result.usedArea += panel.getLength() * panel.getWidth();
                    result.totalCuts += 2;

                    if (bestRect.width > panelLength && bestRect.height > panelWidth) {
                        rectangles.add(new Rectangle(bestRect.x + panelLength, bestRect.y, bestRect.width - panelLength, panelWidth));
                        rectangles.add(new Rectangle(bestRect.x, bestRect.y + panelWidth, panelLength, bestRect.height - panelWidth));
                    } else if (bestRect.width > panelLength) {
                        rectangles.set(index, new Rectangle(bestRect.x + panelLength, bestRect.y, bestRect.width - panelLength, bestRect.height));
                    } else if (bestRect.height > panelWidth) {
                        rectangles.set(index, new Rectangle(bestRect.x, bestRect.y + panelWidth, bestRect.width, bestRect.height - panelWidth));
                    } else {
                        rectangles.remove(index);
                    }
                    placed = true;
                }
            }
            if (!placed && !sheets.isEmpty()) {
                result.usedSheets.add(sheets.remove(0));
                rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).y + result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                        result.usedSheets.get(result.usedSheets.size() - 1).getLength() * scaleFactor, result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor));
            } else if (!placed) {
                result.unplacedPanels.add(panel);
            }
        }

        for (StockSheet sheet : result.usedSheets) {
            result.totalArea += sheet.getLength() * sheet.getWidth();
        }
        result.rectangles = rectangles;
        result.setWastePercentage((result.totalArea - result.usedArea) / result.totalArea * 100);
        return result;
    }

    private static OptimizationResult guillotineAlgorithm(List<Panel> panels, List<StockSheet> sheets) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = Double.parseDouble(Main.getInstance().thicknessInput.getText().trim());
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.usedSheets.add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.usedSheets.get(0).getLength() * scaleFactor, result.usedSheets.get(0).getWidth() * scaleFactor));
        }

        panels.sort((p1, p2) -> Double.compare(p2.getLength() * p2.getWidth(), p1.getLength() * p1.getWidth()));

        for (Panel panel : panels) {
            boolean placed = false;
            double minLength = panel.getLength() + cutThickness;
            double minWidth = panel.getWidth() + cutThickness;
            double[] dims = {minLength, minWidth};

            for (int rot = 0; rot < 2 && !placed; rot++) {
                double panelLength = dims[rot] * scaleFactor;
                double panelWidth = dims[1 - rot] * scaleFactor;

                for (int i = 0; i < rectangles.size(); i++) {
                    Rectangle rect = rectangles.get(i);
                    if (rect.width >= panelLength && rect.height >= panelWidth) {
                        result.placedPanels.put(new Rectangle(rect.x, rect.y, panelLength, panelWidth), panel);
                        result.usedArea += panel.getLength() * panel.getWidth();
                        result.totalCuts += 2;

                        if (rect.width - panelLength > cutThickness * scaleFactor) {
                            rectangles.add(new Rectangle(rect.x + panelLength, rect.y, rect.width - panelLength, rect.height));
                        }
                        if (rect.height - panelWidth > cutThickness * scaleFactor) {
                            rectangles.add(new Rectangle(rect.x, rect.y + panelWidth, panelLength, rect.height - panelWidth));
                        }
                        rectangles.remove(i);
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed && !sheets.isEmpty()) {
                result.usedSheets.add(sheets.remove(0));
                rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).y + result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                        result.usedSheets.get(result.usedSheets.size() - 1).getLength() * scaleFactor, result.usedSheets.get(result.usedSheets.size() - 1).getWidth() * scaleFactor));
            } else if (!placed) {
                result.unplacedPanels.add(panel);
            }
        }

        for (StockSheet sheet : result.usedSheets) {
            result.totalArea += sheet.getLength() * sheet.getWidth();
        }
        result.rectangles = rectangles;
        result.setWastePercentage((result.totalArea - result.usedArea) / result.totalArea * 100);
        return result;
    }

    private static Main getInstance() {
        return (Main) Main.getInstance();
    }
}