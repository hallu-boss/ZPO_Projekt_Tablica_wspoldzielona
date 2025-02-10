package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClientController {

    static final int ERASER_MUL = 2;

    @FXML
    private Slider thicknessSlider;

    @FXML
    private Label thicknessValue;

    @FXML
    private Canvas mainCanvas;

    @FXML
    private Canvas tempCanvas;

    private double startX, startY; // Startowe koordynaty linii
    private boolean isDrawing = false; // Flaga trybu rysowania
    private GraphicsContext mainGraphicsContext;
    private GraphicsContext tempGraphicsContext;



    @FXML
    private ColorPicker colorPicker;

    @FXML
    private ToggleButton lineTool;
    @FXML
    private ToggleButton rectTool;
    @FXML
    private ToggleButton triangleTool;
    @FXML
    private ToggleButton circleTool;
    @FXML
    private ToggleButton eraserTool;

    @FXML
    public void initialize() {
        // Połączenie wartoście thicknessSlider z thicknessValue
        thicknessValue.textProperty().bind(
                Bindings.format("%.0f", thicknessSlider.valueProperty())
        );

        mainGraphicsContext = mainCanvas.getGraphicsContext2D();
        tempGraphicsContext = tempCanvas.getGraphicsContext2D();

        // Ustawienie odznaczania poprzedniego narzędzia
        ToggleGroup toolsGroup = new ToggleGroup();
        lineTool.setToggleGroup(toolsGroup);
        rectTool.setToggleGroup(toolsGroup);
        triangleTool.setToggleGroup(toolsGroup);
        circleTool.setToggleGroup(toolsGroup);
        eraserTool.setToggleGroup(toolsGroup);

        // Rysowanie
        tempCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, this::startDrawing);
        tempCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::drawTemporary);
        tempCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::finishDrawing);

        // inicjalizacja tablicy i obrazka
        try {
            this.tablica = loadListFromFile(filePath);
            initPrint();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Nie udało się zainicjalizować tablicy");
            throw new RuntimeException(e);
        }

        // Łączenie z serwerem


    }
    private List<ChangePrint> tablica;

    private void startDrawing(MouseEvent event) {
        startX = event.getX();
        startY = event.getY();
        isDrawing = true;

        tempGraphicsContext.setStroke(colorPicker.getValue());
        tempGraphicsContext.setLineWidth(thicknessSlider.getValue());
    }

    private void drawTemporary(MouseEvent event) {
        double endX = event.getX();
        double endY = event.getY();

        if(eraserTool.isSelected() && isDrawing) {
            ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                    colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );
            tablica.add(changePrint);
            try {
                saveListToFile(tablica, filePath);
            } catch (IOException e) {
                System.out.println("Zapis tablicy nie poszedł pomyślnie");
                throw new RuntimeException(e);
            }
            DrawShape.erase(mainGraphicsContext, endX, endY, thicknessSlider.getValue() * ERASER_MUL);
            return;
        }

        tempGraphicsContext.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());

        dragHandle(endX, endY);
    }

    private void finishDrawing(MouseEvent event) {
        double endX = event.getX();
        double endY = event.getY();

        mainGraphicsContext.setStroke(colorPicker.getValue());
        mainGraphicsContext.setLineWidth(thicknessSlider.getValue());

        finishHandler(endX, endY);

        tempGraphicsContext.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        isDrawing = false;
    }

    private void drawSimpleShape(GraphicsContext gc, double startX, double startY, double endX, double endY) {
        if (!isDrawing) {
            return;
        }
        if (lineTool.isSelected()) {
            DrawShape.drawLine(gc, startX, startY, endX, endY);
        } else if (rectTool.isSelected()) {
            DrawShape.drawRect(gc, startX, startY, endX, endY);
        } else if (triangleTool.isSelected()) {
            DrawShape.drawTriangle(gc, startX, startY, endX, endY);
        } else if (circleTool.isSelected()) {
            DrawShape.drawCircle(gc, startX, startY, endX, endY);
        }
    }

    private ChangePrint.ShapeToDraw currentShape() {
        ChangePrint.ShapeToDraw shape;

        if (lineTool.isSelected()) {
            shape = ChangePrint.ShapeToDraw.LINE;
        } else if (rectTool.isSelected()) {
            shape = ChangePrint.ShapeToDraw.RECT;
        } else if (triangleTool.isSelected()) {
            shape = ChangePrint.ShapeToDraw.TRIANGLE;
        } else if (circleTool.isSelected()) {
            shape = ChangePrint.ShapeToDraw.CIRCLE;
        }
        else {
            shape = ChangePrint.ShapeToDraw.ERASER;
        }
        return shape;
    }

    private void finishHandler(double endX, double endY) {
        // TODO: Wysyłanie na serwer modyfikacji obrazu

        ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );
        tablica.add(changePrint);
        try {
            saveListToFile(tablica, filePath);
        } catch (IOException e) {
            System.out.println("Zapis tablicy nie poszedł pomyślnie");
            throw new RuntimeException(e);
        }
        drawSimpleShape(mainGraphicsContext, startX, startY, endX, endY);
    }

    private static void chanePrintToCanvas(ChangePrint changePrint, GraphicsContext gc) {
        gc.setStroke(changePrint.getColor());
        gc.setLineWidth(changePrint.thicknessSlider);

        if( changePrint.shapeToDraw == ChangePrint.ShapeToDraw.LINE) {
            DrawShape.drawLine(gc, changePrint.startX, changePrint.startY, changePrint.endX, changePrint.endY);
        }
        else if( changePrint.shapeToDraw == ChangePrint.ShapeToDraw.RECT) {
            DrawShape.drawRect(gc, changePrint.startX, changePrint.startY, changePrint.endX, changePrint.endY);
        }
        else if (changePrint.shapeToDraw == ChangePrint.ShapeToDraw.TRIANGLE) {
            DrawShape.drawTriangle(gc, changePrint.startX, changePrint.startY, changePrint.endX, changePrint.endY);
        }
        else if( changePrint.shapeToDraw == ChangePrint.ShapeToDraw.CIRCLE) {
            DrawShape.drawCircle(gc, changePrint.startX, changePrint.startY, changePrint.endX, changePrint.endY);
        }
        else {
            DrawShape.erase(gc, changePrint.endX, changePrint.endY, changePrint.thicknessSlider);
        }
    }

    private void dragHandle(double endX, double endY) {
        drawSimpleShape(tempGraphicsContext, startX, startY, endX, endY);
        if (eraserTool.isSelected()) {
            DrawShape.erase(mainGraphicsContext, endX, endY, thicknessSlider.getValue());

        }
    }

    private static void saveListToFile(List<ChangePrint> list, String filePath) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath));
        out.writeObject(list);
        out.close();
    }
    public static List<ChangePrint> loadListFromFile(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath));
        List<ChangePrint> list = (List<ChangePrint>) in.readObject();
        in.close();
        return list;
    }
    final String filePath = "tablica.ser";
    void initPrint() {
        for(ChangePrint changePrint : tablica) {
            chanePrintToCanvas(changePrint, mainGraphicsContext);
        }
        System.out.println("Inicjalizacja obrazka udana");
    }
}
