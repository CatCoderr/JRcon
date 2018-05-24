package me.catcoder.jrcon;

public interface RconResponseHandler {

    public void handleResponse( String payload );

    public void authenticationFailed( );
}
