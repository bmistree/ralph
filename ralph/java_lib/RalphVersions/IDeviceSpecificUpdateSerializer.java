package RalphVersions;

import com.google.protobuf.ByteString;

public interface IDeviceSpecificUpdateSerializer<DeviceUpdateType>
{
    public ByteString serialize(DeviceUpdateType to_serialize);
}