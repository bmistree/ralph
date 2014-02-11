package emit_test_harnesses;

import emit_test_package.BasicSpeculation.SpeculativeEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

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
        try
        {
            SpeculativeEndpoint endpt = new SpeculativeEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();

            // just check pipeline without speculation
            double amt_to_inc_by = 35.0;
            long speculation_off_ms = back_to_back_events_time_ms(
                endpt,false,amt_to_inc_by);
            long speculation_on_ms = back_to_back_events_time_ms(
                endpt,true,amt_to_inc_by);
            
            if ((speculation_off_ms - speculation_on_ms) < 400)
                return false;

            
            double new_internal_number = endpt.get_number().doubleValue();
            double expected_internal_number =
                original_internal_number + 4*amt_to_inc_by;
            
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
        
        public SingleOp(
            SpeculativeEndpoint endpt, Double amt_to_inc_by,
            boolean speculate)
        {
            this.endpt = endpt;
            this.amt_to_inc_by = amt_to_inc_by;
            this.speculate = speculate;
        }
        public void run()
        {
            try
            {
                endpt.pipeline(amt_to_inc_by,speculate);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception = true;
            }
        }
    }
    
    private static long back_to_back_events_time_ms(
        SpeculativeEndpoint endpt,boolean speculate,
        double amt_to_inc_by) throws Exception
    {
        long start = System.currentTimeMillis();
        SingleOp t = new SingleOp(endpt,amt_to_inc_by,speculate);
        t.start();
        Thread.sleep(10);

        SingleOp t2 = new SingleOp(endpt,amt_to_inc_by,speculate);
        t2.start();

        t.join();
        t2.join();

        if ((t.had_exception) || (t2.had_exception))
            throw new Exception();
        
        long end = System.currentTimeMillis();
        return end-start;
    }
}