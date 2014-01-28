package ralph;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import RalphServiceActions.ServiceAction;

public class ThreadPool 
{
    private ExecutorService executor = null;
	
    public ThreadPool(int num_threads)
    {
        executor = Executors.newCachedThreadPool(
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

