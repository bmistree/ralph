package ralph;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
   Ralph has a parallel keyword that looks like this

   parallel(
       some_list,
       ParallelBlock);

   When we execute the parallel block, we're applying the same
   parallel block multiple times to each element of the list.  

   This singleton holds a pool of threads that actually processes all
   parallel statements.
 */
public class SingletonParallelExecutors
{
    // May want to size this larger.
    private static final int NUM_PARALLEL_THREADS = 20;
    private static final ExecutorService executor =
        Executors.newFixedThreadPool(NUM_PARALLEL_THREADS);
    
    public static Future<Integer> submit(Callable<Integer> to_exec)
    {
        return executor.submit(to_exec);
    }
}

