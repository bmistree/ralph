package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import RalphVersions.ILocalVersionManager;

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

        // null means that we should not perform any logging.
        public ILocalVersionManager local_version_manager = null;
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
    private final IUUIDGenerator uuid_generator;
    public final String host_uuid;    
    public final String ip_addr_to_listen_for_connections_on;
    public final int tcp_port_to_listen_for_connections_on;
    public final DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm;
    private final ConnectionListener connection_listener;
    public final ThreadPool thread_pool;
    public final ILocalVersionManager local_version_manager;
    
    public RalphGlobals()
    {
        this(DEFAULT_PARAMETERS);
    }
    
    public RalphGlobals(Parameters params)
    {
        local_version_manager = params.local_version_manager;
        ip_addr_to_listen_for_connections_on =
            params.ip_addr_to_listen_for_connections_on;
        tcp_port_to_listen_for_connections_on =
            params.tcp_port_to_listen_for_connections_on;
        deadlock_avoidance_algorithm = params.deadlock_avoidance_algorithm;
        
        connection_listener =
            new ConnectionListener(
                all_endpoints,params.tcp_port_to_listen_for_connections_on);

        thread_pool = new ThreadPool(params.threadpool_params);
        uuid_generator = params.uuid_generator;
        // For now, generating host uuids as unique identifiers.  If
        // accidentally use local_atom_int generator, will get
        // collisions with other hosts.
        host_uuid = UUIDGenerators.REAL_UUID_GENERATOR.generate_uuid();
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
    
    // FIXME: get rid of this?
    public void add_stoppable(Stoppable stoppable)
    {
        stoppable_list.add(stoppable);
    }
}
