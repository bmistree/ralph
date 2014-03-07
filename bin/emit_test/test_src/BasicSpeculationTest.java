package emit_test_harnesses;

import emit_test_package.BasicSpeculation.SpeculativeEndpoint;
import emit_test_package.BasicSpeculation.SpeculativeInterface;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Arrays;


public class BasicSpeculationTest
{
    private final static int RALPH_INTERNAL_SLEEP_TIME_MS = 250;
    private final static int RALPH_BETWEEN_EVENTS_SLEEP_TIME_MS = 5;
    
    // Given number of events, can calculate how long would expect a series of
    // events to run if we turn on speculation.  Importantly, due to rounding
    // error + time it takes to setup and tear down transactions, that might be
    // a little off.  Using fudge_factor_ms to adjust for these minor variations.
    private final static int FUDGE_FACTOR_MS = 1;

    
    public static void main(String[] args)
    {
        if (BasicSpeculationTest.run_test())
            System.out.println("\nSUCCESS in BasicSpeculationTest\n");
        else
            System.out.println("\nFAILURE in BasicSpeculationTest\n");
    }

    public static boolean run_test()
    {
        return all_unmixed_speculation_uninterrupted() &&
            all_unmixed_speculation_interrupted() &&
            all_mixed_speculation_tests();
    }

    public static boolean all_mixed_speculation_tests()
    {
        List<Integer> number_every_other_events =
            new ArrayList<Integer>(Arrays.asList(4,8,16));

        for (int num_every_other : number_every_other_events)
        {
            if (! every_other_test(num_every_other,true))
                return false;
        }
        for (int num_every_other : number_every_other_events)
        {
            if (! every_other_test(num_every_other,false))
                return false;
        }

        // arbitrary order of events
        List<Boolean> speculate_interrupted_pattern =
            new ArrayList<Boolean>(Arrays.asList(false,false,false,true,false,false));

        if (! mixed_speculation_test(speculate_interrupted_pattern,null))
            return false;
        
        speculate_interrupted_pattern =
            new ArrayList<Boolean>(Arrays.asList(true,true,false,false,true));
        
        return true;
    }

    /**
       @param num_every_other --- Runs a test that puts num_every_other events
       into system.  Alternates between putting in an event in that causes an
       interruption in derivative events and that doesn't.

       @param first_interrupted --- True: alternate pipeline_interrupted,
       pipeline, pipeline_interrupted, pipeline, ...  False: alternate
       pipeline, pipeline_interrupted, pipeline, ...
       
       Ie, every_other_test(4) creates a test that creates events that do the
       following:
       
       pipeline(num, false)
       pipeline_interrupted(num, false)
       pipeline(num, false)
       pipeline_interrupted(num, false)

       and then:
       
       pipeline(num, true)
       pipeline_interrupted(num, true)
       pipeline(num, true)
       pipeline_interrupted(num, true)
     */
    public static boolean every_other_test(
        int num_every_other, boolean first_interrupted)
    {
        List<Boolean> speculate_interrupted_pattern = new ArrayList<Boolean>();
        for (int i = 0; i < num_every_other; ++i)
        {
            int mod_equals = 1;
            if (first_interrupted)
                mod_equals = 0;
            
            boolean should_interrupt = (i%2 == mod_equals);
            speculate_interrupted_pattern.add(should_interrupt);
        }
        return mixed_speculation_test(speculate_interrupted_pattern,null);
    }
    
    /**
       When speculating, later must backout the speculation.
     */
    public static boolean all_unmixed_speculation_interrupted()
    {
        return unmixed_speculation_test(true,2) &&
            unmixed_speculation_test(true,4) &&
            unmixed_speculation_test(true,8);
    }

    public static boolean all_unmixed_speculation_uninterrupted()
    {
        return unmixed_speculation_test(false,2) &&
            unmixed_speculation_test(false,4) &&
            unmixed_speculation_test(false,8);
    }


    public static boolean unmixed_speculation_test(
        boolean interrupt_speculation, int num_events_in_pipeline)
    {
        List<Boolean> interrupt_speculation_pattern =
            new ArrayList<Boolean>();
        for (int i = 0; i < num_events_in_pipeline; ++i)
            interrupt_speculation_pattern.add(interrupt_speculation);

        AtomicLong speculation_on_ms_runtime = new AtomicLong();

        boolean worked = mixed_speculation_test(
            interrupt_speculation_pattern,speculation_on_ms_runtime);

        if (! worked)
            return false;

        if (! interrupt_speculation)
        {
            // If we do not interrupt speculation, then we expect that all of
            // the events can be pipelined.  What this means is that the maximum
            // amount of time all should take to run is below:
            int ms_to_run_if_pipelined_correctly = RALPH_INTERNAL_SLEEP_TIME_MS +
                RALPH_BETWEEN_EVENTS_SLEEP_TIME_MS*num_events_in_pipeline;
            
            if (speculation_on_ms_runtime.get() >
                (ms_to_run_if_pipelined_correctly + FUDGE_FACTOR_MS))
            {
                return false;
            }
        }
        return true;
    }


    /**
       @param interrupted_test_pattern --- Run a separate thread for each
       element in this list.  If the element is true, then run a pipeline
       interrupted test.  If it is false, then run just a pipeline test.
     */
    public static boolean mixed_speculation_test(
        List<Boolean> interrupt_speculation_pattern,
        AtomicLong speculation_on_ms_runtime)
    {
        try
        {
            SpeculativeEndpoint endpt = new SpeculativeEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();

            // just check pipeline without speculation
            double amt_to_inc_by = 35.0;
            
            long speculation_on_ms = pipelined_events_time_ms(
                endpt,true,amt_to_inc_by,interrupt_speculation_pattern);

            if (speculation_on_ms_runtime != null)
                speculation_on_ms_runtime.set(speculation_on_ms);
            
            double new_internal_number = endpt.get_number().doubleValue();
            double expected_internal_number =
                original_internal_number +
                interrupt_speculation_pattern.size()*amt_to_inc_by;
            
            if (new_internal_number != expected_internal_number)
                return false;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        return true;
    }

    private static class SingleOp extends Thread
    {
        private SpeculativeInterface endpt = null;
        private Double amt_to_inc_by = null;
        private boolean speculate;
        public boolean had_exception = false;
        private boolean interrupt_speculation = false;
        
        public SingleOp(
            SpeculativeInterface endpt, Double amt_to_inc_by,
            boolean speculate, boolean interrupt_speculation)
        {
            this.endpt = endpt;
            this.amt_to_inc_by = amt_to_inc_by;
            this.speculate = speculate;
            this.interrupt_speculation = interrupt_speculation;
        }
        public void run()
        {
            try
            {
                if (interrupt_speculation)
                    endpt.pipeline_interrupted(amt_to_inc_by,speculate);
                else
                    endpt.pipeline(amt_to_inc_by,speculate);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception = true;
            }
        }
    }


    /**
       @param interrupted_test_pattern --- Run a separate thread for each
       element in this list.  If the element is true, then run a pipeline
       interrupted test.  If it is false, then run just a pipeline test.
     */
    private static long pipelined_events_time_ms(
        SpeculativeInterface endpt, boolean speculate,
        double amt_to_inc_by,
        List<Boolean> interrupt_speculation_pattern)
        throws Exception
    {
        long start = System.currentTimeMillis();
        Set<SingleOp> all_threads = new HashSet<SingleOp>();
        for (Boolean interrupt_speculation : interrupt_speculation_pattern)
        {
            SingleOp t = new SingleOp(
                endpt,amt_to_inc_by,speculate,
                interrupt_speculation.booleanValue());
            all_threads.add(t);
            t.start();
            Thread.sleep(RALPH_BETWEEN_EVENTS_SLEEP_TIME_MS);
        }

        for (SingleOp s_op : all_threads)
        {
            s_op.join();
            if (s_op.had_exception)
                throw new Exception();
        }
        
        long end = System.currentTimeMillis();
        return end-start;
    }
}