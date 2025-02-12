package com.example.zpo_projekt_tablica_wspoldzielona;


import javafx.scene.paint.Color;

import java.io.Serializable;

public class ChangePrint implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ShapeToDraw {LINE, RECT, TRIANGLE, CIRCLE, ERASER}

    public final double startX, startY;
    public final double endX, endY;
    private final double red, green, blue, alpha; // kolor kształtu
    public final  double  thicknessSlider;   // grubość pędzla

    public final ShapeToDraw shapeToDraw;

    public ChangePrint(double startX, double startY, double endX, double endY, Color colorShape, double  thicknessSlider, ShapeToDraw shapeToDraw) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.red = colorShape.getRed();
        this.green = colorShape.getGreen();
        this.blue = colorShape.getBlue();
        this.alpha = colorShape.getOpacity();

        this.thicknessSlider = thicknessSlider;
        this.shapeToDraw = shapeToDraw;
    }

    public Color getColor() {
        return new Color(red, green, blue, alpha);
    }
}
