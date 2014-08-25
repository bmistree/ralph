package RalphVersions;

public interface IDeviceSpecificUpdateSerializer<DeviceUpdateType>
{
    public byte[] serialize(DeviceUpdateType to_serialize);
}