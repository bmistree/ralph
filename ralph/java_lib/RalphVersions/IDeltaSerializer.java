package RalphVersions;

import ralph_protobuffs.DeltaProto.Delta;

public interface IDeltaSerializer<DataTypeToSerialize>
{
    public Delta serialize(DataTypeToSerialize to_serialize);
}
