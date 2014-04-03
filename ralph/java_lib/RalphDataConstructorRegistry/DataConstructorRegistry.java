package RalphDataConstructorRegistry;

import java.util.Map;
import java.util.HashMap;

import ralph_protobuffs.VariablesProto;
import ralph.RalphObject;
import ralph.RalphGlobals;


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
    
    // Maps from unique name defining the struct to a DataConstructor
    // that can be used to produce the struct.
    private final Map<String, StructDataConstructor> struct_constructors =
        new HashMap<String,StructDataConstructor>();
    
    protected DataConstructorRegistry()
    {
        // protected so that nothing else can create a new version of
        // this class
    }

    public static DataConstructorRegistry get_instance()
    {
        return instance;
    }


    public void register_struct(String name, StructDataConstructor constructor)
    {
        struct_constructors.put(name,constructor);
    }
    
    public RalphObject deserialize_struct(
        VariablesProto.Variables.Struct proto_struct,
        RalphGlobals ralph_globals)
    {
        StructDataConstructor constructor =
            struct_constructors.get(proto_struct.getStructIdentifier());
        return constructor.construct(ralph_globals,proto_struct);
    }
}