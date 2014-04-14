package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;

public class RalphGlobals
{
    public final String host_uuid = Util.generate_uuid();
    
    public final AllEndpoints all_endpoints = new AllEndpoints();
    public final LamportClock clock = new LamportClock(all_endpoints);
    public final ThreadPool thread_pool =
        new ThreadPool(Util.DEFAULT_NUM_THREADS);
    public DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm =
        DeadlockAvoidanceAlgorithm.BOOSTED;

    public final ConcurrentHashMap<String,ActiveEvent> all_events =
        new ConcurrentHashMap<String,ActiveEvent>();

    public int tcp_port_to_listen_for_connections_on =
        Util.DEFAULT_TCP_PORT_NEW_CONNECTIONS;
    public String ip_addr_to_listen_for_connections_on =
        Util.DEFAULT_IP_ADDRESS_NEW_CONNECTIONS;
    
    private List<Stoppable> stoppable_list =
        Collections.synchronizedList(new ArrayList<Stoppable>());

    public RalphGlobals(DeadlockAvoidanceAlgorithm daa)
    {
        deadlock_avoidance_algorithm = daa;
    }
    
    public RalphGlobals()
    { }

    public RalphGlobals (
        String _ip_addr_to_listen_for_connections_on,
        int _tcp_port_to_listen_for_connections_on)
    {
        ip_addr_to_listen_for_connections_on =
            _ip_addr_to_listen_for_connections_on;
        tcp_port_to_listen_for_connections_on =
            _tcp_port_to_listen_for_connections_on;
    }
    
    public void add_stoppable(Stoppable stoppable)
    {
        stoppable_list.add(stoppable);
    }
}
