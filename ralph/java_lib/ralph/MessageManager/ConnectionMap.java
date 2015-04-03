package ralph.MessageManager;

import java.util.Map;
import java.util.HashMap;

public class ConnectionMap
{
    private final Map<String, IConnection> connection_map =
        new HashMap<String, IConnection>();

    public synchronized void add_connection(IConnection conn)
    {
        connection_map.put(conn.remote_host_uuid(), conn);
    }

    public synchronized IConnection get_connection(String remote_host_uuid)
    {
        return connection_map.get(remote_host_uuid);
    }

    public synchronized IConnection remove_connection(String remote_host_uuid)
    {
        return connection_map.remove(remote_host_uuid);
    }
}