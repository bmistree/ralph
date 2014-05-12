package RalphDeserializer;

import java.util.Map;
import java.util.HashMap;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;


import ralph_protobuffs.VariablesProto;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.Util;
import ralph.ActiveEvent;

import ralph.EndpointConstructorObj;
import ralph.InternalServiceFactory;
import ralph.InternalServiceReference;
import ralph.AtomicList;
import ralph.NonAtomicList;
import ralph.AtomicMap;
import ralph.NonAtomicMap;

import ralph.Variables.NonAtomicTextVariable;
import ralph.Variables.NonAtomicNumberVariable;
import ralph.Variables.NonAtomicTrueFalseVariable;
import ralph.Variables.NonAtomicServiceFactoryVariable;
import ralph.Variables.NonAtomicServiceReferenceVariable;
import ralph.Variables.AtomicTextVariable;
import ralph.Variables.AtomicNumberVariable;
import ralph.Variables.AtomicTrueFalseVariable;
import ralph.Variables.AtomicServiceFactoryVariable;
import ralph.Variables.AtomicServiceReferenceVariable;


/**
   Singleton.
   
   Need special facilities for serializing and deserializing
   user-defined structs.  Whenever create a user-defined struct, one
   of its static members tries to register a constructor for it into
   Deserializer.  Can then use deserialize_struct to create
   a struct of that type.
 */
public class Deserializer
{
    private final static Deserializer instance =
        new Deserializer();

    // unused: creation here registers all basic list and map data
    // deserializers in deserializers map.
    private final static BasicListDataDeserializers basic_list_deserializers_instance =
        BasicListDataDeserializers.get_instance();
    private final static BasicMapDataDeserializers basic_map_deserializers_instance =
        BasicMapDataDeserializers.get_instance();
    
    
    // Maps from unique name defining the struct to a DataDeserializer
    // that can be used to produce the struct.
    private final Map<String, DataDeserializer> deserializers =
        new HashMap<String,DataDeserializer>();

    // reuse same event when deserializing
    private final static DeserializationEvent const_deserialization_event =
        new DeserializationEvent();
    public static ActiveEvent dummy_deserialization_active_event()
    {
        return const_deserialization_event;
    }
    
    protected Deserializer()
    {
        // protected so that nothing else can create a new version of
        // this class
    }

    public static Deserializer get_instance()
    {
        return instance;
    }

    public static String merge_labels(String label_outside, String label_inside)
    {
        return label_outside + "|" + label_inside;
    }
    
    public void register(String name, DataDeserializer deserializer)
    {
        deserializers.put(name,deserializer);
    }

    /**
       @returns {RalphObject or null} --- Returns null if the
       argument passed in was empty.  This could happen for instance
       if deserializing any corresponding to result for a
       non-passed-by-reference argument.
     */
    public RalphObject deserialize(
        VariablesProto.Variables.Any any, RalphGlobals ralph_globals)
    {
        RalphObject lo = null;
        if (any.hasNum())
        {
            if (any.getIsTvar())
            {
                lo = new AtomicNumberVariable(
                    false,new Double(any.getNum()),ralph_globals);
            }
            else
            {
                lo = new NonAtomicNumberVariable(
                    false,new Double(any.getNum()),ralph_globals);
            }
        }
        else if (any.hasText())
        {
            if (any.getIsTvar())
            {
                lo = new AtomicTextVariable(
                    false,any.getText(),ralph_globals);
            }
            else
            {
                lo = new NonAtomicTextVariable(
                    false,any.getText(),ralph_globals);
            }
        }
        else if (any.hasTrueFalse())
        {
            if (any.getIsTvar())
            {
                lo = new AtomicTrueFalseVariable(
                    false,new Boolean(any.getTrueFalse()),ralph_globals);
            }
            else
            {
                lo = new NonAtomicTrueFalseVariable(
                    false,new Boolean(any.getTrueFalse()),ralph_globals);
            }
        }
        else if (any.hasList())
        {
            String list_deserialization_label =
                NonAtomicList.deserialization_label;
            if (any.getIsTvar())
                list_deserialization_label = AtomicList.deserialization_label;

            VariablesProto.Variables.List list_message = any.getList();
            
            String full_deserialization_label =
                merge_labels(
                    list_deserialization_label,
                    list_message.getElementTypeIdentifier());

            DataDeserializer dc =
                deserializers.get(full_deserialization_label);
            if (dc != null)
                lo = dc.deserialize(any,ralph_globals);
            else
                Util.logger_assert("Missing list deserialiation type.");
        }
        else if (any.hasMap())
        {
            String map_deserialization_label =
                NonAtomicMap.deserialization_label;
            if (any.getIsTvar())
                map_deserialization_label = AtomicMap.deserialization_label;

            VariablesProto.Variables.Map map_message = any.getMap();

            String partial_deserialization_label = 
                merge_labels(
                    map_deserialization_label,
                    map_message.getKeyTypeIdentifier());
            String full_deserialization_label =
                merge_labels(
                    partial_deserialization_label,
                    map_message.getValueTypeIdentifier());
            
            DataDeserializer dc =
                deserializers.get(full_deserialization_label);
            if (dc != null)
                lo = dc.deserialize(any,ralph_globals);
            else
                Util.logger_assert("Missing map deserialiation type.");
        }
        else if (any.hasStruct())
        {
            // FIXME: should check if is tvar or not.
            VariablesProto.Variables.Struct struct_proto =
                any.getStruct();
            DataDeserializer dc =
                deserializers.get(struct_proto.getStructIdentifier());
            lo = dc.deserialize(any,ralph_globals);
        }
        else if (any.hasServiceFactory())
        {
            EndpointConstructorObj constructor_obj = null;
            try {
                byte [] byte_array = any.getServiceFactory().toByteArray();
                ByteArrayInputStream array_stream =
                    new ByteArrayInputStream(byte_array);
                ObjectInputStream in = new ObjectInputStream(array_stream);
                Class<EndpointConstructorObj> constructor_class =
                    (Class<EndpointConstructorObj>) in.readObject();
                in.close();
                array_stream.close();
                constructor_obj = constructor_class.newInstance();
            } catch (Exception ex) {
                // FIXME: should catch deserialization error and backout.
                ex.printStackTrace();
                Util.logger_assert("Issue deserializing service factory");
            }

            InternalServiceFactory internal_service_factory =
                new InternalServiceFactory(constructor_obj,ralph_globals);

            if (any.getIsTvar())
            {
                lo = new AtomicServiceFactoryVariable(
                    false,internal_service_factory,ralph_globals);
            }
            else
            {
                lo = new NonAtomicServiceFactoryVariable(
                    false,internal_service_factory,ralph_globals);
            }
        }
        else if (any.hasServiceReference())
        {
            VariablesProto.Variables.ServiceReference service_reference_proto =
                any.getServiceReference();
            
            String ip_addr = service_reference_proto.getIpAddr();
            int tcp_port = service_reference_proto.getTcpPort();
            String service_uuid =
                service_reference_proto.getServiceUuid().getData();

            InternalServiceReference isr =
                new InternalServiceReference(ip_addr,tcp_port,service_uuid);
            
            if (any.getIsTvar())
            {
                lo = new AtomicServiceReferenceVariable(
                    false,isr,ralph_globals);
            }
            else
            {
                lo = new NonAtomicServiceReferenceVariable(
                    false,isr,ralph_globals);
            }
        }
        return lo;
    }
}