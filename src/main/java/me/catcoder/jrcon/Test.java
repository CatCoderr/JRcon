package me.catcoder.jrcon;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;

public class Test {

    public static void main( String[] args ) throws ExecutionException, InterruptedException {
        EventLoopGroup eventLoops = JRcon.newEventloops( 2 );
        Bootstrap bootstrap = JRcon.newBootstrap( new InetSocketAddress( "localhost", 25567 ), eventLoops );

        bootstrap.connect().addListener( ( ChannelFutureListener ) future -> {
            if ( future.isSuccess() ) {
                //Connection is success
                JRconSession session = JRcon.newSession( future );

                session.authenticate( "password" ).get(); //Sync authentication

                System.out.println( "Authenticated: " + session.isAuthenticated() );

                session.executeCommand( "time set 0" ).thenAccept( System.out::println ); //Executing commands
            } else {
                //Handle I/O transport errors.
            }
        } );

    }
}
