package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

import RalphServiceConnectionListener.ConnectionListener;
import RalphDataWrappers.ListTypeDataWrapper;
import RalphDataWrappers.ListTypeDataWrapperFactory;
import RalphAtomicWrappers.BaseAtomicWrappers;

import ralph.MessageManager.MessageManager;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import ralph.ExecutionContext.ExecutionContext;
import ralph.Connection.SameHostConnection;
import ralph.Connection.TCPConnection.TCPAcceptThread;

import ralph.Install.InstallSender;
import ralph.MessageManager.InstallMessageProvider;


/**
   Have a sort of tortured approach to RalphGlobals.  RalphGlobals is
   supposed to be a singleton.  However, a lot of the testing code
   relies on being able to run both sides of a connection in a single
   process.  Therefore, even though we primarily use RalphGlobals as a
   singleton, we also pass it through as an argument to a bunch of
   objects.  Should eventually change test code.
 */

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
        public boolean logging_on = false;
        public String host_uuid = null;
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

    public final ConcurrentHashMap<String,ExecutionContext> all_ctx_map =
        new ConcurrentHashMap<String,ExecutionContext>();

    // set in constructor
    private final IUUIDGenerator uuid_generator;
    public final String host_uuid;
    public final DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm;
    private final ConnectionListener connection_listener;
    public final ThreadPool thread_pool;

    public final SameHostConnection same_host_connection;
    public final MessageManager message_manager =
        new MessageManager(this);
    private final InstallSender install_sender;

    // each ralph_globals object have a single connection listener
    // that listens for connections from remote hosts that we then can
    // send service factories, etc. to.
    private final TCPAcceptThread new_connection_listener;

    /**
       Some services need to know when we're stopping (eg., tcp
       listening for connections).  They call stop on ralph_globals
       object, which calls stop on stoppable.  RalphGlobals can pass
       stoppable to other objects and they check whether to stop.
     */
    private final Stoppable stoppable = new Stoppable();

    public RalphGlobals()
    {
        this(DEFAULT_PARAMETERS);
    }

    public void stop()
    {
        stoppable.stop();
    }

    public RalphGlobals(Parameters params)
    {
        deadlock_avoidance_algorithm = params.deadlock_avoidance_algorithm;

        connection_listener =
            new ConnectionListener(
                this, params.tcp_port_to_listen_for_connections_on);

        thread_pool = new ThreadPool(params.threadpool_params);
        uuid_generator = params.uuid_generator;
        // For now, generating host uuids as unique identifiers.  If
        // accidentally use local_atom_int generator, will get
        // collisions with other hosts.
        if (params.host_uuid != null)
            host_uuid = params.host_uuid;
        else
            host_uuid = UUIDGenerators.REAL_UUID_GENERATOR.generate_uuid();

        same_host_connection = new SameHostConnection(this);

        new_connection_listener = new TCPAcceptThread(
            stoppable, this, params.ip_addr_to_listen_for_connections_on,
            params.tcp_port_to_listen_for_connections_on);

        install_sender = new InstallSender(
            message_manager.install_message_provider, this);
    }

    private static class EndpointClassInitializerPair {
        private final Class<?> clazz;
        private final IInitializer initializer;

        public EndpointClassInitializerPair(Class<?> clazz,
                                            IInitializer initializer) {
            this.clazz = clazz;
            this.initializer = initializer;
        }
        public Class<?> getClazz() {
            return clazz;
        }
        public IInitializer getInitializer() {
            return initializer;
        }
    }

    private final Set<EndpointClassInitializerPair> all_initializers =
        new HashSet<EndpointClassInitializerPair>();
    public synchronized void register_initializer(Class<?> clazz,
                                                  IInitializer initializer) {
        all_initializers.add(new EndpointClassInitializerPair(clazz, initializer));
    }
    public synchronized void initialize_installed(Endpoint installed_endpoint) {
        for (EndpointClassInitializerPair pair : all_initializers) {
            Class<?> clazz = pair.getClazz();
            if (clazz.isInstance(installed_endpoint)) {
                IInitializer initializer = pair.getInitializer();
                try {
                    initializer.init_new(installed_endpoint);
                } catch (Exception ex) {
                    Util.logger_warn("Warning, encountered exception " + ex.toString() +
                                     "while initializing installed endpoint. Proceeding.");
                }
            }
        }
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

    public NonAtomicInternalList<String,String> connected_uuids()
    {
        List<RalphObject<String,String>> init_val =
            new ArrayList<RalphObject<String,String>>();
        for (String remote_uuid : message_manager.remote_connection_uuids())
        {
            init_val.add(
                new Variables.NonAtomicTextVariable(
                    false, remote_uuid, this));
        }

        NonAtomicInternalList<String,String> to_return =
            new NonAtomicInternalList(
                this,
                new ListTypeDataWrapperFactory<String,String>(java.lang.String.class),
                init_val,
                BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER);

        return to_return;
    }

    /**
       Forwards request to install on remote host.
     */
    public InternalServiceReference install_remote(
        String remote_host_uuid,
        InternalServiceFactory internal_service_factory)
    {
        Future<InternalServiceReference> fut_service_ref =
            install_sender.install_remote (
                remote_host_uuid, internal_service_factory);

        InternalServiceReference to_return = null;
        try
        {
            to_return = fut_service_ref.get();
        }
        catch (InterruptedException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Unexpected interrupted exception in install");
        }
        catch (ExecutionException ex)
        {
            ex.printStackTrace();
            Util.logger_assert("Unexpected execution exception in install");
        }


        return to_return;
    }
}