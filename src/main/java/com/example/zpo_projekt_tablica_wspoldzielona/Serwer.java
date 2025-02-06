package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class Serwer {
    static AtomicBoolean running = new AtomicBoolean(true);
    // obsługa socet

    private static final int PORT = 5000;
    private static final List<Socket> clients = new ArrayList<>();

    static BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();

    // tworzenie obiektu do rysowania
    private Canvas mainCanvas;
    private GraphicsContext mainGraphicsContext = mainCanvas.getGraphicsContext2D();;

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[+] Serwer nasłuchuje na porcie: " + PORT);

            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                synchronized (clients) {
                    clients.add(clientSocket);
                    System.out.println("Nowy Klient: " );
                }
                new Thread(new ClientHandlerReceving(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandlerReceving implements Runnable {
        private final Socket socket;

        public ClientHandlerReceving(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {

            // dwa wątki na wysyłanie i odbiór

            new Thread(
                     () -> {
                         while(running.get()) {
                             try {
                                 broadcastImageChange(socket);
                             } catch (InterruptedException e) {
                                 throw new RuntimeException(e);
                             }
                        }
                     }
            ).start();

            new Thread(
                    () ->  {
                        while (running.get()) {
                            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                                // TODO: nasłuch zmian przybyłych od danego klienta

                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                synchronized (clients) {
                                    clients.remove(socket);
                                }
                            }
                        }
                    }
            ).start();


        }

        private static void broadcastImageChange(Socket socket) throws InterruptedException {
            ChangePrint changePrint = queueChangePrint.take();
            synchronized (clients) {
                for (Socket client : clients) {
                    try (OutputStream out = socket.getOutputStream()) {

                        // TODO: przesył zmian do wszystkich klienta

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
