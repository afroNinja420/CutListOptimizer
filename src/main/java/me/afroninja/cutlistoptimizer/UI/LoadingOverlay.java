package me.afroninja.cutlistoptimizer.UI;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class LoadingOverlay {
    private final Pane overlayPane;
    private final ProgressBar progressBar;
    private final Label progressLabel;
    private final Button stopButton;
    private final VBox content;

    public LoadingOverlay(Pane parentPane) {
        overlayPane = new Pane();
        overlayPane.setPrefSize(parentPane.getPrefWidth(), parentPane.getPrefHeight());
        overlayPane.setVisible(false);

        progressBar = new ProgressBar();
        progressBar.setPrefWidth(200);

        progressLabel = new Label("Processing...");
        stopButton = new Button("Stop");

        content = new VBox(10, progressBar, progressLabel, stopButton);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        overlayPane.getChildren().add(content);

        content.layoutXProperty().bind(overlayPane.widthProperty().subtract(content.widthProperty()).divide(2));
        content.layoutYProperty().bind(overlayPane.heightProperty().subtract(content.heightProperty()).divide(2));

        parentPane.getChildren().add(overlayPane);
    }

    public void showLoadingOverlay() {
        overlayPane.setVisible(true);
    }

    public void hideLoadingOverlay() {
        overlayPane.setVisible(false);
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getProgressLabel() {
        return progressLabel;
    }

    public Button getStopButton() {
        return stopButton;
    }
}