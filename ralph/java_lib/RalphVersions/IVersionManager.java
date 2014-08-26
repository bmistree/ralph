package RalphVersions;

import ralph_version_protobuffs.VersionMessageProto.VersionMessage;
import ralph_version_protobuffs.VersionRequestProto.VersionRequestMessage;

public interface IVersionManager
{
    public VersionMessage.Builder produce_response(
        VersionRequestMessage request);
    public void add_single_device_update_list(
        SingleDeviceUpdateList sdul);
}
