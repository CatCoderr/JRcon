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
import java.util.concurrent.CompletableFuture;

/***
 * <p>
 * Example implementation of RCON protocol based on Netty.
 *
 * @author CatCoder
 * @version 2.0
 */
public class JRcon {

    private JRcon( ) {
        throw new IllegalStateException( "Don't initialize me!" );
    }

    /**
     * Define JRcon bootstrap.
     *
     * @param address - server address
     * @param group   - event loops
     * @return configured bootstrap
     */
    public static Bootstrap newBootstrap( InetSocketAddress address,
                                          EventLoopGroup group ) {
        return new Bootstrap()
                .channel( getClientChannel() )
                .group( group )
                .handler( new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel( SocketChannel ch ) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast( "rcon_framer", new RconFramingHandler() );
                        pipeline.addLast( "rcon_handler", new RconHandler() );

                    }
                } )
                .remoteAddress( address );
    }

    /**
     * Creates new JRcon session to interact with server.
     *
     * @param connectFuture - connect future (use {@link JRcon#newBootstrap(InetSocketAddress, EventLoopGroup)} method)
     * @return new unauthenticated JRcon session.
     */
    public static JRconSession newSession( ChannelFuture connectFuture ) {
        Channel channel = connectFuture.syncUninterruptibly().channel();

        ChannelPipeline pipeline = channel.pipeline();

        RconHandler handler = pipeline.get( RconHandler.class );

        return new JRconSession() {
            @Override
            public CompletableFuture<String> executeCommand( String command ) {
                return handler.executeCommand( command );
            }

            @Override
            public boolean isAuthenticated( ) {
                return handler.isAuthenticated();
            }

            @Override
            public CompletableFuture<Boolean> authenticate( String password ) {
                return handler.authenticate( password );
            }

            @Override
            public ChannelFuture close( ) {
                return channel.close();
            }
        };
    }

    public static EventLoopGroup newEventloops( int threads ) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup( threads ) : new NioEventLoopGroup( threads );
    }

    public static Class<? extends SocketChannel> getClientChannel( ) {
        return Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class;
    }

}
