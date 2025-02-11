package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;


import java.io.*;
import java.util.List;

public class ClientController {

    static final int ERASER_MUL = 3;

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

    Client_SerwerComunicator serwerComunicator;

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

       // init obrazu + łączenie z serwerem
        try {
            serwerComunicator = new Client_SerwerComunicator("245835", "1234");
            loadTableFromSerwer();

            new Thread( () -> {

                ChangePrint changePrint = serwerComunicator.getModification();
                if( changePrint != null ) {
                    chanePrintToCanvas(changePrint, mainGraphicsContext);
                }

            }).start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

    private void loadTableFromSerwer() {
        List<ChangePrint> tablica = serwerComunicator.getTablica();
        for(ChangePrint element : tablica) {
            chanePrintToCanvas(element, mainGraphicsContext);
        }
    }


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
            fullErase(endX, endY);
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
        ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );
        serwerComunicator.sedChangeToServer(changePrint);
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

    private void fullErase(double endX, double endY) {
        ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );

        serwerComunicator.sedChangeToServer(changePrint);
        DrawShape.erase(mainGraphicsContext, endX, endY, thicknessSlider.getValue() );
    }

    private void dragHandle(double endX, double endY) {
        drawSimpleShape(tempGraphicsContext, startX, startY, endX, endY);
        if (eraserTool.isSelected()) {
            fullErase(endX, endY);
        }
    }



}
