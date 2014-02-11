package emit_test_harnesses;

import emit_test_package.BasicSpeculation.SpeculativeEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class BasicSpeculationTest
{
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
        return true;
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

        AtomicLong speculation_off_ms_runtime = new AtomicLong();
        AtomicLong speculation_on_ms_runtime = new AtomicLong();

        boolean worked = mixed_speculation_test(
            interrupt_speculation_pattern,speculation_off_ms_runtime,
            speculation_on_ms_runtime);

        if (! worked)
            return false;

        if (! interrupt_speculation)
        {
            long time_diff =
                speculation_off_ms_runtime.get() - speculation_on_ms_runtime.get();
            // each event sleeps for 500 ms we start events staggered by 10 ms.
            // (pad to 11 to handle minor overheads)
            if (time_diff < (500 - 11*num_events_in_pipeline))
                return false;
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
        AtomicLong speculation_off_ms_runtime,
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

            
            long speculation_off_ms = pipelined_events_time_ms(
                endpt,false,amt_to_inc_by,interrupt_speculation_pattern);
            long speculation_on_ms = pipelined_events_time_ms(
                endpt,true,amt_to_inc_by,interrupt_speculation_pattern);

            if (speculation_off_ms_runtime != null)
                speculation_off_ms_runtime.set(speculation_off_ms);
            if (speculation_on_ms_runtime != null)
                speculation_on_ms_runtime.set(speculation_on_ms);
            
            double new_internal_number = endpt.get_number().doubleValue();
            double expected_internal_number =
                original_internal_number +
                2*interrupt_speculation_pattern.size()*amt_to_inc_by;
            
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
        private SpeculativeEndpoint endpt = null;
        private Double amt_to_inc_by = null;
        private boolean speculate;
        public boolean had_exception = false;
        private boolean interrupt_speculation = false;
        
        public SingleOp(
            SpeculativeEndpoint endpt, Double amt_to_inc_by,
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
        SpeculativeEndpoint endpt, boolean speculate,
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
            Thread.sleep(10);
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