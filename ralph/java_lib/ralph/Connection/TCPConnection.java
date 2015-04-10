package ralph.Connection;


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
import ralph.Stoppable;
import ralph.RalphGlobals;
import ralph.DurabilityInfo;

import RalphDurability.DurabilityContext;

import ralph_protobuffs.GeneralMessageProto.GeneralMessage;
import ralph_protobuffs.CreateConnectionProto.CreateConnection;
import ralph_protobuffs.UtilProto;
import ralph_protobuffs.UtilProto.UUID;
import ralph_protobuffs.GeneralMessageProto.GeneralMessage;

public class TCPConnection extends ConnectionListenerManager
    implements Runnable, IConnection
{
    private final Socket sock;
    private final RalphGlobals ralph_globals;
    private final ReentrantLock _mutex = new ReentrantLock();
    /**
       Before registering this connection with sender, exchange
       messages with other side to populate remote_host_uuid.
     */
    private String remote_host_uuid = null;
    
    /**
       If not passed in a socket, then create a new connection to
       dst_host, dst_port.  Use sock for this connection.

       @throws IOException 
     */
    public TCPConnection(
        RalphGlobals ralph_globals, String dst_host, int dst_port)
        throws IOException
    {
        this(ralph_globals, new Socket(dst_host, dst_port));

    }

    public TCPConnection(RalphGlobals ralph_globals, Socket _sock)
    {
        sock = _sock;
        this.ralph_globals = ralph_globals;
        try
        {
            sock.setTcpNoDelay(true);
        }
        catch (SocketException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        send_initial_msg();
    }
    
    private void _lock()
    {
        _mutex.lock();
    }
    private void _unlock()
    {
        _mutex.unlock();
    }
    
    @Override
    public void close() 
    {}

    @Override
    public void send_msg(GeneralMessage msg_to_write)
    {
        _lock();
        try
        {
            msg_to_write.writeDelimitedTo(sock.getOutputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            _unlock();
        }
    }

    @Override
    public String remote_host_uuid()
    {
        return remote_host_uuid;
    }
    
    @Override
    public void run ()
    {
        listening_loop();
    }

    /**
       Wait for the first message from other side (to get remote
       side's host uuid) and register this connection with other side.
     */
    private void wait_for_initial_msg_and_register()
    {
        // first, listen for an initial message that provides the
        // remote host's uuid.  discard initial message
        GeneralMessage initial_gm = null;
        try
        {
            initial_gm = GeneralMessage.parseDelimitedFrom(
                sock.getInputStream());
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            Util.logger_assert("Error reading initial general message.");
        }
        remote_host_uuid = initial_gm.getSenderHostUuid().getData();
        ralph_globals.message_manager.add_connection(this);
    }

    /**
       When start a connection, send a message to other side so that
       other side can grab this side's host_uuid.
     */
    private void send_initial_msg()
    {
        GeneralMessage.Builder initial_msg = GeneralMessage.newBuilder();
        initial_msg.setTimestamp(ralph_globals.clock.get_int_timestamp());
        UtilProto.UUID.Builder sender_host_uuid_msg =
            UtilProto.UUID.newBuilder();
        sender_host_uuid_msg.setData(ralph_globals.host_uuid);
        initial_msg.setSenderHostUuid(sender_host_uuid_msg.build());
        
        send_msg(initial_msg.build());
    }
    
    private void listening_loop()
    {
        wait_for_initial_msg_and_register();
        while (true)
        {
            GeneralMessage gm = null;
            try
            {
                gm = GeneralMessage.parseDelimitedFrom(
                    sock.getInputStream());
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
                break;
            }
            recvd_msg_to_listeners(gm);
        }
    }
	
	
    public static class TCPAcceptThread extends Thread
    {
        private final RalphGlobals ralph_globals;
        private final String host_listen_on ;
        private final int port_listen_on;
        private final Stoppable stoppable;
        
        /**
         * @param{_TCPListeningStoppable object} stoppable --- Every 1s,
         breaks out of listening for new connections and checks if
         should stop waiting on connections.
	        
         @param{String} host_listen_on --- The ip/host to listen for
         new connections on.

         @param{int} port_listen_on --- The prot to listen for new
         connections on.
        */
        public TCPAcceptThread(
            Stoppable _stoppable, RalphGlobals _ralph_globals,
            String _host_listen_on, int _port_listen_on)
        {
            stoppable = _stoppable;
            ralph_globals = _ralph_globals;
            host_listen_on = _host_listen_on;
            port_listen_on = _port_listen_on;
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
                return;
            }
						
            while (true)
            {
                Socket client_conn;
                try 
                {
                    client_conn = sock.accept();
                }
                catch (IOException e) 
                {
                    if (stoppable.is_stopped())
                        break;
                    continue;
                }
                TCPConnection tcp_conn =
                    new TCPConnection(ralph_globals, client_conn);
                
                if (stoppable.is_stopped())
                    break;
            }
        }
    }
}

