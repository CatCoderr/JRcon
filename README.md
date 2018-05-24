# JRcon
Example implementation of RCON protocol based on Netty

## Usage

Firstly, we need the following settings:
```java
        InetSocketAddress serverAddress = new InetSocketAddress( "localhost", 25565 );
        String password = "example";
        String command = "broadcast Hello from Rcon!";
        EventLoopGroup eventLoops = JRcon.newEventloops( numberOfThreads );

        RconResponseHandler responseHandler = new RconResponseHandler() {
            @Override
            public void handleResponse( String payload ) {
                //handle command response
            }

            @Override
            public void authenticationFailed( ) {
                //server rejected our password
            }
        };
```
Finally, we can create a new bootstrap from settings below:
```java
        Bootstrap bootstrap = JRcon.newBootstrap( serverAddress, password, command, eventLoops, responseHandler );

        //Connect and listen I/O transport errors
        bootstrap.connect().addListener( ( ChannelFutureListener ) future -> {
            if ( !future.isSuccess() ) {
                //handle error
                future.cause().printStackTrace();
            }
        } );
```
