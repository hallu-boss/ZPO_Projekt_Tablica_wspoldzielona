package com.example.zpo_projekt_tablica_wspoldzielona;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client_SerwerComunicator {
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final int PORT = 5000;
    private Socket socket;
    private List<ChangePrint> tablica = new ArrayList<ChangePrint>();
    private final BlockingQueue<ChangePrint> queueChangePrint = new LinkedBlockingQueue<>();

    private ObjectInputStream in;
    private ObjectOutputStream out;

    ChangePrint getModification() {
        return queueChangePrint.poll();
    }


    private void loadChangeFromSerwer() {
        if( !socket.isClosed()) {
            try {
                ChangePrint changePrint = (ChangePrint) in.readObject();
                queueChangePrint.add(changePrint);
            }
            catch (SocketTimeoutException e) {
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public Client_SerwerComunicator(String login, String passoword) throws IOException, ClassNotFoundException {
        connectServer();
        sendClinetsData(login, passoword);
        loadImageFromServer();
        new Thread( () -> {
            while (running.get()) {
                loadChangeFromSerwer();
            }
        }).start();
    }



    private void connectServer() throws IOException{
        socket = new Socket("localhost", 5000);
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.out.flush();
        this.in = new ObjectInputStream(socket.getInputStream());
        socket.setSoTimeout(1000);
    }

    private void sendClinetsData(String login, String password) throws IOException{
        UserData userData = new UserData(password, login);
        out.writeObject(userData);
        out.flush();
    }



    private void loadImageFromServer() throws IOException, ClassNotFoundException {
       synchronized (tablica) {
           tablica = (List<ChangePrint>) in.readObject();

       }
    }
    List<ChangePrint> getTablica(){return tablica;}



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
