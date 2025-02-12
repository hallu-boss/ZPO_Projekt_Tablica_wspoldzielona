package com.example.zpo_projekt_tablica_wspoldzielona;

import java.io.Serializable;

public class UserData implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String passowrd, login;

    UserData(String passowrd, String login) {
        this.passowrd = passowrd;
        this.login = login;
    }
}
