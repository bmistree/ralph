package RalphVersions;

import ralph.EnumConstructorObj;
import ralph_protobuffs.DeltaProto.Delta;


public class EnumSerializer<EnumType extends Enum>
    implements IDeltaSerializer<EnumType>
{
    private final EnumConstructorObj<EnumType> enum_constructor_obj;
    
    public EnumSerializer(EnumConstructorObj<EnumType> _enum_constructor_obj)
    {
        enum_constructor_obj = _enum_constructor_obj;
    }

    @Override
    public Delta serialize(EnumType to_serialize)
    {
        Delta.EnumDelta.Builder enum_delta =
            Delta.EnumDelta.newBuilder();
        enum_delta.setEnumConstructorObjClassName(
            enum_constructor_obj.getClass().getName());
        
        if (to_serialize != null)
            enum_delta.setEnumOrdinal(to_serialize.ordinal());
        else
            enum_delta.setEnumOrdinal(-1);
        
        Delta.Builder proto_buff = Delta.newBuilder();
        proto_buff.setEnumDelta(enum_delta);
        return proto_buff.build();
    }
}