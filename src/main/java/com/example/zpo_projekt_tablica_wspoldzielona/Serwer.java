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


public class Serwer {
    // obsługa socet

    private static final int PORT = 5000;
    private static List<Socket> clients = new ArrayList<>();


    BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();



    // tworzenie obiektu do rysowania
    private Canvas mainCanvas;
    private GraphicsContext mainGraphicsContext = mainCanvas.getGraphicsContext2D();;

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[+] Serwer nasłuchuje na porcie: " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (clients) {
                    clients.add(clientSocket);
                    System.out.println("Nowy Klient: " );
                }
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream out = socket.getOutputStream()) {

                // TODO: nasłuch zmian przybyłych odo danego klienta

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (clients) {
                    clients.remove(socket);
                }
            }
        }

        private static void broadcastImage() {
            synchronized (clients) {
                for (Socket client : clients) {
                    // TODO: przesyłanie kolejnych zmian do wszystkich klientów
                }
            }
        }

    }
}
