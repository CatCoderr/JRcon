# JRcon
Example implementation of RCON protocol based on Netty

## Add to your project

Add jitpack repo
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

Add JRcon to your project
```xml
	<dependency>
	    <groupId>com.github.CatCoderr</groupId>
	    <artifactId>JRcon</artifactId>
	    <version>LATEST_VERSION</version>
	</dependency>
```

## Usage

Firstly, we need the following settings:
```java
        InetSocketAddress serverAddress = new InetSocketAddress( "localhost", 25565 );
        String password = "example";
        String command = "broadcast Hello from Rcon!";
        EventLoopGroup eventLoops = JRcon.newEventloops( numberOfThreads );

```
Finally, we can create a new bootstrap from settings below:
```java
        Bootstrap bootstrap = JRcon.newBootstrap( serverAddress, eventLoops );

        bootstrap.connect().addListener( ( ChannelFutureListener ) future -> {
            if ( future.isSuccess() ) {
                //Connection is sucessfully, then create a new session
                JRconSession session = JRcon.newSession( future );

                session.authenticate( password ).get(); //Sync authentication

                System.out.println( "Authenticated: " + session.isAuthenticated() );

                session.executeCommand( command ).thenAccept( System.out::println ); //Executing commands
            } else {
                //Handle I/O transport errors.
            }
        } );
```
