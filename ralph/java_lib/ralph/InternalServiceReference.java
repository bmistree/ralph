package ralph;


public class InternalServiceReference
{
    private final String ip_addr;
    private final int tcp_port;
    
    public InternalServiceReference(String _ip_addr, int _tcp_port)
    {
        ip_addr = _ip_addr;
        tcp_port = _tcp_port;
    }
}