package RalphConnObj;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import ralph.Util;
import ralph.Endpoint;
import ralph.EndpointConstructorObj;
import ralph.SignalFunction;
import ralph.Stoppable;
import ralph.RalphGlobals;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import ralph_protobuffs.CreateConnectionProto.CreateConnection;

public class TCPConnectionObj implements ConnectionObj, Runnable
{
    private Endpoint local_endpoint = null;
    private java.net.Socket sock = null;

    private final ReentrantLock _mutex = new ReentrantLock();
    
    /**
     * If not passed in a socket, then
     create a new connection to dst_host, dst_port.
     Use sock for this connection.
     * @throws IOException 

     */
    public TCPConnectionObj(String dst_host, int dst_port) throws IOException
    {
        sock = new java.net.Socket(dst_host,dst_port);
        sock.setTcpNoDelay(true);
    }

    public TCPConnectionObj(java.net.Socket _sock) 
    {
        sock = _sock;
		
        try {
            sock.setTcpNoDelay(true);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void _lock()
    {
        _mutex.lock();
    }
    private void _unlock()
    {
        _mutex.unlock();
    }
    
    
    /**
     * Actually close the socket
     * @throws IOException 
     */
    public void close() 
    {
        try {
            sock.close();
        } catch (IOException e) {
            local_endpoint.partner_connection_failure();
        }
    }

    public void write_stop(
        GeneralMessage msg_to_write,ralph.Endpoint endpoint_writing)
    {
    	write(msg_to_write,endpoint_writing);
    }
     
    

    /**
       @param {String} msg_str_to_write
       @param {Endpoint} sender_endpoint_obj
        
       Gets called from endpoint to send message from one side to the
       other.
    */    
    public void write(
        GeneralMessage msg_to_write, ralph.Endpoint sender_endpoint_obj)
    {
        _lock();
        try {
            msg_to_write.writeDelimitedTo(sock.getOutputStream());
        }
        catch (IOException e)
        {
            local_endpoint.partner_connection_failure();            
        }
        finally
        {
            _unlock();
        }
    }

    public void write_create_connection(CreateConnection msg_to_write)
    {
        _lock();
        try
        {
            msg_to_write.writeDelimitedTo(sock.getOutputStream());
        }
        catch (IOException e)
        {
            // FIXME: consider writing back NetworkException error.
            e.printStackTrace();
            Util.logger_assert(
                "Not handling IOExceptions on write_create_connection.  " +
                "Should likely throw a NetworkException for event.");
        }
        finally
        {
            _unlock();
        }
    }
    

    /**
       @param {_Endpoint object} local_endpoint --- @see the emitted
       code for a list of _Endpoint object methods.
        
       Once we have an attached endpoint, we start listening for data
       to send to that endpoint.
    */
    public void register_endpoint(ralph.Endpoint _local_endpoint)
    {
        local_endpoint = _local_endpoint;
        
        // create anonymous thread to start listening on the
        // connection:
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    public void run ()
    {
        listening_loop();
    }

    private void listening_loop()
    {
        while (true)
        {
            GeneralMessage gm = null;
            try {
                gm = GeneralMessage.parseDelimitedFrom(
                    sock.getInputStream());
            } catch (IOException e) {
                local_endpoint.partner_connection_failure();
                break;
            }
            local_endpoint._receive_msg_from_partner(gm);            
        }
    }
	
	
    public static class TCPAcceptThread extends Thread
    {
		
        private Stoppable stoppable = null;
        private EndpointConstructorObj endpoint_constructor = null;
        private RalphGlobals ralph_globals = null;
        private String host_listen_on ;
        private int port_listen_on;
        private SignalFunction cb = null;
        ArrayBlockingQueue<Boolean> synchronization_listening_queue = null;
		
		
        /**
         * @param{_TCPListeningStoppable object} stoppable --- Every 1s,
         breaks out of listening for new connections and checks if
         should stop waiting on connections.
	        
         @param{function}endpoint_constructor --- An _Endpoint object's
         constructor.  It takes in a tcp connection object, reservation
         manager object, and any additional arguments specified in its
         oncreate method.
	        
         @param{String} host_listen_on --- The ip/host to listen for
         new connections on.

         @param{int} port_listen_on --- The prot to listen for new
         connections on.

         @param {function or Non} cb --- When receive a new connection,
         execute the callback, passing in a new Endpoint object in its
         callback.  

         @param {Queue.Queue} synchronization_listening_queue --- The
         thread that began this thread blocks waiting for a value on
         this queue so that it does not return before this thread has
         started to listen for the connection.
        */
        public TCPAcceptThread(
            Stoppable _stoppable, ralph.EndpointConstructorObj _endpoint_constructor,
            ralph.RalphGlobals _ralph_globals, String _host_listen_on,
            int _port_listen_on, SignalFunction _cb, 
            ArrayBlockingQueue<Boolean> _synchronization_listening_queue)
        {
            stoppable = _stoppable;
            endpoint_constructor = _endpoint_constructor;
            ralph_globals = _ralph_globals;
            host_listen_on = _host_listen_on;
            port_listen_on = _port_listen_on;
            cb = _cb;
            synchronization_listening_queue = _synchronization_listening_queue;
            setDaemon(true);
        }
		

        public void run()
        {
            ServerSocket sock = null;
			
            try
            {
                sock = new ServerSocket(port_listen_on);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                synchronization_listening_queue.add(new Boolean(false));	
                return;
            }
						
            synchronization_listening_queue.add(new Boolean(true));
            while (true)
            {
                Socket client_conn;
                try 
                {
                    client_conn = sock.accept();
                } catch (IOException e) 
                {
                    if (stoppable.is_stopped())
                        break;
                    continue;
                }
                TCPConnectionObj tcp_conn_obj = new TCPConnectionObj(client_conn);
				
                Endpoint created_endpoint =
                    endpoint_constructor.construct(ralph_globals, tcp_conn_obj);
                
                if (cb != null)
                    cb.fire(created_endpoint);
				
                if (stoppable.is_stopped())
                    break;
				
            }
        }
    }
}

