package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.scene.canvas.GraphicsContext;

/**
 * Contains static methods to draw various shapes on a canvas.
 */
public class DrawShape {

    /**
     * Draws a line between two points.
     *
     * @param context The GraphicsContext to draw on.
     * @param x1      The starting X coordinate.
     * @param y1      The starting Y coordinate.
     * @param x2      The ending X coordinate.
     * @param y2      The ending Y coordinate.
     */
    public static void drawLine(GraphicsContext context, double x1, double y1, double x2, double y2) {
        context.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Draws a rectangle between two points.
     *
     * @param context The GraphicsContext to draw on.
     * @param x1      The starting X coordinate.
     * @param y1      The starting Y coordinate.
     * @param x2      The ending X coordinate.
     * @param y2      The ending Y coordinate.
     */
    public static void drawRect(GraphicsContext context, double x1, double y1, double x2, double y2) {
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);
        double topLeftX = Math.min(x1, x2);
        double topLeftY = Math.min(y1, y2);

        context.strokeRect(topLeftX, topLeftY, width, height);
    }

    /**
     * Draws a triangle between two points.
     *
     * @param context The GraphicsContext to draw on.
     * @param x1      The starting X coordinate.
     * @param y1      The starting Y coordinate.
     * @param x2      The ending X coordinate.
     * @param y2      The ending Y coordinate.
     */
    public static void drawTriangle(GraphicsContext context, double x1, double y1, double x2, double y2) {
        double midX = (x1 + x2) / 2;
        double midY = (y1 + y2) / 2;

        double height = Math.abs(y1 - y2);

        double x3 = x1 - (x2 - x1);

        double[] xPoints = {x1, x2, x3};
        double[] yPoints = {y1, y2, y2};

        int nPoints = 3;

        context.strokePolygon(xPoints, yPoints, nPoints);
    }

    /**
     * Draws a circle of radius between two points.
     *
     * @param context The GraphicsContext to draw on.
     * @param cx      The center X coordinate.
     * @param cy      The center Y coordinate.
     * @param ex      The edge X coordinate.
     * @param ey      The edge Y coordinate.
     */
    public static void drawCircle(GraphicsContext context, double cx, double cy, double ex, double ey) {
        double radius = Math.sqrt(Math.pow(ex - cx, 2) + Math.pow(ey - cy, 2));

        double topLeftX = cx - radius;
        double topLeftY = cy - radius;

        double width = 2 * radius;
        double height = 2 * radius;

        context.strokeOval(topLeftX, topLeftY, width, height);
    }

    /**
     * Erases a portion of the canvas at the specified coordinates with the given size.
     * This method clears a rectangular area centered around the specified coordinates,
     * with the dimensions determined by the given size. It simulates an eraser tool on the canvas.
     *
     * @param context The {@link GraphicsContext} on which the erasure will be performed.
     * @param x       The X coordinate where the erasure will begin.
     * @param y       The Y coordinate where the erasure will begin.
     * @param size    The size of the eraser, which determines the area to be cleared.
     */
    public static void erase(GraphicsContext context, double x, double y, double size) {
        double x1 = x - size / 2;
        double y1 = y - size / 2;

        context.clearRect(x1, y1, size, size);
    }
}
