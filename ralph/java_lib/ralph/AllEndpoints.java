package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class AllEndpoints implements IEndpointMap
{
    private HashMap<String,Endpoint> endpoint_map =
        new HashMap<String,Endpoint>();
	
    public AllEndpoints()
    {}
	
    @Override
    public synchronized void add_endpoint(Endpoint endpoint)
    {
        endpoint_map.put(endpoint._uuid, endpoint);
    }
    
    @Override
    public synchronized void remove_endpoint_if_exists(Endpoint endpoint)
    {
        endpoint_map.remove(endpoint._uuid);
    }

    /**
       @returns null if no endpoint available.
     */
    @Override
    public synchronized Endpoint get_endpoint_if_exists(String uuid)
    {
        Endpoint to_return = endpoint_map.get(uuid);
        return to_return;
    }
}
