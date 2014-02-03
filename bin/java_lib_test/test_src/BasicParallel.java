package java_lib_test;

import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.ParallelBlock;
import ralph.ParallelBlockConstructor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;


public class BasicParallel
{
    protected static String test_name = "BasicParallel";
    
    /**
       Tests that can run multiple statements in parallel using a
       ParallelBlock.

       Does so by creating a single atomic int and then loading it
       multiple times into a list.  After running par_block, should
       have incremented atomic integer by number of elements in list.
     */
    public static void main (String [] args)
    {
        if (BasicParallel.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        int initial_atomic_value = 5;
        int num_to_parallelize_over = 25;
        AtomicInteger atom_int = new AtomicInteger(initial_atomic_value);
        ArrayList<AtomicInteger> atom_int_list = new ArrayList<AtomicInteger>();
        for (int i=0; i < num_to_parallelize_over; ++i)
            atom_int_list.add(atom_int);
        
        
        Endpoint endpt = TestClassUtil.create_default_single_endpoint();
        ActiveEvent active_event = null;
        try
        {
            active_event = endpt._act_event_map.create_root_atomic_event(null);

            new ParallelBlockConstructor<AtomicInteger>(
                endpt.global_var_stack,active_event)
            {
                public ParallelBlock<AtomicInteger> produce_par_block ()
                {
                    return new ParallelBlock<AtomicInteger>(vstack,active_event)
                    {
                        public void internal_call()
                            throws ApplicationException, BackoutException,
                            NetworkException,StoppedException
                        {
                            to_run_on.getAndIncrement();
                        }
                    };
                }
            }.exec_par(atom_int_list);
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        
        // test passed
        return true;
    }
}