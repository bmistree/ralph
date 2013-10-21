package ralph;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import WaldoConnObj.TCPConnectionObj.TCPAcceptThread;


public class Waldo {

    private static WaldoGlobals all_globals = new WaldoGlobals();
    private static String host_uuid = Util.generate_uuid();
	

    /**
     *  Creates an endpoint without its partner.

     Returns:
    
     Waldo endpoint: Calls constructor with args for an endpoint that
     has no partner.

     * @param constructor
     */
    public static Endpoint no_partner_create(
        EndpointConstructorObj constructor)
    {
        return constructor.construct(
            all_globals,host_uuid,
            new WaldoConnObj.SingleSideConnection());
    }
        
	
	
    /**
     * 	Tries to connect an endpoint to another endpoint via a TCP
     connection.

     Args:
	    
     constructor (Endpoint Constructor): The constructor of the endpoint to
     create upon connection.  Should be imported from the compiled Waldo file.

     host (String): The name of the host to connect to.

     port (int): The TCP port to try to connect to.

     *args (*args):  Any arguments that should get passed to
     the endpoint's onCreate method for initialization.

     Returns:

     Endpoint object: --- Can call any Public method of this
     object.
     * @throws IOException 
     */
    public static Endpoint tcp_connect(
        EndpointConstructorObj constructor_obj, String host, int port)
        throws IOException
    {
        WaldoConnObj.TCPConnectionObj tcp_connection_obj = 
            new WaldoConnObj.TCPConnectionObj(host,port);
        return constructor_obj.construct(all_globals,host_uuid,tcp_connection_obj);
    }


    /**
       Non-blocking function that listens for TCP connections and
       creates endpoints for each new connection.

       Args:
    
       constructor(Endpoint Constructor): The constructor of the
       endpoint to create upon connection.  Should be imported from
       the compiled Waldo file.

       host (String): The name of the host to listen for
       connections on.

       port(int): The TCP port to listen for connections on.

       *args(*args): Any arguments that should get passed to
       the endpoint's onCreate method for initialization.

       Kwargs:

       connected_callback(function): Use kwarg "connected_callback."
       When a connection is received and we create an endpoint,
       callback gets executed, passing in newly-created endpoint
       object as argument.

       Returns:
    
       Stoppable object: Can call stop method on this to stop
       listening for additional connections.  Note: listeners will not
       stop instantly, but probably within the next second or two.
    */
    public static Stoppable tcp_accept(
        EndpointConstructorObj constructor_obj, String host, int port)
    {
        return tcp_accept(constructor_obj,host,port, null);
    }

    /**
     * 
     * @param constructor_obj
     * @param host
     * @param port
     * @param connected_callback --- Can be null
     * @return
     */
    public static Stoppable tcp_accept(
        EndpointConstructorObj constructor_obj, String host, int port,
        SignalFunction connected_callback)
    {
        //# TCP listenener starts in a separate thread.  We must wait for
        //# the calling thread to actually be listening for connections
        //# before returning.
        ArrayBlockingQueue<Boolean> synchronization_listening_queue =
            new ArrayBlockingQueue<Boolean>(Util.SMALL_QUEUE_CAPACITIES);
		
        Stoppable stoppable = new Stoppable();
		
        TCPAcceptThread accept_thread = new 
            TCPAcceptThread(stoppable,constructor_obj,
                            all_globals,host,port,connected_callback,
                            host_uuid,
                            synchronization_listening_queue);
		
        accept_thread.start();
		
        //# once this returns, we know that we are listening on the
        //# host:port pair.
        try {
            synchronization_listening_queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //# the user can call stop directly on the returned object, but we
        //# still put it into the cleanup queue.  This is because there is
        //# no disadvantage to calling stop multiple times.
        all_globals.add_stoppable(stoppable);
        return stoppable;
    }
	
}
