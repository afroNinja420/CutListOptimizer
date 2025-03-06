package me.afroninja.cutlistoptimizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;

import me.afroninja.cutlistoptimizer.Model.Panel;
import me.afroninja.cutlistoptimizer.Model.StockSheet;
import me.afroninja.cutlistoptimizer.Model.OptimizationResult;
import me.afroninja.cutlistoptimizer.UI.LoadingOverlay;
import me.afroninja.cutlistoptimizer.UI.StatisticsUpdater;

public class Main extends Application {

    private ObservableList<Panel> panels = FXCollections.observableArrayList();
    private ObservableList<StockSheet> stockSheets = FXCollections.observableArrayList();
    private Canvas canvas;
    private TextArea statistics;
    private TextField thicknessInput;
    private int currentSheetIndex = 0;

    private Stage primaryStage;
    private LoadingOverlay loadingOverlay;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        loadingOverlay = new LoadingOverlay(primaryStage);

        primaryStage.setTitle("CutList Optimizer");

        // Custom DoubleStringConverter to display "0" instead of "0.0" by default
        DoubleStringConverter customDoubleConverter = new DoubleStringConverter() {
            @Override
            public String toString(Double value) {
                if (value == null || value == 0.0) return "0";
                return String.format("%.1f", value);
            }
        };

        // Panels Table
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
        panelsTable.setEditable(true);

        // Stock Sheets Table
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
        stockTable.setEditable(true);

        // Buttons for Panels Table
        Button addPanelButton = new Button("Add Panel");
        addPanelButton.setOnAction(e -> panels.add(new Panel(0, 0, 1, "")));
        Button removePanelButton = new Button("Remove Panel");
        removePanelButton.setOnAction(e -> {
            Panel selected = panelsTable.getSelectionModel().getSelectedItem();
            if (selected != null) panels.remove(selected);
        });

        // Buttons for Stock Sheets Table
        Button addSheetButton = new Button("Add Sheet");
        addSheetButton.setOnAction(e -> stockSheets.add(new StockSheet(0, 0, 1, "")));
        Button removeSheetButton = new Button("Remove Sheet");
        removeSheetButton.setOnAction(e -> {
            StockSheet selected = stockTable.getSelectionModel().getSelectedItem();
            if (selected != null) stockSheets.remove(selected);
        });

        // Options Panel
        Label cutThicknessLabel = new Label("Cut Thickness");
        thicknessInput = new TextField("0.13");
        HBox cutThicknessBox = new HBox(10, cutThicknessLabel, thicknessInput);

        // Calculate Button
        Button calculateButton = new Button("Calculate");
        calculateButton.setOnAction(e -> optimizeAndDisplay(panelsTable, stockTable));

        // Output Area
        canvas = new Canvas();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        statistics = new TextArea();
        statistics.setEditable(false);

        // Layout for buttons
        HBox panelsButtons = new HBox(10, addPanelButton, removePanelButton);
        HBox sheetsButtons = new HBox(10, addSheetButton, removeSheetButton);

        // Left Side
        VBox leftSide = new VBox(15, new Label("Panels"), panelsButtons, panelsTable,
                new Label("Stock Sheets"), sheetsButtons, stockTable,
                new VBox(15, cutThicknessBox),
                calculateButton);
        leftSide.setPrefWidth(300);

        // Center Side
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setContent(canvas);
        VBox centerSide = new VBox(15);
        centerSide.getChildren().addAll(new Label("Cutting Pattern"), scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        canvas.widthProperty().bind(centerSide.widthProperty());
        canvas.heightProperty().bind(centerSide.heightProperty().subtract(50));

        // Right Side
        VBox rightSide = new VBox(15);
        HBox sheetNav = new HBox(5);
        Button prevSheetButton = new Button("<");
        Button nextSheetButton = new Button(">");
        prevSheetButton.setOnAction(e -> {
            if (currentSheetIndex > 0) {
                currentSheetIndex--;
                StatisticsUpdater.updateStatistics(statistics, usedSheets, unplacedPanels, currentSheetIndex, panels.size(), usedArea, totalArea, totalCuts);
            }
        });
        nextSheetButton.setOnAction(e -> {
            if (currentSheetIndex < usedSheets.size() - 1) {
                currentSheetIndex++;
                StatisticsUpdater.updateStatistics(statistics, usedSheets, unplacedPanels, currentSheetIndex, panels.size(), usedArea, totalArea, totalCuts);
            }
        });
        sheetNav.getChildren().addAll(prevSheetButton, nextSheetButton);
        rightSide.getChildren().addAll(new Label("Global Statistics"), new Separator(),
                new Label("Sheet Statistics"), sheetNav, new Separator(),
                new Label("Cuts"), new Separator());
        VBox.setVgrow(statistics, Priority.ALWAYS);
        rightSide.getChildren().add(statistics);
        rightSide.setPrefWidth(300);

        // Main Layout
        HBox root = new HBox(15, leftSide, centerSide, rightSide);
        root.setFillHeight(true);
        root.setPadding(new javafx.geometry.Insets(20, 20, 20, 20));
        VBox.setVgrow(centerSide, Priority.ALWAYS);
        VBox.setVgrow(rightSide, Priority.ALWAYS);
        HBox.setHgrow(centerSide, Priority.ALWAYS);

        Scene scene = new Scene(root);
        URL cssUrl = getClass().getResource("/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: styles.css not found in resources. Continuing without styling.");
        }

        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private List<StockSheet> usedSheets = new ArrayList<>();
    private List<Panel> unplacedPanels = new ArrayList<>();
    private Map<String, OptimizationResult> bestResult = new HashMap<>();
    private double usedArea = 0.0;
    private double totalArea = 0.0;
    private int totalCuts = 0;

    private void optimizeAndDisplay(TableView<Panel> panelsTable, TableView<StockSheet> stockTable) {
        loadingOverlay.showLoadingOverlay();
        Optimizer.optimize(panels, stockSheets, thicknessInput, canvas, statistics, usedSheets, unplacedPanels, bestResult,
                usedArea, totalArea, totalCuts, currentSheetIndex, loadingOverlay, this::updateStatistics);
    }

    private void updateStatistics() {
        StatisticsUpdater.updateStatistics(statistics, usedSheets, unplacedPanels, currentSheetIndex, panels.size(), usedArea, totalArea, totalCuts);
    }

    public static void main(String[] args) {
        launch(args);
    }
}