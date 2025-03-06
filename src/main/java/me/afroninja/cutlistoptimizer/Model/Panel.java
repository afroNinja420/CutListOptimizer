package me.afroninja.cutlistoptimizer.Model;

import javafx.beans.property.*;

public class Panel {
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

    public DoubleProperty lengthProperty() {
        return new SimpleDoubleProperty(length);
    }
    public DoubleProperty widthProperty() {
        return new SimpleDoubleProperty(width);
    }
    public IntegerProperty quantityProperty() {
        return new SimpleIntegerProperty(quantity);
    }
    public StringProperty labelProperty() {
        return new SimpleStringProperty(label);
    }
}