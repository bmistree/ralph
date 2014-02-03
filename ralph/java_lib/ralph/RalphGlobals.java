package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;

import java.util.concurrent.ConcurrentHashMap;

public class RalphGlobals {
    public LamportClock clock;
    public final String host_uuid = Util.generate_uuid();
    
    public AllEndpoints all_endpoints;
    public ThreadPool thread_pool;
    public DeadlockAvoidanceAlgorithm deadlock_avoidance_algorithm =
        DeadlockAvoidanceAlgorithm.BOOSTED;

    public final ConcurrentHashMap<String,ActiveEvent> all_events =
        new ConcurrentHashMap<String,ActiveEvent>();
    
    private List<Stoppable> stoppable_list =
        Collections.synchronizedList(new ArrayList<Stoppable>());

    public RalphGlobals(DeadlockAvoidanceAlgorithm daa)
    {
        deadlock_avoidance_algorithm = daa;
        all_endpoints = new AllEndpoints();
        clock = new LamportClock(all_endpoints);
        thread_pool = new ThreadPool(Util.DEFAULT_NUM_THREADS);
    }
    
    public RalphGlobals()
    {
        all_endpoints = new AllEndpoints();
        clock = new LamportClock(all_endpoints);
        thread_pool = new ThreadPool(Util.DEFAULT_NUM_THREADS);
    }

    public void add_stoppable(Stoppable stoppable) {
        stoppable_list.add(stoppable);
    }
	
}
