package com.example.zpo_projekt_tablica_wspoldzielona;

import java.io.Serializable;

/**
 * Represents a user's login credentials for authentication purposes.
 */
public class UserData implements Serializable {
    private static final long serialVersionUID = 1L;
    public final String passowrd, login;

    /**
     * Constructor for initializing user credentials.
     *
     * @param passowrd The user's password.
     * @param login The user's login identifier.
     */
    UserData(String passowrd, String login) {
        this.passowrd = passowrd;
        this.login = login;
    }
}
