package com.example.zpo_projekt_tablica_wspoldzielona;

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



public class Serwer  {
    static AtomicBoolean running = new AtomicBoolean(true);

    private static final int PORT = 5000;


    static final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();
    static final List<ClientHandlerReceving> clients = new ArrayList<>();

    private static List<ChangePrint> changePrints = new ArrayList<>();
    private static List<ChangePrint> tablica;
    private final static String filePath = "tablica.ser";

    public static void main(String[] args) {

        // tworzenie obiektu do rysowania TODO: zastąpić pobieraniem obrazu z bazy danych
        try {
            tablica = loadListFromFile(filePath);
            System.out.println("Tablica - załadowana do pamięci ");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error: Inicjaliacja tablicy się nie powiodła");
            throw new RuntimeException(e);
        }

        try {
            // część sterowania serwera

            new Thread(() -> {
                System.out.println("Wpisuj komendy by sterować serwerem:");
                BufferedReader scan = new BufferedReader(new InputStreamReader(System.in));
                String comand;
                while (running.get()) {
                    try {
                        comand = scan.readLine();

                        if( comand.equalsIgnoreCase("exit")) {
                            running.set(false);
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
            serverSocket.setSoTimeout(5000);
            System.out.println("[+] Serwer nasłuchuje na porcie: " + PORT);

            new Thread(() -> {
                ChangePrint changeToSend;
                while (running.get()) {
                    synchronized (queueChangePrint) {
                        changeToSend = queueChangePrint.poll();
                    }
                    if (changeToSend != null) {
                        for(ClientHandlerReceving client: clients) {
                            try {
                                synchronized (clients) {
                                    client.sendObjectToClient(changeToSend);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        tablica.add(changeToSend);
                    }
                }
            }).start();

            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    ClientHandlerReceving clientHandlerReceving = new ClientHandlerReceving(clientSocket);
//                TODO: if( !ObslugaBazyDanych.sprawdzUzytkownika(clientHandlerReceving.getLogin(),
//                        clientHandlerReceving.getPassword())) {
//                    continue;
//                }
                    clientHandlerReceving.sendTableToClient();


                    new Thread(clientHandlerReceving).start();
                    synchronized (clients) {
                        clients.add(clientHandlerReceving);
                        System.out.println("Nowy Klient: ");
                    }
                }
                catch (SocketTimeoutException e) {
                    continue;
                }
            }

            for (ClientHandlerReceving client : clients) {
                client.getSocket().close();
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            saveListToFile(tablica, filePath);
            System.out.println("Zakończenie pracy pomyślnie");
        } catch (IOException e) {
            throw new RuntimeException(e);
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


    private static class ClientHandlerReceving implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream  out;
        final ObjectInputStream in;
        final public String login, password;

        public ClientHandlerReceving(Socket socket) {
            this.socket = socket;
            try {
                this.out = new  ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());

                 UserData userData = (UserData) in.readObject();

                this.login = userData.login;
                this.password = userData.passowrd;

                System.out.println("Użytkownik : " + login + " password: " + password);

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }


        private void sendTableToClient() throws IOException {
            synchronized (tablica) {
                out.writeObject(tablica);
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
            try {
                while (running.get()) {
                    ChangePrint changePrint = (ChangePrint) in.readObject();
//                    System.out.println("changePrint - otrzymany ");
                    synchronized (queueChangePrint) {
                        queueChangePrint.add(changePrint);
                    }
                }
            } catch (IOException e) {
                System.out.println("Klient rozłączył się: " + socket);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    this.socket.close(); // Zamknięcie gniazda
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clients) {
                    clients.remove(this);
                }
            }
        }


    }
}
