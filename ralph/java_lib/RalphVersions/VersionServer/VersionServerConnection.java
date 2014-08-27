package RalphVersions.VersionServer;

import java.io.IOException;
import java.net.Socket;

import ralph_version_protobuffs.VersionMessageProto.VersionMessage;
import ralph_version_protobuffs.VersionRequestProto.VersionRequestMessage;

import RalphVersions.IVersionManager;

public class VersionServerConnection extends Thread
{
    private final Socket version_requester_socket;
    final IVersionManager version_manager;
    public VersionServerConnection(
        Socket _version_requester_socket,IVersionManager _version_manager)
    {
        version_requester_socket = _version_requester_socket;
        version_manager = _version_manager;

        // Start listening thread, which responds to version
        // information requests.
        setDaemon(true);
        start();
    }


    /**
       Listens for requeset commands from partner and responds to them.
     */
    @Override
    public void run()
    {

        while (true)
        {
            VersionMessage version_message = null;
            try
            {
                version_message = VersionMessage.parseDelimitedFrom(
                    version_requester_socket.getInputStream());
            }
            catch (IOException e)
            {
                // means other side closed connection.  break while
                // loop and finish.
                break;
            }

            //// DEBUG: only can respond to version queries.  server
            //// connection should never receive a response, because
            //// will never issue queries from it.
            if (! version_message.hasRequest())
            {
                System.err.println(
                    "Unexpected version request received in " +
                    "server connection.");
                assert(false);
                return;
            }
            //// END DEBUG

            // produce response
            VersionRequestMessage request_message =
                version_message.getRequest();
            VersionMessage.Builder response =
                version_manager.produce_response(request_message);

            // write back response
            try
            {
                response.build().writeDelimitedTo(
                    version_requester_socket.getOutputStream());
            }
            catch (IOException e)
            {
                e.printStackTrace();
                System.err.println(
                    "Unexpectedly could not write version response to " +
                    "partner: likely other end closed.");
                break;
            }
        }
    }
}
