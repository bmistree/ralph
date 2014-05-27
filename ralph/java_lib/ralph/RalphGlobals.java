package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import RalphServiceConnectionListener.ConnectionListener;

import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;

public class RalphGlobals implements IUUIDGenerator
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
        public IUUIDGenerator uuid_generator =
            UUIDGenerators.ATOM_INT_UUID_GENERATOR;
    }

    /**
       Construct globally unique UUIDs as BASE_UUID +
       uuid_generator.generate_uuid().
     */
    private static final String BASE_UUID =
        UUIDGenerators.REAL_UUID_GENERATOR.generate_uuid();
    
    public static final Parameters DEFAULT_PARAMETERS = new Parameters();
        
    public final AllEndpoints all_endpoints = new AllEndpoints();
    public final LamportClock clock = new LamportClock(all_endpoints);
    
    public final ConcurrentHashMap<String,ActiveEvent> all_events =
        new ConcurrentHashMap<String,ActiveEvent>();

    private List<Stoppable> stoppable_list =
        Collections.synchronizedList(new ArrayList<Stoppable>());

    // set in constructor
    private IUUIDGenerator uuid_generator;
    public String host_uuid;    
    public String ip_addr_to_listen_for_connections_on;
    public int tcp_port_to_listen_for_connections_on;
    public DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm;
    private ConnectionListener connection_listener;
    public ThreadPool thread_pool;
    
    public RalphGlobals()
    {
        init(DEFAULT_PARAMETERS);
    }
    
    public RalphGlobals(Parameters params)
    {
        init(params);
    }

    @Override
    public String generate_uuid()
    {
        return BASE_UUID + uuid_generator.generate_uuid();
    }

    public String generate_local_uuid()
    {
        return UUIDGenerators.LOCAL_ATOM_INT_UUID_GENERATOR.generate_uuid();
    }
    
    private void init(Parameters params)
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
        uuid_generator = params.uuid_generator;
        host_uuid = uuid_generator.generate_uuid();
    }
    
    // FIXME: get rid of this?
    public void add_stoppable(Stoppable stoppable)
    {
        stoppable_list.add(stoppable);
    }
}
