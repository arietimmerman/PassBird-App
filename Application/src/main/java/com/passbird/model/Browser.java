/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.model;

/**
 * Created by Arie Timmerman on 04/05/15.
 *
 * Stores details of a connected browser
 */
public class Browser {

    private int id;
    private String browser_id;
    private String public_key;

    private String my_private_key;

    private String browser_name;

    public Browser(String browser_id){
        setBrowser_id(browser_id);
    }

    public Browser(String browser_id, String public_key, String my_private_key, String browser_name){
        setBrowser_id(browser_id);
        setPublic_key(public_key);
        setBrowser_name(browser_name);
        setMy_private_key(my_private_key);
    }

    public Browser(int id, String browser_id, String public_key, String my_private_key, String browser_name){
        setId(id);
        setBrowser_id(browser_id);
        setPublic_key(public_key);
        setBrowser_name(browser_name);
        setMy_private_key(my_private_key);
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public String getBrowser_id() {
        return browser_id;
    }

    private void setBrowser_id(String browser_id) {
        this.browser_id = browser_id;
    }

    public String getPublic_key() {
        return public_key;
    }

    public void setPublic_key(String public_key) {
        this.public_key = public_key;
    }

    public String getBrowser_name() {
        return browser_name;
    }

    public void setBrowser_name(String browser_name) {
        this.browser_name = browser_name;
    }

    public String toString(){

        return String.format("browser id: %s, public key: %s, browser_name: %s",browser_id, public_key,browser_name);

    }

    public String getMy_private_key() {
        return my_private_key;
    }

    public void setMy_private_key(String my_private_key) {
        this.my_private_key = my_private_key;
    }


}
