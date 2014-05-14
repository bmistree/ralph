package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import RalphServiceConnectionListener.ConnectionListener;

import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;

public class RalphGlobals
{
    public static class Parameters
    {
        public ThreadPool.Parameters threadpool_params =
            ThreadPool.DEFAULT_PARAMETERS;
        public DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm =
            DeadlockAvoidanceAlgorithm.BOOSTED;
        public int tcp_port_to_listen_for_connections_on =
            Util.DEFAULT_TCP_PORT_NEW_CONNECTIONS;
        public String ip_addr_to_listen_for_connections_on = 
            Util.DEFAULT_IP_ADDRESS_NEW_CONNECTIONS;
    }
    
    public static final Parameters DEFAULT_PARAMETERS = new Parameters();
    
    public final String host_uuid = Util.generate_uuid();
    
    public final AllEndpoints all_endpoints = new AllEndpoints();
    public final LamportClock clock = new LamportClock(all_endpoints);
    private ConnectionListener connection_listener;
    
    public ThreadPool thread_pool;
    public final ConcurrentHashMap<String,ActiveEvent> all_events =
        new ConcurrentHashMap<String,ActiveEvent>();

    private List<Stoppable> stoppable_list =
        Collections.synchronizedList(new ArrayList<Stoppable>());

    public String ip_addr_to_listen_for_connections_on;
    public int tcp_port_to_listen_for_connections_on;
    public DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm;
    
    public RalphGlobals()
    {
        init(DEFAULT_PARAMETERS);
    }
    
    public RalphGlobals(Parameters params)
    {
        init(params);
    }

    public void init(Parameters params)
    {
        ip_addr_to_listen_for_connections_on =
            params.ip_addr_to_listen_for_connections_on;
        tcp_port_to_listen_for_connections_on =
            params.tcp_port_to_listen_for_connections_on;
        deadlock_avoidance_algorithm = params.deadlock_avoidance_algorithm;
        
        connection_listener =
            new ConnectionListener(
                all_endpoints,params.tcp_port_to_listen_for_connections_on);
        deadlock_avoidance_algorithm = params.deadlock_avoidance_algorithm;
        thread_pool = new ThreadPool(params.threadpool_params);
    }
    
    // FIXME: get rid of this?
    public void add_stoppable(Stoppable stoppable)
    {
        stoppable_list.add(stoppable);
    }
}
