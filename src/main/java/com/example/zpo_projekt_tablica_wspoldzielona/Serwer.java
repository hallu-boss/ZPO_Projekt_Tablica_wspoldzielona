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

    static final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();
    static final List<ClientHandlerReceving> queueClientHandlerReceving = new ArrayList<>();

    // tworzenie obiektu do rysowania TODO: zastąpić pobieraniem z bazy danych
    private Canvas mainCanvas;
    private final GraphicsContext  mainGraphicsContext = mainCanvas.getGraphicsContext2D();;

    public static void main(String[] args) {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("[+] Serwer nasłuchuje na porcie: " + PORT);

            new Thread(() -> {
                ChangePrint changToSend;
                System.out.println("Start wątku odpowiadającego klientom");
                while (running.get()) {
                    synchronized (queueChangePrint) {
                        changToSend = queueChangePrint.poll();
                    }
                    if (changToSend != null) {
                        for(ClientHandlerReceving client: queueClientHandlerReceving) {
                            try {
                                client.sendObjectToClient(changToSend);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).start();

            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                synchronized (clients) {
                    clients.add(clientSocket);
                    System.out.println("Nowy Klient: " );
                }
                ClientHandlerReceving clientHandlerReceving = new ClientHandlerReceving(clientSocket);
                new Thread(clientHandlerReceving).start();
                queueClientHandlerReceving.add(clientHandlerReceving);
            }

            for (Socket client : clients) {
                client.close();
            }
            serverSocket.close();
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

        protected void sendObjectToClient(ChangePrint changePrint) throws IOException {
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

                this.out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                synchronized (clients) {
                    clients.remove(socket);
                }
            }
        }

    }
}
