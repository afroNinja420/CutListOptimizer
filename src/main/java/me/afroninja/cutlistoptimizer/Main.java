package me.afroninja.cutlistoptimizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority; // Import for Priority
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL; // Added import for URL
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private ObservableList<Panel> panels = FXCollections.observableArrayList();
    private ObservableList<StockSheet> stockSheets = FXCollections.observableArrayList();
    private Canvas canvas; // Store reference to the Canvas
    private TextArea statistics; // Store reference to the TextArea
    private TextField thicknessInput; // Store reference to the Cut Thickness TextField for optimization

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("CutList Optimizer");

        // Custom DoubleStringConverter to display "0" instead of "0.0" by default
        DoubleStringConverter customDoubleConverter = new DoubleStringConverter() {
            @Override
            public String toString(Double value) {
                if (value == null || value == 0.0) {
                    return "0"; // Display "0" instead of "0.0" for zero values
                }
                return String.format("%.1f", value); // Display one decimal place for non-zero values
            }
        };

        // Panels Table (with editable cells)
        TableView<Panel> panelsTable = new TableView<>(panels);
        TableColumn<Panel, Double> lengthCol = new TableColumn<>("Length");
        lengthCol.setCellValueFactory(cellData -> cellData.getValue().lengthProperty().asObject());
        lengthCol.setCellFactory(TextFieldTableCell.forTableColumn(customDoubleConverter));
        lengthCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setLength(event.getNewValue()));

        TableColumn<Panel, Double> widthCol = new TableColumn<>("Width");
        widthCol.setCellValueFactory(cellData -> cellData.getValue().widthProperty().asObject());
        widthCol.setCellFactory(TextFieldTableCell.forTableColumn(customDoubleConverter));
        widthCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setWidth(event.getNewValue()));

        TableColumn<Panel, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        qtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        qtyCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setQuantity(event.getNewValue()));

        TableColumn<Panel, String> labelCol = new TableColumn<>("Label");
        labelCol.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        labelCol.setCellFactory(TextFieldTableCell.forTableColumn());
        labelCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setLabel(event.getNewValue()));
        panelsTable.getColumns().addAll(lengthCol, widthCol, qtyCol, labelCol);
        panelsTable.setEditable(true); // Enable editing for the table

        // Stock Sheets Table (with editable cells)
        TableView<StockSheet> stockTable = new TableView<>(stockSheets);
        TableColumn<StockSheet, Double> stockLengthCol = new TableColumn<>("Length");
        stockLengthCol.setCellValueFactory(cellData -> cellData.getValue().lengthProperty().asObject());
        stockLengthCol.setCellFactory(TextFieldTableCell.forTableColumn(customDoubleConverter));
        stockLengthCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setLength(event.getNewValue()));

        TableColumn<StockSheet, Double> stockWidthCol = new TableColumn<>("Width");
        stockWidthCol.setCellValueFactory(cellData -> cellData.getValue().widthProperty().asObject());
        stockWidthCol.setCellFactory(TextFieldTableCell.forTableColumn(customDoubleConverter));
        stockWidthCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setWidth(event.getNewValue()));

        TableColumn<StockSheet, Integer> stockQtyCol = new TableColumn<>("Qty");
        stockQtyCol.setCellValueFactory(cellData -> cellData.getValue().quantityProperty().asObject());
        stockQtyCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        stockQtyCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setQuantity(event.getNewValue()));

        TableColumn<StockSheet, String> stockLabelCol = new TableColumn<>("Label");
        stockLabelCol.setCellValueFactory(cellData -> cellData.getValue().labelProperty());
        stockLabelCol.setCellFactory(TextFieldTableCell.forTableColumn());
        stockLabelCol.setOnEditCommit(event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setLabel(event.getNewValue()));
        stockTable.getColumns().addAll(stockLengthCol, stockWidthCol, stockQtyCol, stockLabelCol);
        stockTable.setEditable(true); // Enable editing for the table

        // Buttons for Panels Table
        Button addPanelButton = new Button("Add Panel");
        addPanelButton.setOnAction(e -> {
            panels.add(new Panel(0, 0, 1, "")); // Default Length and Width as integers (0)
        });
        Button removePanelButton = new Button("Remove Panel");
        removePanelButton.setOnAction(e -> {
            Panel selected = panelsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                panels.remove(selected);
            }
        });

        // Buttons for Stock Sheets Table
        Button addSheetButton = new Button("Add Sheet");
        addSheetButton.setOnAction(e -> {
            stockSheets.add(new StockSheet(0, 0, 1, "")); // Default Length and Width as integers (0)
        });
        Button removeSheetButton = new Button("Remove Sheet");
        removeSheetButton.setOnAction(e -> {
            StockSheet selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                stockSheets.remove(selected);
            }
        });

        // Options Panel (Simplified with only Cut Thickness)
        Label cutThicknessLabel = new Label("Cut Thickness");
        thicknessInput = new TextField("0"); // Default to "0", editable and stored for optimization

        // Calculate Button
        Button calculateButton = new Button("Calculate");
        calculateButton.setOnAction(e -> optimizeAndDisplay(panelsTable, stockTable));

        // Output Area (Canvas for cutting pattern and Statistics)
        canvas = new Canvas(600, 400); // Revert to a fixed size to ensure visibility
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight()); // Clear canvas with white

        statistics = new TextArea();
        statistics.setEditable(false);
        statistics.setPrefWidth(600); // Default width, but it will adjust

        // Layout for buttons
        HBox panelsButtons = new HBox(10, addPanelButton, removePanelButton);
        HBox sheetsButtons = new HBox(10, addSheetButton, removeSheetButton);

        // Left Side (Input Elements)
        HBox cutThicknessBox = new HBox(10, cutThicknessLabel, thicknessInput); // TextField beside Label
        VBox leftSide = new VBox(15, new Label("Panels"), panelsButtons, panelsTable,
                new Label("Stock Sheets"), sheetsButtons, stockTable,
                new VBox(15, cutThicknessBox),
                calculateButton);

        // Right Side (Output Elements)
        VBox rightSide = new VBox(15);
        rightSide.getChildren().addAll(new Label("Cutting Pattern"), canvas);
        VBox.setVgrow(canvas, Priority.ALWAYS); // Ensure canvas fills vertical space
        rightSide.getChildren().addAll(new Label("Statistics"), statistics);

        // Split layout into left and right using HBox with padding
        HBox root = new HBox(15, leftSide, rightSide);
        root.setFillHeight(true); // Allow right side to fill vertical space
        root.setPadding(new javafx.geometry.Insets(20, 20, 20, 20)); // Add padding around the entire window (20 pixels on all sides)
        VBox.setVgrow(rightSide, Priority.ALWAYS); // Ensure right side stretches vertically

        Scene scene = new Scene(root);
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: styles.css not found in resources. Continuing without styling.");
        }

        // Set window to open maximized
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void optimizeAndDisplay(TableView<Panel> panelsTable, TableView<StockSheet> stockTable) {
        System.out.println("Optimizing cutting pattern...");

        // Get Cut Thickness
        double cutThickness = 0.0;
        try {
            cutThickness = Double.parseDouble(thicknessInput.getText().trim());
        } catch (NumberFormatException e) {
            System.err.println("Invalid Cut Thickness. Using 0.0.");
        }

        // Collect panels and stock sheets
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

        // Sort panels by area (descending) for FFDH/BFDH
        panelsToOptimize.sort((p1, p2) -> Double.compare(p2.getLength() * p2.getWidth(), p1.getLength() * p1.getWidth()));

        // Clear and prepare the canvas
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        double usedArea = 0.0;
        double totalArea = 0.0;
        int totalCuts = 0;

        // Draw each stock sheet outline and place panels
        double yOffset = 0; // Track vertical position for multiple sheets
        for (StockSheet sheet : availableSheets) {
            double sheetLength = sheet.getLength();
            double sheetWidth = sheet.getWidth();

            // Draw stock sheet outline (e.g., in gray)
            gc.setStroke(javafx.scene.paint.Color.GRAY);
            gc.setLineWidth(2); // Thicker line for visibility
            gc.strokeRect(0, yOffset, sheetLength, sheetWidth);

            double x = 0, y = yOffset;

            for (Panel panel : panelsToOptimize) {
                double panelLength = panel.getLength() + (cutThickness * 2); // Account for cuts
                double panelWidth = panel.getWidth() + (cutThickness * 2);

                if (x + panelLength <= sheetLength && y + panelWidth <= sheetWidth + yOffset) {
                    // Place panel
                    gc.setFill(javafx.scene.paint.Color.BLUE);
                    gc.fillRect(x, y, panelLength - (cutThickness * 2), panelWidth - (cutThickness * 2)); // Draw panel
                    gc.setStroke(javafx.scene.paint.Color.BLACK);
                    gc.strokeRect(x, y, panelLength - (cutThickness * 2), panelWidth - (cutThickness * 2)); // Outline
                    gc.setFill(javafx.scene.paint.Color.BLACK);
                    gc.fillText(panel.getLabel(), x + 5, y + 15); // Label (if needed)

                    usedArea += panel.getLength() * panel.getWidth();
                    x += panelLength; // Move right for next panel
                    totalCuts += 2; // Assume 2 cuts per panel (horizontal and vertical)

                    if (x >= sheetLength) {
                        x = 0;
                        y += panelWidth;
                    }
                } else {
                    // Skip or handle overflow (simplified for now)
                    System.out.println("Panel too large or no space: " + panel.getLength() + "x" + panel.getWidth());
                }
            }

            totalArea += sheetLength * sheetWidth;
            yOffset += sheetWidth + 20; // Add spacing between sheets
        }

        // Calculate statistics
        double wastePercentage = (totalArea - usedArea) / totalArea * 100;
        String stats = String.format("Used area: %.1f%%\nWasted area: %.1f%%\nTotal cuts: %d",
                (usedArea / totalArea) * 100, wastePercentage, totalCuts);
        statistics.setText(stats);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

// Simple data classes for Panels and Stock Sheets (unchanged, but note default values are integers)
class Panel {
    private double length, width;
    private int quantity;
    private String label;

    public Panel(double length, double width, int quantity, String label) {
        this.length = length;
        this.width = width;
        this.quantity = quantity;
        this.label = label;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
    public int getQuantity() { return quantity; }
    public String getLabel() { return label; }

    public void setLength(double length) { this.length = length; }
    public void setWidth(double width) { this.width = width; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setLabel(String label) { this.label = label; }

    // JavaFX properties for TableView
    public javafx.beans.property.DoubleProperty lengthProperty() {
        return new javafx.beans.property.SimpleDoubleProperty(length);
    }
    public javafx.beans.property.DoubleProperty widthProperty() {
        return new javafx.beans.property.SimpleDoubleProperty(width);
    }
    public javafx.beans.property.IntegerProperty quantityProperty() {
        return new javafx.beans.property.SimpleIntegerProperty(quantity);
    }
    public javafx.beans.property.StringProperty labelProperty() {
        return new javafx.beans.property.SimpleStringProperty(label);
    }
}

class StockSheet {
    private double length, width;
    private int quantity;
    private String label;

    public StockSheet(double length, double width, int quantity, String label) {
        this.length = length;
        this.width = width;
        this.quantity = quantity;
        this.label = label;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
    public int getQuantity() { return quantity; }
    public String getLabel() { return label; }

    public void setLength(double length) { this.length = length; }
    public void setWidth(double width) { this.width = width; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setLabel(String label) { this.label = label; }

    // JavaFX properties for TableView
    public javafx.beans.property.DoubleProperty lengthProperty() {
        return new javafx.beans.property.SimpleDoubleProperty(length);
    }
    public javafx.beans.property.DoubleProperty widthProperty() {
        return new javafx.beans.property.SimpleDoubleProperty(width);
    }
    public javafx.beans.property.IntegerProperty quantityProperty() {
        return new javafx.beans.property.SimpleIntegerProperty(quantity);
    }
    public javafx.beans.property.StringProperty labelProperty() {
        return new javafx.beans.property.SimpleStringProperty(label);
    }
}