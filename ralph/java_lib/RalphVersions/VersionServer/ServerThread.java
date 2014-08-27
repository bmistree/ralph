package RalphVersions.VersionServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import RalphVersions.IVersionManager;

public class ServerThread extends Thread
{
    final IVersionManager version_manager;
    final String host_to_listen_on;
    final int port_to_listen_on;

    public ServerThread(
        IVersionManager _version_manager,
        String _host_to_listen_on, int _port_to_listen_on)
    {
        version_manager = _version_manager;
        host_to_listen_on = _host_to_listen_on;
        port_to_listen_on = _port_to_listen_on;
        setDaemon(true);
    }

    @Override
    public void run()
    {
        ServerSocket sock = null;

        try
        {
            sock = new ServerSocket(port_to_listen_on);
            sock.setSoTimeout(1000);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println(
                "Could not listen on port " + port_to_listen_on);
            assert(false);
            return;
        }

        while (true)
        {
            Socket version_requester_socket = null;
            try 
            {
                version_requester_socket = sock.accept();
            } catch (IOException e) 
            {
                e.printStackTrace();
                System.err.println(
                    "IOException for versions on " + port_to_listen_on);
                assert(false);
                return;
            }

            VersionServerConnection version_server_connection =
                new VersionServerConnection(
                    version_requester_socket,version_manager);
        }
    }
}

