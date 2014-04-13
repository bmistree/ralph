package ralph;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


import RalphConnObj.SingleSideConnection;

/**
   Should cast to whatever type expect outside of construct.
 */
public class InternalServiceFactory
{
    private EndpointConstructorObj endpt_constructor = null;
    private RalphGlobals ralph_globals = null;

    
    public InternalServiceFactory(
        EndpointConstructorObj endpt_constructor,RalphGlobals ralph_globals)
    {
        this.endpt_constructor = endpt_constructor;
        this.ralph_globals = ralph_globals;
    }

    byte [] convert_constructor_to_byte_array() throws IOException
    {
        ByteArrayOutputStream byte_array_output_stream =
            new ByteArrayOutputStream();
        ObjectOutputStream out =
            new ObjectOutputStream(byte_array_output_stream);
        out.writeObject(endpt_constructor.getClass());
        out.close();

        out.writeObject(endpt_constructor.getClass());
        byte[] to_return = byte_array_output_stream.toByteArray();
        byte_array_output_stream.close();
        return to_return;
    }
    
    public Endpoint construct(ActiveEvent active_event)
    {
        SingleSideConnection ssc = new SingleSideConnection();
        return this.endpt_constructor.construct(ralph_globals,ssc);
    }
    public Endpoint construct_from_reference(
        ActiveEvent active_event,InternalServiceReference service_reference)
    {
        // FIXME: should fill in this stub for constructing endpoints
        // from references.
        Util.logger_assert(
            "FIXME: Must fill in construct from reference method");
    }
    
}