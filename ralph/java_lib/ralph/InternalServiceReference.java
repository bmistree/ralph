package ralph;

import ralph_protobuffs.DeltaProto.Delta.ServiceReferenceDelta;

public class InternalServiceReference
{
    public final String remote_host_uuid;
    public final String service_uuid;
    
    
    public InternalServiceReference(
        String remote_host_uuid, String service_uuid)
    {
        this.remote_host_uuid = remote_host_uuid;
        this.service_uuid = service_uuid;
    }

    public static InternalServiceReference deserialize_delta (
        ServiceReferenceDelta delta)
    {
        if (! delta.hasRemoteHostUuid())
            return null;

        String remote_host_uuid = delta.getRemoteHostUuid();
        String service_uuid = delta.getServiceUuid();

        return new InternalServiceReference(remote_host_uuid, service_uuid);
    }
}