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
    public static final int DEFAULT_WORK_QUEUE_CAPACITIES = 500;
    public static final int DEFAULT_PERSISTENT_NUM_THREADS = 70;
    public static final int DEFAULT_MAX_NUM_THREADS = 150;
    public static final long DEFAULT_KEEP_ALIVE_TIME = 1L;
    public static final TimeUnit DEFAULT_KEEP_ALIVE_TIME_UNIT =
        TimeUnit.SECONDS;
    
    public static class Parameters
    {
        public int work_queue_capacities = DEFAULT_WORK_QUEUE_CAPACITIES;
        public int persistent_num_threads = DEFAULT_PERSISTENT_NUM_THREADS;
        public int max_num_threads = DEFAULT_MAX_NUM_THREADS;
        public long keep_alive_time = DEFAULT_KEEP_ALIVE_TIME;
        public TimeUnit keep_alive_time_unit = DEFAULT_KEEP_ALIVE_TIME_UNIT;
    }
    public static final Parameters DEFAULT_PARAMETERS = new Parameters();
    
    
    private final ThreadPoolExecutor executor;
    private final ArrayBlockingQueue<Runnable> work_queue;
    
    public ThreadPool(Parameters params)
    {
        work_queue =
            new ArrayBlockingQueue<Runnable>(params.work_queue_capacities);
        
        executor = new ThreadPoolExecutor (
            params.persistent_num_threads,params.max_num_threads,
            params.keep_alive_time, params.keep_alive_time_unit, work_queue,
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

