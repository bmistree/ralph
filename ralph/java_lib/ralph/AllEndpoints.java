package ralph;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class AllEndpoints extends Thread
{
    private ReentrantLock _mutex = new ReentrantLock();
    private HashMap<String,Endpoint> endpoint_map =
        new HashMap<String,Endpoint>();

    /**
       each element will be a map of endpoints.  send_clock_update
       will put copies of self.endpoint_map into the queue and a
       separate thread (clock_update_thread) running
       sep_thread_listening_for_updates will read off the queue and
       then request each endpoint in the map to send a clock update
       to its partner.
    */
    private ArrayBlockingQueue<ArrayList<Endpoint>> endpoints_to_update_queue = 
        new ArrayBlockingQueue<ArrayList<Endpoint>> (Util.QUEUE_CAPACITIES); 
	
    public AllEndpoints()
    {
        setDaemon(true);
        // start self as soon as constructed
        start();
    }
	
    /**
     * Run from run()
     @see comment above self.endpoints_to_update_queue        
    */
    public void sep_thread_listening_for_updates()
    {
        while (true)
        {
            ArrayList<Endpoint> copied_endpoint_list = null;
            try {
                copied_endpoint_list = endpoints_to_update_queue.take();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for (Endpoint endpt : copied_endpoint_list)
                endpt._send_clock_update();
        }
    }
    public void run()
    {
        sep_thread_listening_for_updates();
    }
	
    public void _lock()
    {
        _mutex.lock();
    }
    public void _unlock()
    {
        _mutex.unlock();
    }

    public void send_clock_update()
    {
        _lock();
        ArrayList<Endpoint>copied_endpoint_list = new ArrayList<Endpoint>(endpoint_map.values());
        _unlock();
        
        //note: okay that running through after release lock because
        //new endpoints will already grab new clock dates
        endpoints_to_update_queue.add(copied_endpoint_list);
    }
            
    public void add_endpoint(Endpoint endpoint)
    {
        _lock();
        endpoint_map.put(endpoint._uuid, endpoint);
        _unlock();
    }
	
    public void _remove_endpoint_if_exists(Endpoint endpoint)
    {
        _lock();
        endpoint_map.remove(endpoint._uuid);
        _unlock();
    }
	
    public void endpoint_stopped(Endpoint which_endpoint_stopped)
    {
        _remove_endpoint_if_exists(which_endpoint_stopped);
    }

    public void network_exception(Endpoint which_endpoint_excepted)
    {
        _remove_endpoint_if_exists(which_endpoint_excepted);
    }
	
	
}
