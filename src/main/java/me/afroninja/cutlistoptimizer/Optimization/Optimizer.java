package me.afroninja.cutlistoptimizer.Optimization;

import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.collections.ObservableList;
import me.afroninja.cutlistoptimizer.Model.*;
import me.afroninja.cutlistoptimizer.UI.LoadingOverlay;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class Optimizer {
    public static void optimize(ObservableList<Panel> panels, ObservableList<StockSheet> stockSheets,
                                TextField thicknessInput, Canvas canvas, TextArea statistics,
                                List<StockSheet> usedSheets, List<Panel> unplacedPanels,
                                Map<String, OptimizationResult> bestResult, double usedArea, double totalArea,
                                int totalCuts, int currentSheetIndex, LoadingOverlay loadingOverlay,
                                Runnable updateStatistics) {
        final double[] usedAreaArray = {usedArea};
        final double[] totalAreaArray = {totalArea};
        final int[] totalCutsArray = {totalCuts};
        final int[] currentSheetIndexArray = {currentSheetIndex};

        final LoadingOverlay finalLoadingOverlay = loadingOverlay;
        final AtomicBoolean stopOptimization = new AtomicBoolean(false);

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

                Map<String, OptimizationResult> localBestResult = new HashMap<>();
                localBestResult.clear();

                Map<String, BiFunction<List<Panel>, List<StockSheet>, OptimizationResult>> algorithms = new HashMap<>();
                algorithms.put("FFDH", (p, s) -> ffdhAlgorithm(p, s, thicknessInput));
                algorithms.put("BFDH", (p, s) -> bfdhAlgorithm(p, s, thicknessInput));
                algorithms.put("Guillotine", (p, s) -> guillotineAlgorithm(p, s, thicknessInput));

                OptimizationResult best = null;
                int totalSteps = algorithms.size() * panelsToOptimize.size();
                int currentStep = 0;

                for (Map.Entry<String, BiFunction<List<Panel>, List<StockSheet>, OptimizationResult>> entry : algorithms.entrySet()) {
                    if (stopOptimization.get()) break;

                    OptimizationResult result = entry.getValue().apply(new ArrayList<>(panelsToOptimize), new ArrayList<>(availableSheets));
                    localBestResult.put(entry.getKey(), result);
                    currentStep += panelsToOptimize.size();

                    if (best == null || result.getWastePercentage() < best.getWastePercentage() ||
                            (result.getWastePercentage() == best.getWastePercentage() && result.getTotalCuts() < best.getTotalCuts())) {
                        best = result;
                    }

                    updateProgress(currentStep, totalSteps);
                    updateMessage(String.format("Searching for best solution - %.0f%%", best.getWastePercentage()));
                    Thread.sleep(50);
                }

                if (best != null) {
                    GraphicsContext gc = canvas.getGraphicsContext2D();
                    gc.setFill(javafx.scene.paint.Color.WHITE);
                    gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                    Map<String, javafx.scene.paint.Color> panelColorMap = new HashMap<>();
                    AtomicInteger colorIndex = new AtomicInteger(0);
                    String[] colors = {"0xFFB6C1", "0x90EE90", "0xFFFFE0", "0xD8BFD8", "0xFFA07A"};
                    double canvasPadding = 10.0;
                    double scaleFactor = 5.0;

                    List<Rectangle> rectangles = best.getRectangles();
                    List<StockSheet> localUsedSheets = best.getUsedSheets();
                    for (int i = 0; i < localUsedSheets.size(); i++) {
                        StockSheet sheet = localUsedSheets.get(i);
                        double sheetLength = sheet.getLength() * scaleFactor;
                        double sheetWidth = sheet.getWidth() * scaleFactor;
                        double yPos = i == 0 ? canvasPadding : rectangles.get(rectangles.size() - 1).getY() + sheetWidth + 20 * scaleFactor;
                        gc.setStroke(javafx.scene.paint.Color.GRAY);
                        gc.setLineWidth(2);
                        gc.strokeRect(canvasPadding, yPos, sheetLength, sheetWidth);

                        for (Rectangle rect : rectangles) {
                            if (rect.getY() >= yPos && rect.getY() < yPos + sheetWidth) {
                                Panel panel = best.getPlacedPanels().get(rect);
                                if (panel != null) {
                                    String sizeKey = panel.getLength() + "x" + panel.getWidth();
                                    javafx.scene.paint.Color panelColor = panelColorMap.computeIfAbsent(sizeKey,
                                            k -> javafx.scene.paint.Color.web(colors[colorIndex.incrementAndGet() % colors.length]));
                                    gc.setFill(panelColor);
                                    gc.fillRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
                                    gc.setStroke(javafx.scene.paint.Color.BLACK);
                                    gc.setLineWidth(2);
                                    gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

                                    double labelX = rect.getX() + (rect.getWidth() / 2) - (gc.getFont().getSize() * panel.getLabel().length() / 4);
                                    double labelY = rect.getY() + (rect.getHeight() / 2) + (gc.getFont().getSize() / 3);
                                    gc.setFill(javafx.scene.paint.Color.BLACK);
                                    gc.fillText(panel.getLabel(), labelX, labelY);
                                }
                            }
                        }
                    }

                    if (best != null) {
                        best.setCustomData(localBestResult);
                    }
                }

                return best;
            }
        };

        optimizationTask.setOnSucceeded(event -> {
            OptimizationResult best = optimizationTask.getValue();
            if (best != null) {
                usedSheets.clear();
                usedSheets.addAll(best.getUsedSheets());
                unplacedPanels.clear();
                unplacedPanels.addAll(best.getUnplacedPanels());
                bestResult.clear();
                @SuppressWarnings("unchecked")
                Map<String, OptimizationResult> localBestResult = (Map<String, OptimizationResult>) best.getCustomData();
                if (localBestResult != null) {
                    bestResult.putAll(localBestResult);
                }
                usedAreaArray[0] = best.getUsedArea();
                totalAreaArray[0] = best.getTotalArea();
                totalCutsArray[0] = best.getTotalCuts();
                currentSheetIndexArray[0] = 0;
                updateStatistics.run();
            }
            finalLoadingOverlay.hideLoadingOverlay();
        });

        optimizationTask.setOnFailed(event -> {
            System.err.println("Optimization failed: " + optimizationTask.getException());
            finalLoadingOverlay.hideLoadingOverlay();
        });

        optimizationTask.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 1.0) {
                finalLoadingOverlay.hideLoadingOverlay();
            }
        });

        finalLoadingOverlay.getProgressBar().progressProperty().bind(optimizationTask.progressProperty());
        finalLoadingOverlay.getProgressLabel().textProperty().bind(optimizationTask.messageProperty());
        finalLoadingOverlay.getStopButton().setOnAction(e -> {
            stopOptimization.set(true);
            finalLoadingOverlay.hideLoadingOverlay();
        });

        new Thread(optimizationTask).start();

        usedArea = usedAreaArray[0];
        totalArea = totalAreaArray[0];
        totalCuts = totalCutsArray[0];
        currentSheetIndex = currentSheetIndexArray[0];
    }

    private static OptimizationResult ffdhAlgorithm(List<Panel> panels, List<StockSheet> sheets, TextField thicknessInput) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = 0.0;
        try {
            cutThickness = Double.parseDouble(thicknessInput.getText().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid Cut Thickness. Using 0.0.");
        }
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.getUsedSheets().add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.getUsedSheets().get(0).getLength() * scaleFactor, result.getUsedSheets().get(0).getWidth() * scaleFactor));
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
                    if (rect.getWidth() >= panelLength && rect.getHeight() >= panelWidth) {
                        result.getPlacedPanels().put(new Rectangle(rect.getX(), rect.getY(), panelLength, panelWidth), panel);
                        result.setUsedArea(result.getUsedArea() + (panel.getLength() * panel.getWidth()));
                        result.setTotalCuts(result.getTotalCuts() + 2);

                        if (rect.getWidth() > panelLength && rect.getHeight() > panelWidth) {
                            rectangles.add(new Rectangle(rect.getX() + panelLength, rect.getY(), rect.getWidth() - panelLength, panelWidth));
                            rectangles.add(new Rectangle(rect.getX(), rect.getY() + panelWidth, panelLength, rect.getHeight() - panelWidth));
                        } else if (rect.getWidth() > panelLength) {
                            rectangles.set(i, new Rectangle(rect.getX() + panelLength, rect.getY(), rect.getWidth() - panelLength, rect.getHeight()));
                        } else if (rect.getHeight() > panelWidth) {
                            rectangles.set(i, new Rectangle(rect.getX(), rect.getY() + panelWidth, rect.getWidth(), rect.getHeight() - panelWidth));
                        } else {
                            rectangles.remove(i);
                        }
                        placed = true;
                        break;
                    }
                }

                if (!placed && !sheets.isEmpty()) {
                    result.getUsedSheets().add(sheets.remove(0));
                    rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).getY() + result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                            result.getUsedSheets().get(result.getUsedSheets().size() - 1).getLength() * scaleFactor, result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor));
                }
            }
            if (!placed) result.getUnplacedPanels().add(panel);
        }

        for (StockSheet sheet : result.getUsedSheets()) {
            result.setTotalArea(result.getTotalArea() + (sheet.getLength() * sheet.getWidth()));
        }
        result.getRectangles().addAll(rectangles);
        result.setWastePercentage((result.getTotalArea() - result.getUsedArea()) / result.getTotalArea() * 100);
        return result;
    }

    private static OptimizationResult bfdhAlgorithm(List<Panel> panels, List<StockSheet> sheets, TextField thicknessInput) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = 0.0;
        try {
            cutThickness = Double.parseDouble(thicknessInput.getText().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid Cut Thickness. Using 0.0.");
        }
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.getUsedSheets().add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.getUsedSheets().get(0).getLength() * scaleFactor, result.getUsedSheets().get(0).getWidth() * scaleFactor));
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
                    if (rect.getWidth() >= panelLength && rect.getHeight() >= panelWidth) {
                        double fit = Math.abs(rect.getWidth() - panelLength) + Math.abs(rect.getHeight() - panelWidth);
                        if (fit < bestFit) {
                            bestFit = fit;
                            bestRect = rect;
                        }
                    }
                }

                if (bestRect != null) {
                    int index = rectangles.indexOf(bestRect);
                    result.getPlacedPanels().put(new Rectangle(bestRect.getX(), bestRect.getY(), panelLength, panelWidth), panel);
                    result.setUsedArea(result.getUsedArea() + (panel.getLength() * panel.getWidth()));
                    result.setTotalCuts(result.getTotalCuts() + 2);

                    if (bestRect.getWidth() > panelLength && bestRect.getHeight() > panelWidth) {
                        rectangles.add(new Rectangle(bestRect.getX() + panelLength, bestRect.getY(), bestRect.getWidth() - panelLength, panelWidth));
                        rectangles.add(new Rectangle(bestRect.getX(), bestRect.getY() + panelWidth, panelLength, bestRect.getHeight() - panelWidth));
                    } else if (bestRect.getWidth() > panelLength) {
                        rectangles.set(index, new Rectangle(bestRect.getX() + panelLength, bestRect.getY(), bestRect.getWidth() - panelLength, bestRect.getHeight()));
                    } else if (bestRect.getHeight() > panelWidth) {
                        rectangles.set(index, new Rectangle(bestRect.getX(), bestRect.getY() + panelWidth, bestRect.getWidth(), bestRect.getHeight() - panelWidth));
                    } else {
                        rectangles.remove(index);
                    }
                    placed = true;
                }
            }
            if (!placed && !sheets.isEmpty()) {
                result.getUsedSheets().add(sheets.remove(0));
                rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).getY() + result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                        result.getUsedSheets().get(result.getUsedSheets().size() - 1).getLength() * scaleFactor, result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor));
            } else if (!placed) {
                result.getUnplacedPanels().add(panel);
            }
        }

        for (StockSheet sheet : result.getUsedSheets()) {
            result.setTotalArea(result.getTotalArea() + (sheet.getLength() * sheet.getWidth()));
        }
        result.getRectangles().addAll(rectangles);
        result.setWastePercentage((result.getTotalArea() - result.getUsedArea()) / result.getTotalArea() * 100);
        return result;
    }

    private static OptimizationResult guillotineAlgorithm(List<Panel> panels, List<StockSheet> sheets, TextField thicknessInput) {
        OptimizationResult result = new OptimizationResult();
        List<Rectangle> rectangles = new ArrayList<>();
        double cutThickness = 0.0;
        try {
            cutThickness = Double.parseDouble(thicknessInput.getText().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid Cut Thickness. Using 0.0.");
        }
        double scaleFactor = 5.0;

        if (!sheets.isEmpty()) {
            result.getUsedSheets().add(sheets.remove(0));
            rectangles.add(new Rectangle(10.0, 10.0, result.getUsedSheets().get(0).getLength() * scaleFactor, result.getUsedSheets().get(0).getWidth() * scaleFactor));
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
                    if (rect.getWidth() >= panelLength && rect.getHeight() >= panelWidth) {
                        result.getPlacedPanels().put(new Rectangle(rect.getX(), rect.getY(), panelLength, panelWidth), panel);
                        result.setUsedArea(result.getUsedArea() + (panel.getLength() * panel.getWidth()));
                        result.setTotalCuts(result.getTotalCuts() + 2);

                        if (rect.getWidth() - panelLength > cutThickness * scaleFactor) {
                            rectangles.add(new Rectangle(rect.getX() + panelLength, rect.getY(), rect.getWidth() - panelLength, rect.getHeight()));
                        }
                        if (rect.getHeight() - panelWidth > cutThickness * scaleFactor) {
                            rectangles.add(new Rectangle(rect.getX(), rect.getY() + panelWidth, panelLength, rect.getHeight() - panelWidth));
                        }
                        rectangles.remove(i);
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed && !sheets.isEmpty()) {
                result.getUsedSheets().add(sheets.remove(0));
                rectangles.add(new Rectangle(10.0, rectangles.isEmpty() ? 10.0 : rectangles.get(rectangles.size() - 1).getY() + result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor + 20 * scaleFactor,
                        result.getUsedSheets().get(result.getUsedSheets().size() - 1).getLength() * scaleFactor, result.getUsedSheets().get(result.getUsedSheets().size() - 1).getWidth() * scaleFactor));
            } else if (!placed) {
                result.getUnplacedPanels().add(panel);
            }
        }

        for (StockSheet sheet : result.getUsedSheets()) {
            result.setTotalArea(result.getTotalArea() + (sheet.getLength() * sheet.getWidth()));
        }
        result.getRectangles().addAll(rectangles);
        result.setWastePercentage((result.getTotalArea() - result.getUsedArea()) / result.getTotalArea() * 100);
        return result;
    }
}