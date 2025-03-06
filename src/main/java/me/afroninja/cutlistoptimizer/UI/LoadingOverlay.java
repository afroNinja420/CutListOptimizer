package me.afroninja.cutlistoptimizer.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoadingOverlay {
    private Stage loadingStage;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Button stopButton;
    private Stage primaryStage;

    public LoadingOverlay(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void showLoadingOverlay() {
        loadingStage = new Stage();
        loadingStage.initOwner(primaryStage);
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.initStyle(StageStyle.UNDECORATED);
        loadingStage.setAlwaysOnTop(true);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressLabel = new Label("Searching for best solution...");
        stopButton = new Button("Click to accept and stop");

        VBox loadingBox = new VBox(10, progressBar, progressLabel, stopButton);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(20));
        loadingBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #d3d3d3; -fx-border-width: 1;");

        Scene loadingScene = new Scene(loadingBox);
        loadingStage.setScene(loadingScene);

        loadingStage.setX(primaryStage.getX() + primaryStage.getWidth() / 2 - 100);
        loadingStage.setY(primaryStage.getY() + primaryStage.getHeight() / 2 - 50);

        primaryStage.getScene().getRoot().setEffect(new javafx.scene.effect.ColorAdjust(0, 0, -0.3, 0));
        primaryStage.getScene().getRoot().setDisable(true);

        loadingStage.show();
    }

    public void hideLoadingOverlay() {
        if (loadingStage != null) {
            loadingStage.close();
        }
        primaryStage.getScene().getRoot().setEffect(null);
        primaryStage.getScene().getRoot().setDisable(false);
    }

    public ProgressBar getProgressBar() { return progressBar; }
    public Label getProgressLabel() { return progressLabel; }
    public Button getStopButton() { return stopButton; }
}