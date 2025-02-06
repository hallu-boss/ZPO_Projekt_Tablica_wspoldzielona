package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.io.*;
import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.List;

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
    private final GraphicsContext  mainGraphicsContext = mainCanvas.getGraphicsContext2D();;

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
        private final ObjectOutputStream out;


        public ClientHandlerReceving(Socket socket)  {
            this.socket = socket;
            try {
                 this.out = new  ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void sendObjectToClient(ChangePrint changePrint) throws IOException {

            synchronized (clients) {
                out.writeObject(changePrint);
            }
        }

        @Override
        public void run() {
            // dwa wątki na wysyłanie i odbiór

            try (final ObjectInputStream in = new ObjectInputStream(socket.getInputStream()) ) {

                new Thread(
                        () ->  {
                            while (running.get()) {
                                // TODO: nasłuch zmian przybyłych od danego klienta
                                try {
                                    ChangePrint changePrint = (ChangePrint) in.readObject();
                                    queueChangePrint.add(changePrint);
                                } catch (IOException | ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                ).start();

                clients.add(socket);


            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                synchronized (clients) {
                    clients.remove(socket);
                }
            }






        }

        private static void broadcastImageChange() throws InterruptedException {
            ChangePrint changePrint = queueChangePrint.take();
            synchronized (clients) {
                for (Socket client : clients) {

                }
            }
        }

    }
}
