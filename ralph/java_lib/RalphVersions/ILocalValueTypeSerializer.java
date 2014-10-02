package RalphVersions;

import ralph_version_protobuffs.DeltaProto.Delta.ValueType;

public interface ILocalValueTypeSerializer<ToSerializeType>
{
    public ValueType serialize_value_type(ToSerializeType to_serialize);
}