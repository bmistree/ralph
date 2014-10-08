package ralph;

import ralph_protobuffs.DeltaProto.Delta.ServiceReferenceDelta;

public class InternalServiceReference
{
    public final String ip_addr;
    public final int tcp_port;
    public final String service_uuid;
    
    
    public InternalServiceReference(
        String _ip_addr, int _tcp_port, String _service_uuid)
    {
        ip_addr = _ip_addr;
        tcp_port = _tcp_port;
        service_uuid = _service_uuid;
    }

    public static InternalServiceReference deserialize_delta (
        ServiceReferenceDelta delta)
    {
        if (! delta.hasIpAddr())
            return null;

        String ip_addr = delta.getIpAddr();
        int tcp_port = delta.getTcpPort();
        String service_uuid = delta.getServiceUuid();

        return new InternalServiceReference(ip_addr,tcp_port,service_uuid);
    }
}