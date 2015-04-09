package ralph;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

import RalphDurability.DurabilityContext;

import ralph.Connection.TCPConnection;
import ralph.Connection.TCPConnection.TCPAcceptThread;
import ralph.Connection.SingleSideConnection;

public class Ralph
{
    /**
     *  Creates an endpoint without its partner.

     Returns:
    
     Waldo endpoint: Calls constructor with args for an endpoint that
     has no partner.

     * @param constructor
     */
    public static Endpoint no_partner_create(
        EndpointConstructorObj constructor, RalphGlobals all_globals)
    {
        // log the newly created endpoint
        DurabilityContext durability_context = null;
        if (DurabilityInfo.instance.durability_saver != null)
        {
            String dummy_event_uuid = all_globals.generate_uuid();
            durability_context =
                new DurabilityContext(dummy_event_uuid,all_globals);
        }

        try
        {
            return constructor.construct(
                all_globals, SingleSideConnection.INSTANCE,
                durability_context,null);
        }
        finally
        {
            if (DurabilityInfo.instance.durability_saver != null)
            {
                // FIXME: should actually be logging the endpoint
                // type as well here.
                DurabilityInfo.instance.durability_saver.prepare_operation(
                    durability_context);
                DurabilityInfo.instance.durability_saver.complete_operation(
                    durability_context,true);
            }
        }
    }

	
    /**
     * Tries to connect an endpoint to another endpoint via a TCP
     connection.

     Args:
	    
     constructor (Endpoint Constructor): The constructor of the
     endpoint to create upon connection.  Should be imported from the
     compiled Waldo file.

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
        EndpointConstructorObj constructor_obj, String host, int port,
        RalphGlobals all_globals)
        throws IOException
    {
        // log the newly created endpoint
        DurabilityContext durability_context = null;
        if (DurabilityInfo.instance.durability_saver != null)
        {
            String dummy_event_uuid = all_globals.generate_uuid();
            durability_context =
                new DurabilityContext(dummy_event_uuid,all_globals);
        }
        
        TCPConnection tcp_connection_obj = 
            new TCPConnection(all_globals, host, port);
        try
        {
            return constructor_obj.construct(
                all_globals,tcp_connection_obj,durability_context,null);
        }
        finally
        {
            if (DurabilityInfo.instance.durability_saver != null)
            {
                // FIXME: should actually be logging the endpoint
                // type as well here.
                DurabilityInfo.instance.durability_saver.prepare_operation(
                    durability_context);
                DurabilityInfo.instance.durability_saver.complete_operation(
                    durability_context,true);
            }
        }
    }
}
