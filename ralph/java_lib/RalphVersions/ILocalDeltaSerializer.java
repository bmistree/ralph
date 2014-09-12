package RalphVersions;

import ralph_local_version_protobuffs.DeltaProto.Delta;

public interface ILocalDeltaSerializer<DataTypeToSerialize>
{
    public Delta serialize(DataTypeToSerialize to_serialize);
}
