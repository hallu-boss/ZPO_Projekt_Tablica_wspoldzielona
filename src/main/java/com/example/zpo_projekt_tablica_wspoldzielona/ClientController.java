package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The controller responsible for handling user interactions and coordinating drawing operations in the client application.
 */
public class ClientController {
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
    private final AtomicBoolean running = new AtomicBoolean(true);


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


    /**
     * Initializes the canvas, tools, and connections to the server.
     */
    @FXML
    public void initialize() {
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
                while (running.get()) {
                    ChangePrint changePrint = serwerComunicator.getModification();
                    if( changePrint != null ) {
                        chanePrintToCanvas(changePrint, mainGraphicsContext);
                    }
                }
                serwerComunicator.disconnectServer();
            }).start();

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        showLoginDialog();

        addCloseRequestHandler();
    }

    /**
     * Handles login dialog for user authentication.
     */
    private void showLoginDialog() {
        TextInputDialog loginDialog = new TextInputDialog();
        loginDialog.setTitle("Logowanie");
        loginDialog.setHeaderText("Podaj dane logowania");
        loginDialog.setContentText("Nazwa użytkownika:");

        Optional<String> usernameResult = loginDialog.showAndWait();
        if (!usernameResult.isPresent()) {
            Platform.exit();
            return;
        }
        String username = usernameResult.get();

        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Logowanie");
        passwordDialog.setHeaderText("Podaj dane logowania");
        passwordDialog.setContentText("Hasło:");

        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (!passwordResult.isPresent()) {
            Platform.exit();
            return;
        }
        String password = passwordResult.get();

        try {
            if( serwerComunicator != null) {
                serwerComunicator.disconnectServer();
            }
            serwerComunicator = new Client_SerwerComunicator(username, password);

            loadTableFromSerwer();

            new Thread(() -> {
                while (running.get()) {
                    ChangePrint changePrint = serwerComunicator.getModification();
                    if (changePrint != null) {
                        chanePrintToCanvas(changePrint, mainGraphicsContext);
                    }
                }
                serwerComunicator.disconnectServer();
            }).start();
        } catch (IOException | ClassNotFoundException e) {
            running.set(false);
            Platform.exit();
        }
    }

    /**
     * Adds a handler for the close request of the main application window.
     * This handler displays a confirmation dialog when the user tries to close the application.
     * If the user confirms, the application will shut down; otherwise, the close request is canceled.
     */
    private void addCloseRequestHandler() {
        Platform.runLater(() -> {
            Stage stage = (Stage) mainCanvas.getScene().getWindow();
            stage.setOnCloseRequest((WindowEvent event) -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Zamykanie aplikacji");
                alert.setHeaderText("Czy na pewno chcesz zamknąć aplikację?");
                Optional<ButtonType> result = alert.showAndWait();
                if ( result.get() != ButtonType.OK) {
                    event.consume();  // Anuluj zamknięcie
                    return;
                }

                running.set(false);
                if (serwerComunicator != null) {
                    serwerComunicator.disconnectServer();
                }
                System.out.println("Aplikacja zostanie zamknięta.");
            });
        });
    }

    /**
     * Loads the current drawing from the server and updates the canvas.
     */
    private void loadTableFromSerwer() {
        List<ChangePrint> tablica = serwerComunicator.getTablica();
        for(ChangePrint element : tablica) {
            chanePrintToCanvas(element, mainGraphicsContext);
        }
    }

    /**
     * Starts drawing when the mouse is pressed.
     *
     * @param event The mouse event that triggers the drawing action.
     */
    private void startDrawing(MouseEvent event) {
        startX = event.getX();
        startY = event.getY();
        isDrawing = true;

        tempGraphicsContext.setStroke(colorPicker.getValue());
        tempGraphicsContext.setLineWidth(thicknessSlider.getValue());
    }

    /**
     * Draws a temporary shape while dragging the mouse.
     *
     * @param event The mouse event containing current mouse position.
     */
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

    /**
     * Finalizes the drawing after the mouse is released.
     *
     * @param event The mouse event that finalizes the drawing.
     */
    private void finishDrawing(MouseEvent event) {
        double endX = event.getX();
        double endY = event.getY();

        mainGraphicsContext.setStroke(colorPicker.getValue());
        mainGraphicsContext.setLineWidth(thicknessSlider.getValue());

        finishHandler(endX, endY);
        tempGraphicsContext.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        tempGraphicsContext.clearRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        isDrawing = false;
    }

    /**
     * Draws a simple shape (line, rectangle, triangle, or circle) on the given GraphicsContext.
     * The shape to be drawn is determined based on the selected tool (line, rectangle, triangle, circle).
     * If the eraser tool is selected, it erases content on the canvas instead.
     *
     * @param gc      The GraphicsContext on which the shape will be drawn.
     * @param startX  The starting X coordinate of the shape.
     * @param startY  The starting Y coordinate of the shape.
     * @param endX    The ending X coordinate of the shape.
     * @param endY    The ending Y coordinate of the shape.
     */
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

    /**
     * Determines the currently selected drawing tool and returns the corresponding shape type.
     * The method checks which tool (line, rectangle, triangle, circle, or eraser) is selected,
     * and returns the appropriate enum value representing the shape to be drawn.
     *
     * @return The current shape type, represented as a value from the {@link ChangePrint.ShapeToDraw} enum.
     */
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

    /**
     * Finalizes the drawing operation by sending the drawing change to the server and updating the canvas.
     * After the drawing is completed, the method creates a {@link ChangePrint} object representing the drawing
     * and sends it to the server for broadcasting to other clients. It also updates the main canvas with the new drawing.
     *
     * @param endX The ending X coordinate of the drawn shape.
     * @param endY The ending Y coordinate of the drawn shape.
     */
    private void finishHandler(double endX, double endY) {
        ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );
        serwerComunicator.sedChangeToServer(changePrint);
        drawSimpleShape(mainGraphicsContext, startX, startY, endX, endY);
    }

    /**
     * Converts a {@link ChangePrint} object into a visual drawing on the given {@link GraphicsContext}.
     * This method takes the drawing change (represented by the {@link ChangePrint} object) and renders it
     * onto the canvas using the provided {@link GraphicsContext}. The shape is drawn based on the information
     * in the {@link ChangePrint} object, such as coordinates, color, thickness, and shape type.
     *
     * @param changePrint The {@link ChangePrint} object containing drawing data (coordinates, color, shape type).
     * @param gc The {@link GraphicsContext} used to render the shape onto the canvas.
     */
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

    /**
     * Erases content from the canvas at the specified coordinates using the eraser tool.
     * This method creates a {@link ChangePrint} object for the erase operation and sends it to the server.
     * It also performs the actual erasure on the main canvas at the given coordinates with the current eraser size.
     *
     * @param endX The X coordinate where the erasure will occur.
     * @param endY The Y coordinate where the erasure will occur.
     */
    private void fullErase(double endX, double endY) {
        ChangePrint changePrint = new ChangePrint(startX, startY, endX, endY,
                colorPicker.getValue(), thicknessSlider.getValue(), currentShape() );

        serwerComunicator.sedChangeToServer(changePrint);
        DrawShape.erase(mainGraphicsContext, endX, endY, thicknessSlider.getValue() );
    }

    /**
     * Handles the drawing or erasing operation while dragging the mouse on the canvas.
     * Depending on the selected tool, this method either draws the selected shape or erases content.
     * The method updates the temporary canvas to display the drawing action in real-time while dragging.
     *
     * @param endX The current X coordinate while dragging the mouse.
     * @param endY The current Y coordinate while dragging the mouse.
     */
    private void dragHandle(double endX, double endY) {
        drawSimpleShape(tempGraphicsContext, startX, startY, endX, endY);
        if (eraserTool.isSelected()) {
            fullErase(endX, endY);
        }
    }



}
