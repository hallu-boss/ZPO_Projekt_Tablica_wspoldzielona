package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;

import java.io.Serializable;

public class ChangePrint implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ShapeToDraw {LINE, RECT, TRIANGLE, CIRCLE, ERASER}

    public final double startX, startY;
    public final double endX, endY;
    public final ColorPicker colorShape;    // kolor kształtu
    public final  Slider thicknessSlider;   // grubość pędzla

    public final ShapeToDraw shapeToDraw;


    public ChangePrint(double startX, double startY, double endX, double endY, ColorPicker colorShape, Slider thicknessSlider, ShapeToDraw shapeToDraw) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.colorShape = colorShape;
        this.thicknessSlider = thicknessSlider;
        this.shapeToDraw = shapeToDraw;
    }
}
