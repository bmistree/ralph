package RalphVersions;

public interface ILocalDeltaSerializer<DataTypeToSerialize>
{
    public byte[] serialize(DataTypeToSerialize to_serialize);
}
