package ralph;


public class InternalServiceReference
{
    private final String ip_addr;
    private final int tcp_port;
    private final String service_uuid;
    
    
    public InternalServiceReference(
        String _ip_addr, int _tcp_port, String _service_uuid)
    {
        ip_addr = _ip_addr;
        tcp_port = _tcp_port;
        service_uuid = _service_uuid;
    }
}