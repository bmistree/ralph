package ralph;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
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

    /**
       Deserialize endpoint constructors directly from byte strings.
     */
    public static EndpointConstructorObj deserialize_endpt_constructor(
        ByteString serialized_byte_string)
    {
        try
        {
            ObjectInputStream object_input_stream =
                new ObjectInputStream(serialized_byte_string.newInput());

            Class<EndpointConstructorObj> constructor_class =
                (Class<EndpointConstructorObj>) object_input_stream.readObject();
            object_input_stream.close();

            EndpointConstructorObj constructor_obj =
                constructor_class.newInstance();
            return constructor_obj;
        }
        catch (IOException _ex)
        {
            _ex.printStackTrace();
        }
        catch (ClassNotFoundException _ex)
        {
            _ex.printStackTrace();
        }
        catch (InstantiationException _ex)
        {
            _ex.printStackTrace();
        }
        catch (IllegalAccessException _ex)
        {
            _ex.printStackTrace();
        }

        // Should never get a case where 
        Util.logger_assert(
            "Not handling the case of unusual exceptions when deserializing " +
            "internal service factories.");
        return null;
    }
    
    /**
       Deserializing constructor.
     */
    public static InternalServiceFactory deserialize (
        ByteString serialized_byte_string,RalphGlobals ralph_globals)
    {
        if (serialized_byte_string.isEmpty())
            return null;
        
        EndpointConstructorObj internal = deserialize_endpt_constructor(
            serialized_byte_string);
        InternalServiceFactory to_return =
                new InternalServiceFactory(internal,ralph_globals);
        return to_return;
    }

    
    ByteString convert_constructor_to_byte_string() throws IOException
    {
        return InternalServiceFactory.static_convert_constructor_to_byte_string(
            endpt_constructor);
    }

    public static ByteString static_convert_constructor_to_byte_string(
        EndpointConstructorObj endpt_constructor) throws IOException
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
        return this.endpt_constructor.construct(
            ralph_globals,ssc,active_event.durability_context,null);
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
        return endpt_constructor.construct(
            ralph_globals,tcp_connection_obj,
            active_event.durability_context,null);
    }
}
