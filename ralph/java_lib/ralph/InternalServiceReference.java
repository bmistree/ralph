package ralph;


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
}