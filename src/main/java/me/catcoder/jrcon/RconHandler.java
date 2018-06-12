package me.catcoder.jrcon;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RconHandler extends ChannelInboundHandlerAdapter {

    private static final byte FAILURE = -1;
    private static final byte TYPE_RESPONSE = 0;
    private static final byte TYPE_COMMAND = 2;
    private static final byte TYPE_LOGIN = 3;


    private StringBuffer buffer;
    private boolean authenticated;

    private CompletableFuture<Boolean> authFuture;
    private final Map<Integer, CompletableFuture<String>> commandResponses = new HashMap<>();

    private final AtomicInteger requestId = new AtomicInteger( 1 );

    private Channel channel;

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception {
        this.channel = ctx.channel();
    }

    CompletableFuture<Boolean> authenticate( String password ) {
        checkState( channel != null, "Channel is not active yet." );
        checkState( !isAuthenticated(), "Session already authenticated." );

        this.authFuture = new CompletableFuture<>();
        channel.writeAndFlush( writeTo( TYPE_LOGIN, password, channel.alloc().buffer().order( ByteOrder.LITTLE_ENDIAN ) ) );
        return authFuture;
    }

    CompletableFuture<String> executeCommand( String command ) {
        checkState( channel != null, "Channel is not active yet." );
        checkState( isAuthenticated(), "Session is not authenticated." );

        CompletableFuture<String> future = new CompletableFuture<>();

        commandResponses.put( requestId.incrementAndGet(), future );

        channel.writeAndFlush( writeTo( TYPE_COMMAND, command, channel.alloc().buffer().order( ByteOrder.LITTLE_ENDIAN ) ) );

        return future;
    }

    boolean isAuthenticated( ) {
        return authenticated;
    }

    @Override
    public void channelRead( ChannelHandlerContext ctx, Object msg ) throws Exception {
        ByteBuf buf = ( ( ByteBuf ) msg ).order( ByteOrder.LITTLE_ENDIAN );

        int requestId = buf.readInt();
        int type = buf.readInt();

        if ( type == TYPE_RESPONSE ) { //Large packet
            if ( buffer == null ) {
                buffer = new StringBuffer();
            }
            buffer.append( readPayload( buf ) );
        }
        String payload = buffer == null ? readPayload( buf ) : buffer.toString();

        if ( type == TYPE_COMMAND ) {

            authenticated = requestId != FAILURE;

        } else if ( type == TYPE_RESPONSE && !buf.isReadable() ) {

            CompletableFuture<String> responseHandler = commandResponses.remove( requestId );

            if ( responseHandler != null ) {
                responseHandler.complete( payload );
            }

            buffer = null;
        }

        if ( authFuture != null && type == TYPE_COMMAND ) {
            authFuture.complete( authenticated );
            authFuture = null;
        }
        if ( !authenticated ) {
            ctx.close();
        }

    }


    private void checkState( boolean condition, String messageFormat, Object... objects ) {
        if ( !condition ) {
            throw new IllegalStateException( String.format( messageFormat, objects ) );
        }

    }

    private ByteBuf writeTo( int type, String payload, ByteBuf buf ) {
        buf.writeInt( requestId.get() );
        buf.writeInt( type );
        buf.writeBytes( payload.getBytes( StandardCharsets.UTF_8 ) );
        buf.writeByte( 0 );
        buf.writeByte( 0 );
        return buf;
    }

    private String readPayload( ByteBuf buf ) {
        byte[] payloadBytes = new byte[ buf.readableBytes() - 2 ];
        buf.readBytes( payloadBytes );
        buf.skipBytes( 2 ); //two byte padding
        return new String( payloadBytes, StandardCharsets.UTF_8 );
    }

}
