package RalphVersions;

import ralph_protobuffs.DeltaProto.Delta.ValueType;

public interface IValueTypeSerializer<ToSerializeType>
{
    public ValueType serialize_value_type(ToSerializeType to_serialize);
}