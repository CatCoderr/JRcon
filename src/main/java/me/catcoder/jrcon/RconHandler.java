package me.catcoder.jrcon;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class RconHandler extends ChannelInboundHandlerAdapter {

    static final AttributeKey<RconResponseHandler> RESPONSE_HANDLER_ATTR = AttributeKey.valueOf( "JRcon Response Handler" );
    static final AttributeKey<String> PASSWORD_ATTR = AttributeKey.valueOf( "Password" );
    static final AttributeKey<String> COMMAND_ATTR = AttributeKey.valueOf( "Command" );

    private static final byte FAILURE = -1;
    private static final byte TYPE_RESPONSE = 0;
    private static final byte TYPE_COMMAND = 2;
    private static final byte TYPE_LOGIN = 3;

    private final int requestId = ThreadLocalRandom.current().nextInt( 0xff );

    private RconState state = RconState.LOGIN;
    private StringBuffer buffer;

    public RconState getState( ) {
        return state;
    }

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception {
        ctx.writeAndFlush( writeTo( TYPE_LOGIN, ctx.channel().attr( PASSWORD_ATTR ).get(), ctx.alloc().buffer().order( ByteOrder.LITTLE_ENDIAN ) ) );
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

        RconResponseHandler handler = ctx.channel().attr( RESPONSE_HANDLER_ATTR ).get();

        checkState( this.requestId == requestId, "Request id mismatch (excepted %s), got %s", this.requestId, requestId );

        if ( type == TYPE_COMMAND ) {
            checkExceptedState( RconState.LOGIN );

            if ( requestId == FAILURE ) {
                handler.authenticationFailed();
                state = RconState.FINISHED;
                return;
            }
            writeTo( TYPE_COMMAND, ctx.channel().attr( COMMAND_ATTR ).get(), buf );
            state = RconState.COMMAND;

        } else if ( type == TYPE_RESPONSE && !buf.isReadable() ) {

            checkExceptedState( RconState.COMMAND );

            state = RconState.FINISHED;
            buffer = null;

            handler.handleResponse( payload );
        }

        if ( buf.isReadable() ) {
            ctx.writeAndFlush( buf );
        }

        if ( state == RconState.FINISHED ) {
            ctx.close();
        }
    }


    private void checkExceptedState( RconState state ) {
        checkState( this.state == state, "Excepted %s", state );
    }

    private void checkState( boolean condition, String messageFormat, Object... objects ) {
        if ( !condition ) {
            throw new IllegalStateException( String.format( messageFormat, objects ) );
        }

    }

    private ByteBuf writeTo( int type, String payload, ByteBuf buf ) {
        buf.writeInt( requestId );
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
