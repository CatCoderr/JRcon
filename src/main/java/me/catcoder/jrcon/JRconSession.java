package me.catcoder.jrcon;

import io.netty.channel.ChannelFuture;

import java.util.concurrent.CompletableFuture;

public interface JRconSession {

    CompletableFuture<String> executeCommand( String command );

    boolean isAuthenticated( );

    CompletableFuture<Boolean> authenticate( String password );

    ChannelFuture close( );
}
