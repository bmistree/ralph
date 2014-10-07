package ralph;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;

import com.google.protobuf.ByteString;

import ralph_protobuffs.CreateConnectionProto.CreateConnection;
import ralph_protobuffs.UtilProto.UUID;

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

    ByteString convert_constructor_to_byte_string() throws IOException
    {
        ByteArrayOutputStream byte_array_output_stream =
            new ByteArrayOutputStream();
        ObjectOutputStream out =
            new ObjectOutputStream(byte_array_output_stream);
        out.writeObject(endpt_constructor.getClass());
        out.close();

        out.writeObject(endpt_constructor.getClass());
        byte[] byte_array = byte_array_output_stream.toByteArray();
        byte_array_output_stream.close();
        
        // FIXME: this may be an inefficient way to construct
        // bytestring (two copies, instead of one).
        ByteString byte_string = ByteString.copyFrom(byte_array);
        return byte_string;
    }

    
    public Endpoint construct(ActiveEvent active_event)
    {
        SingleSideConnection ssc = new SingleSideConnection();
        return this.endpt_constructor.construct(ralph_globals,ssc);
    }
    public Endpoint construct_from_reference(
        ActiveEvent active_event,InternalServiceReference service_reference)
    {
        RalphConnObj.TCPConnectionObj tcp_connection_obj = null;
        try
        {
            tcp_connection_obj =
                new RalphConnObj.TCPConnectionObj(
                    service_reference.ip_addr,service_reference.tcp_port);
        }
        catch (IOException ex)
        {
            // FIXME: handle rejected tcp connection to alternate host.
            ex.printStackTrace();
            Util.logger_assert(
                "Unhandled IOEXception in construct_from_reference");
        }
        
        CreateConnection.Builder create_connection_msg =
            CreateConnection.newBuilder();
        
        UUID.Builder target_endpoint_uuid = UUID.newBuilder();
        target_endpoint_uuid.setData(service_reference.service_uuid);
        create_connection_msg.setTargetEndpointUuid(target_endpoint_uuid);

        UUID.Builder host_uuid = UUID.newBuilder();
        host_uuid.setData(ralph_globals.host_uuid);
        create_connection_msg.setHostUuid(host_uuid);
        
        tcp_connection_obj.write_create_connection(
            create_connection_msg.build());
        return endpt_constructor.construct(ralph_globals,tcp_connection_obj);
    }
}
