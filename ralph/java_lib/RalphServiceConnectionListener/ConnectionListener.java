package RalphServiceConnectionListener;

import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.lang.Thread;

import ralph_protobuffs.CreateConnectionProto.CreateConnection;

import ralph.Connection.TCPConnection;
import ralph.Util;
import ralph.RalphGlobals;

/**
   To create a Service, we can use a ServiceFactory.  Eg.,

   // On host 1
   ServiceFactory sf = ...;
   Service SomeService serv =
       dynamic_cast<Service SomeService> sf.construct();

   However, if we then want to create another service that is
   connected to serv through partner calls, we must use
   ServiceReferences.  To create a ServiceReference, we modify the
   above code.

   // On host 1
   ServiceFactory sf = ...;
   Service SomeService serv =
       dynamic_cast<Service SomeService> sf.construct();
   ServiceReference sr = serv.rpc_reference();

   Following, we can send the reference to other hosts, which can
   instantiate objects that make direct partner calls on serv from a
   different host.  Eg.,

   // On host 2
   ServiceReference sr = ...; // from above
   ServiceFactory sf = ...; // can be different from above

   Service OtherService other_service =
       dynamic_cast<Service OtherService> sf.construct_from_reference(sr);
   other_service.method_call(); // where method_call may make @partner
                                // calls to serv above.

   ------------------------------------------------------
   
   Importantly, to make this feasible, host 2 needs to create a
   connection to Host1, which ties other_service and serv together.

   This class listens for connections from host 2 to host 1 containing
   the uuid of serv.  It uses these messages to create a connection to
   serv and set serv's connection_obj.
 */
public class ConnectionListener implements Runnable
{
    private final RalphGlobals ralph_globals;
    private final int tcp_port_to_listen_on;
    
    public ConnectionListener(
        RalphGlobals ralph_globals, int _tcp_port_to_listen_on)
    {
        this.ralph_globals = ralph_globals;
        tcp_port_to_listen_on = _tcp_port_to_listen_on;
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    /**
       Run by a separate thread which listens for tcp connections.
       For each tcp connection, should get a createConnection message,
       which can use to 
     */
    @Override
    public void run()
    {
        ServerSocket sock = null;
        try
        {
            sock = new ServerSocket(tcp_port_to_listen_on);
        }
        catch(IOException io_ex)
        {
            io_ex.printStackTrace();
            Util.logger_assert(
                "Unexpected error when opening socket for service connection.");
        }
        
        while (true)
        {
            try
            {
                final Socket connector = sock.accept();
                
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        handle_new_connection(connector);
                    }
                };
                t.setDaemon(true);
                t.start();
            }
            catch (IOException io_ex)
            {
                // NOTE: we probably do not have to assert out on this
                // error.  We'd expect some clients to die while
                // connecting.  That doesn't bother us because we have
                // allocated no resources to deal with them.
                io_ex.printStackTrace();
                Util.logger_assert(
                    "Unexpected error when opening socket " +
                    "for service connection.");
            }
        }
    }

    /**
       Started in distinct thread.  Try to read a createConnection
       message from socket.
     */
    private void handle_new_connection(Socket connector_sock)
    {
        Thread t = 
            new Thread(new TCPConnection(ralph_globals, connector_sock));
        t.setDaemon(true);
        t.start();
    }
}
