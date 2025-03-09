package me.afroninja.cutlistoptimizer.Model;

public class Panel {
    private double length;
    private double width;
    private int quantity;
    private String label;

    public Panel(double length, double width, int quantity, String label) {
        this.length = length;
        this.width = width;
        this.quantity = quantity;
        this.label = label;
    }

    // Getters and setters
    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}