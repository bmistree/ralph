package RalphDataConstructorRegistry;

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
import ralph.AtomicList;
import ralph.NonAtomicList;

import ralph.Variables.NonAtomicTextVariable;
import ralph.Variables.NonAtomicNumberVariable;
import ralph.Variables.NonAtomicTrueFalseVariable;
import ralph.Variables.NonAtomicServiceFactoryVariable;
import ralph.Variables.AtomicTextVariable;
import ralph.Variables.AtomicNumberVariable;
import ralph.Variables.AtomicTrueFalseVariable;
import ralph.Variables.AtomicServiceFactoryVariable;


/**
   Singleton.
   
   Need special facilities for serializing and deserializing
   user-defined structs.  Whenever create a user-defined struct, one
   of its static members tries to register a constructor for it into
   DataConstructorRegistry.  Can then use deserialize_struct to create
   a struct of that type.
 */
public class DataConstructorRegistry
{
    private final static DataConstructorRegistry instance =
        new DataConstructorRegistry();

    // unused: creation here registers all basic list data
    // constructors in constructors map.
    private final static BasicListDataConstructors basic_list_constructors_instance =
        BasicListDataConstructors.get_instance();
    
    // Maps from unique name defining the struct to a DataConstructor
    // that can be used to produce the struct.
    private final Map<String, DataConstructor> constructors =
        new HashMap<String,DataConstructor>();

    // reuse same event when deserializing
    private final static DeserializationEvent const_deserialization_event =
        new DeserializationEvent();
    public static ActiveEvent dummy_deserialization_active_event()
    {
        return const_deserialization_event;
    }
    
    protected DataConstructorRegistry()
    {
        // protected so that nothing else can create a new version of
        // this class
    }

    public static DataConstructorRegistry get_instance()
    {
        return instance;
    }

    public static String merge_labels(String label_outside, String label_inside)
    {
        return label_outside + "|" + label_inside;
    }
    
    public void register(String name, DataConstructor constructor)
    {
        constructors.put(name,constructor);
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

            DataConstructor dc =
                constructors.get(full_deserialization_label);
            if (dc != null)
                lo = dc.construct(any,ralph_globals);
            else
                Util.logger_assert("Missing list deserialiation type.");
        }
        else if (any.hasMap())
        {
            Util.logger_assert("Skipping locked maps");
        }
        else if (any.hasStruct())
        {
            // FIXME: should check if is tvar or not.
            VariablesProto.Variables.Struct struct_proto =
                any.getStruct();
            DataConstructor dc =
                constructors.get(struct_proto.getStructIdentifier());
            lo = dc.construct(any,ralph_globals);
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
        return lo;
    }
}