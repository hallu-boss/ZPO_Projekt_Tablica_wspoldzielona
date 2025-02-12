package com.example.zpo_projekt_tablica_wspoldzielona;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles communication between the client and server, including sending and receiving drawing data.
 */
public class Client_SerwerComunicator {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final int PORT = 5000;
    private Socket socket;
    private List<ChangePrint> tablica = new ArrayList<ChangePrint>();
    private final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * Retrieves the latest drawing change from the server.
     *
     * @return A ChangePrint object representing the drawing modification, or null if none is available.
     */
    ChangePrint getModification() {
        return queueChangePrint.poll();
    }

    /**
     * Loads a new drawing change from the server and adds it to the queue of changes.
     * This method reads a {@link ChangePrint} object from the server input stream,
     * adds it to the queue of drawing changes to be processed, and handles any exceptions
     * that may occur during the data retrieval process.
     */
    private void loadChangeFromSerwer() {
        if( !socket.isClosed()) {
            try {
                ChangePrint changePrint = (ChangePrint) in.readObject();
                queueChangePrint.add(changePrint);
            }
            catch (SocketTimeoutException e) {
            }
            catch (SocketException e) {

            }
            catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    /**
     * Initializes a connection to the server and authenticates the client using the provided login credentials.
     * This constructor establishes a socket connection to the server, sends the login credentials (username and password),
     * and loads the drawing data from the server. A separate thread is started to continuously receive drawing changes.
     *
     * @param login    The username used to authenticate the client.
     * @param password The password associated with the provided username.
     * @throws IOException If there is an error establishing the connection or during data transmission.
     * @throws ClassNotFoundException If there is an error deserializing the server response.
     */
    public Client_SerwerComunicator(String login, String password) throws IOException, ClassNotFoundException {
        connectServer();
        sendClinetsData(login, password);
        loadImageFromServer();
        new Thread( () -> {
            while (running.get()) {
                loadChangeFromSerwer();
            }
        }).start();
    }

    /**
     * Establishes a connection to the server by opening a socket on the specified port.
     * This method initializes input and output streams for communication with the server
     * and sets a timeout for the socket connection. It prepares the client to send and receive data from the server.
     *
     * @throws IOException If there is an error while opening the socket or initializing the streams.
     */
    private void connectServer() throws IOException{
        socket = new Socket("localhost", 5000);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        socket.setSoTimeout(1000);
    }

    /**
     * Sends the client's login credentials (username and password) to the server for authentication.
     * This method serializes the {@link UserData} object containing the login information and sends it over the network
     * to the server for validation.
     *
     * @param login    The username of the client to be authenticated.
     * @param password The password associated with the provided username.
     * @throws IOException If there is an error during the data transmission to the server.
     */
    private void sendClinetsData(String login, String password) throws IOException{
        UserData userData = new UserData(password, login);
        out.writeObject(userData);
        out.flush();
    }

    /**
     * Loads the drawing data (image) from the server.
     * This method retrieves a serialized list of {@link ChangePrint} objects from the server,
     * which represent the drawing modifications, and deserializes it into a list for use on the client side.
     *
     * @throws IOException If there is an error reading the data from the server.
     * @throws ClassNotFoundException If the class definition for {@link ChangePrint} cannot be found during deserialization.
     */
    private void loadImageFromServer() throws IOException, ClassNotFoundException {
       synchronized (tablica) {
           tablica = (List<ChangePrint>) in.readObject();

       }
    }
    List<ChangePrint> getTablica(){return tablica;}

    /**
     * Closes the connection to the server by shutting down the socket and closing the input/output streams.
     * This method ensures that all resources related to the server connection are properly released.
     */
    public void disconnectServer() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            running.set(false);
        } catch (IOException e) {
            System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
        }
        System.out.println("Rozłączono się z serwerem");
    }

    /**
     * The entry point for the client application. It attempts to establish a connection to the server using the provided
     * login credentials and then disconnects the client after the connection is established.
     * If there are any issues with the connection, an error message is displayed, and the exception is thrown.
     *
     * @param args The command-line arguments (not used in this implementation).
     */
    public static void main(String[] args) {
        try {
            Client_SerwerComunicator client = new Client_SerwerComunicator("245835", "1234");


            client.disconnectServer();
        } catch (IOException e) {
            System.out.println("Klient nie połączył się z serwerem");
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a {@link ChangePrint} object representing a drawing change to the server.
     * This method serializes the drawing change and transmits it to the server to be broadcast to other connected clients.
     *
     * @param changePrint The {@link ChangePrint} object that contains the drawing change data to be sent.
     */
    public void sedChangeToServer(ChangePrint changePrint) {
        try {
            if(!socket.isClosed()) {
                out.writeObject(changePrint);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
