package ralph;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import RalphServiceActions.ServiceAction;


public class ThreadPool 
{
    public final int WORK_QUEUE_CAPACITIES = 500;
    
    private final ThreadPoolExecutor executor;
    private final ArrayBlockingQueue<Runnable> work_queue =
        new ArrayBlockingQueue<Runnable>(WORK_QUEUE_CAPACITIES);
    
    public ThreadPool(
        int persistent_num_threads, int max_num_threads, TimeUnit keep_alive_time)
    {
        executor = new ThreadPoolExecutor (
            persistent_num_threads,max_num_threads, 1L,keep_alive_time,
            work_queue,
            new ThreadFactory()
            {
                // each thread created is a daemon
                public Thread newThread(Runnable r)
                {
                    Thread t=new Thread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
    }
	
    public void add_service_action(ServiceAction service_action)
    {
        executor.execute(service_action);
    }
}

