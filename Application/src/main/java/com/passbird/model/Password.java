/*
 * Copyright 2015 Arie Timmerman
 */

package com.passbird.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Arie Timmerman on 04/05/15.
 *
 * Stores password details
 */
public class Password {


    private int id = -1;

    private HashMap<String,String> values = new HashMap<>();

    //password table
    public static final String KEY_LOCATION = "location";
    public static final String KEY_DOMAIN = "domain";
    public static final String KEY_TITLE = "title";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_ICON = "icon";
    public static final String KEY_EXTRA = "extra";
    public static final String KEY_PASSWORD = "password";

    private JSONObject extra = new JSONObject();

    private final static String[] icons = {"adn","android","angellist","apple","behance","behance-square","bitbucket","bitbucket-square","bitcoin (alias)","btc","buysellads","cc-amex","cc-discover","cc-mastercard","cc-paypal","cc-stripe","cc-visa","codepen","connectdevelop","css3","dashcube","delicious","deviantart","digg","dribbble","dropbox","drupal","empire","facebook","facebook-f (alias)","facebook-official","facebook-square","flickr","forumbee","foursquare","ge (alias)","git","git-square","github","github-alt","github-square","gittip (alias)","google","google-plus","google-plus-square","google-wallet","gratipay","hacker-news","html5","instagram","ioxhost","joomla","jsfiddle","lastfm","lastfm-square","leanpub","linkedin","linkedin-square","linux","maxcdn","meanpath","medium","openid","pagelines","paypal","pied-piper","pied-piper-alt","pinterest","pinterest-p","pinterest-square","qq","ra (alias)","rebel","reddit","reddit-square","renren","sellsy","share-alt","share-alt-square","shirtsinbulk","simplybuilt","skyatlas","skype","slack","slideshare","soundcloud","spotify","stack-exchange","stack-overflow","steam","steam-square","stumbleupon","stumbleupon-circle","tencent-weibo","trello","tumblr","tumblr-square","twitch","twitter","twitter-square","viacoin","vimeo-square","vine","vk","wechat (alias)","weibo","weixin","whatsapp","windows","wordpress","xing","xing-square","yahoo","yelp","youtube","youtube-play","youtube-square"};

    public static String findAutoIcon(String domain){

        String result = "globe";

        if(domain != null) for(String icon : icons){
            if(domain.contains(icon)){
                result = icon;
                break;
            }
        }

        return result;
    }

    public Password(){

    }

    @SuppressWarnings("WeakerAccess")
    public Password(String location, String domain, String title, String username, String icon, JSONObject extra, String password){

        setId(id);

        icon = (icon==null||icon.isEmpty())? "cloud" : icon;

        setValue(KEY_LOCATION, location);
        setValue(KEY_DOMAIN,domain);
        setValue(KEY_TITLE,title);
        setValue(KEY_USERNAME,username);
        setValue(KEY_ICON,icon);
        setValue(KEY_PASSWORD,password);

        setExtra(extra);

    }

    public Password(int id, String location, String domain, String title, String username, String icon, String extra, String password){
        setId(id);

        icon = (icon==null||icon.isEmpty())? "cloud" : icon;

        setValue(KEY_LOCATION, location);
        setValue(KEY_DOMAIN,domain);
        setValue(KEY_TITLE,title);
        setValue(KEY_USERNAME,username);
        setValue(KEY_ICON,icon);
        setValue(KEY_PASSWORD, password);

        if(extra == null){
            extra = "{}";
        }

        try {
            setExtra(new JSONObject(extra));
        } catch (JSONException ignored) {

        }
    }

    public JSONObject getExtra() {
        return extra;
    }

    private void setExtra(JSONObject extra) {
        if(extra == null) {
            this.extra = new JSONObject();
        }else{
            this.extra = extra;
        }
    }

    public int getId() {
        return id;
    }

    @SuppressWarnings("WeakerAccess")
    public void setId(int id) {
        this.id = id;
    }

    public String toString(){
        return String.format("location: %s, title: %s, username: %s, extra: %s, password: %s",getValue(KEY_LOCATION),getValue(KEY_TITLE),getValue(KEY_USERNAME),extra.toString(),getValue(KEY_PASSWORD));
    }


    public void putExtra(String key, Object value){
        try {
            this.extra.put(key,value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Object getExtra(@SuppressWarnings("SameParameterValue") String key){
        Object result = null;

        try {
            result = this.extra.get(key);
        } catch (JSONException ignored) {
        }

        return result;
    }


    public void setValue(String key, String value){
        values.put(key,value);
    }

    public String getValue(String key){
        return values.get(key)!=null?values.get(key):"";
    }
}
