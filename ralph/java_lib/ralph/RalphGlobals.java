package ralph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class RalphGlobals {
    public LamportClock clock;
    public AllEndpoints all_endpoints;
    public ThreadPool thread_pool;
	
    private List<Stoppable> stoppable_list =
        Collections.synchronizedList(new ArrayList<Stoppable>());
	
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
