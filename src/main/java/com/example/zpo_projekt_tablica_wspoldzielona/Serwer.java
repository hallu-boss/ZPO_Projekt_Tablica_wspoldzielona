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

/**
 * Handles the server-side logic, managing multiple clients and broadcasting drawing changes.
 */
public class Serwer  {
    static AtomicBoolean running = new AtomicBoolean(true);

    private static final int PORT = 5000;


    static final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();
    static final List<ClientHandlerReceving> clients = new ArrayList<>();

    private static List<ChangePrint> tablica;
    private final static String filePath = "tablica.ser";

    /**
     * Main method that runs the server, accepting client connections and handling drawing changes.
     */
    public static void main(String[] args) {
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
                                System.out.println("\t " + client.toString());
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
                    if( !ObslugaBazyDanych.sprawdzUzytkownika(clientHandlerReceving.getLogin(),
                            clientHandlerReceving.getPassword())) {
                        System.out.println("Użytkownik o danym loginie nie istnieje");
                        clientHandlerReceving.getSocket().close();
                        continue;
                    }
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

    /**
     * Saves the drawing list to a file.
     *
     * @param list The list of drawing changes to save.
     * @param filePath The file path to save the data.
     * @throws IOException If there is an error writing to the file.
     */
    private static void saveListToFile(List<ChangePrint> list, String filePath) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath));
        out.writeObject(list);
        out.close();
    }

    /**
     * Loads a list of {@link ChangePrint} objects from a file.
     * This method reads a serialized list of {@link ChangePrint} objects from the specified file path.
     * The list is deserialized and returned for use in the application.
     *
     * @param filePath The path to the file containing the serialized list of {@link ChangePrint} objects.
     * @return A list of {@link ChangePrint} objects loaded from the file.
     * @throws IOException If there is an error reading the file.
     * @throws ClassNotFoundException If the class definition for {@link ChangePrint} cannot be found during deserialization.
     */
    public static List<ChangePrint> loadListFromFile(String filePath) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath));
        List<ChangePrint> list = (List<ChangePrint>) in.readObject();
        in.close();
        return list;
    }

    /**
     * A handler class that manages communication with a client connected to the server.
     * This class is responsible for receiving and sending data between the server and a specific client,
     * handling user authentication, and processing drawing changes received from the client.
     * It implements the {@link Runnable} interface to allow multi-threaded processing of client requests.
     */
    private static class ClientHandlerReceving implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream  out;
        final ObjectInputStream in;
        final public String login, password;

        /**
         * Constructor that initializes the client handler by setting up input and output streams
         * for communication with the connected client. It also handles user authentication
         * by reading the login credentials sent by the client.
         *
         * @param socket The socket through which the server communicates with the client.
         */
        public ClientHandlerReceving(Socket socket) {
            this.socket = socket;
            try {
                this.out = new  ObjectOutputStream(socket.getOutputStream());
                this.in = new ObjectInputStream(socket.getInputStream());

                 UserData userData = (UserData) in.readObject();

                this.login = userData.login;
                this.password = userData.passowrd;

                System.out.println("Użytkownik : " + login + " password: " + password);

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Retrieves the login of the connected client.
         *
         * @return The login of the client.
         */
        public String getLogin() {return login;}

        /**
         * Retrieves the password of the connected client.
         *
         * @return The password of the client.
         */
        public String getPassword() {return password;}

        /**
         * Sends the current drawing data (the table of drawing changes) to the client.
         * This method serializes the list of drawing changes and sends it over the network to the client.
         *
         * @throws IOException If there is an error while sending the drawing data to the client.
         */
        private void sendTableToClient() throws IOException {
            synchronized (tablica) {
                out.writeObject(tablica);
                out.flush();
            }
        }

        /**
         * Sends a specific drawing change to the client.
         * This method serializes the {@link ChangePrint} object and sends it over the network to the client.
         *
         * @param changePrint The drawing change to send to the client.
         * @throws IOException If there is an error while sending the drawing change to the client.
         */
        protected void sendObjectToClient(ChangePrint changePrint) throws IOException {
            synchronized (clients) {
                out.writeObject(changePrint);
                out.flush();
            }
        }

        /**
         * Retrieves the socket associated with this client handler.
         *
         * @return The socket used for communication with the client.
         */
        public Socket getSocket() {return socket;}

        /**
         * The main loop for the client handler. Continuously receives drawing changes from the client
         * and adds them to the queue for further processing by the server.
         */
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
                System.out.println("Klient rozłączył się: " + this.toString());
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

        /**
         * Provides a string representation of the client handler, displaying the login of the connected user.
         *
         * @return A string representing the client handler, including the login of the user.
         */
        public String toString() {
            return "Użytkownik: " + login;
        }

    }
}
