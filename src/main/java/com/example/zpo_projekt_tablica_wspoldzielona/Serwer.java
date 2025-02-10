package com.example.zpo_projekt_tablica_wspoldzielona;

import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class Serwer {
    static AtomicBoolean running = new AtomicBoolean(true);

    private static final int PORT = 5000;

    static final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();
    static final List<ClientHandlerReceving> clients = new ArrayList<>();

    @FXML
    static private Canvas mainCanvas = new Canvas(640, 480);
    static private GraphicsContext mainGraphicsContext;

    public static void main(String[] args) {
        mainGraphicsContext = mainCanvas.getGraphicsContext2D();
        loadWork();

        // tworzenie obiektu do rysowania TODO: zastąpić pobieraniem obrazu z bazy danych
        Serwer sw = new Serwer();
         final GraphicsContext  mainGraphicsContext = sw.mainCanvas.getGraphicsContext2D();

        try {
            // część sterowania serwera

            new Thread(() -> {
                System.out.println("Wpisuj komendy by sterować serwerem:");
                BufferedReader scan = new BufferedReader(new InputStreamReader(System.in));
                String comand;
                while (running.get()) {
                    try {
                        System.out.print("> ");
                        comand = scan.readLine();

                        if( comand.equalsIgnoreCase("exit")) {
                            running.set(false);
                            saveWork();
                            System.out.println("Konczę prace serwera");
                        }
                        else if( comand.equalsIgnoreCase("show")) {
                            System.out.println("Serwera połączony z " + clients.size() + " użytkownikami");
                            for (ClientHandlerReceving client : clients) {
                                System.out.println("\t " + client.getSocket().toString());
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

            // Część klientów

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
                        for(ClientHandlerReceving client: clients) {
                            try {
                                synchronized (clients) {
                                    client.sendObjectToClient(changToSend);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }).start();

            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandlerReceving clientHandlerReceving = new ClientHandlerReceving(clientSocket, mainGraphicsContext);
                new Thread(clientHandlerReceving).start();
                synchronized (clients) {
                    clients.add(clientHandlerReceving);
                    System.out.println("Nowy Klient: " );
                }
            }

            for (ClientHandlerReceving client : clients) {
                client.getSocket().close();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveWork() {
        WritableImage writableImage = new WritableImage((int) mainCanvas.getWidth(), (int) mainCanvas.getHeight());
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);  // lub inny kolor, jeśli potrzebujesz
        mainCanvas.snapshot(params, writableImage);

        int width = (int) writableImage.getWidth();
        int height = (int) writableImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = writableImage.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Pobieramy wartość ARGB dla danego piksela i ustawiamy ją w BufferedImage
                int argb = pixelReader.getArgb(x, y);
                bufferedImage.setRGB(x, y, argb);
            }
        }


        File outputFile = new File("zapisanyObraz.png");
        try {
            ImageIO.write(bufferedImage, "png", outputFile);
            System.out.println("Obraz został zapisany pomyślnie!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadWork() {
        File file = new File("zapisanyObraz.png");

        if (file.exists()) {
            Image image = new Image(file.toURI().toString());
            mainGraphicsContext.drawImage(image, 0, 0);
        } else {
            System.out.println("Plik obrazu nie został znaleziony!");
        }
    }

    private static class ClientHandlerReceving implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream out;


        public ClientHandlerReceving(Socket socket, GraphicsContext  initGraphicsContext)  {
            this.socket = socket;
            try {
                 this.out = new  ObjectOutputStream(socket.getOutputStream());
                 initPrintClient(initGraphicsContext);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void initPrintClient( GraphicsContext  initGraphicsContext) throws IOException {
            synchronized (clients) {
                //TODO: wysłanie obrazu z bazy danych do klienta
                out.writeObject(initGraphicsContext);
                out.flush();
            }
        }

        protected void sendObjectToClient(ChangePrint changePrint) throws IOException {
            synchronized (clients) {
                out.writeObject(changePrint);
                out.flush();
            }
        }

        public Socket getSocket() {return socket;}

        @Override
        public void run() {
            // dwa wątki na wysyłanie i odbiór
            try (final ObjectInputStream in = new ObjectInputStream(socket.getInputStream()) ) {

                new Thread(
                        () ->  {
                            while (running.get()) {
                                try {
                                    ChangePrint changePrint = (ChangePrint) in.readObject();
                                    queueChangePrint.add(changePrint);
                                } catch (IOException | ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                ).start();

                this.out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }

    }
}
