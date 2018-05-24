package me.catcoder.jrcon;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/***
 * <p>
 * Example implementation of RCON protocol based on Netty.
 *
 * @author CatCoder
 * @version 1.0
 */
public class JRcon {

    private JRcon( ) {
        throw new IllegalStateException( "Don't initialize me!" );
    }

    public static Bootstrap newBootstrap( InetSocketAddress address,
                                          String password,
                                          String command,
                                          EventLoopGroup group,
                                          RconResponseHandler handler ) {
        return new Bootstrap()
                .channel( getClientChannel() )
                .group( group )
                .attr( RconHandler.COMMAND_ATTR, command )
                .attr( RconHandler.PASSWORD_ATTR, password )
                .attr( RconHandler.RESPONSE_HANDLER_ATTR, handler )
                .handler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel( SocketChannel ch ) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast( "framer", new RconFramingHandler() );
                        pipeline.addLast( "handler", new RconHandler() );

                    }
                } )
                .remoteAddress( address );
    }

    public static EventLoopGroup newEventloops( int threads ) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup( threads ) : new NioEventLoopGroup( threads );
    }

    public static Class<? extends SocketChannel> getClientChannel( ) {
        return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

}
