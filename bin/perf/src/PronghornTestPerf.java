package performance;

import performance.PronghornInstance.Pronghorn;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import performance.PerfUtil.PerfClock;

public class PronghornTestPerf
{
    private final static int NUM_OPS_TO_PERFORM_INDEX = 0;

    public static void main(String[] args)
    {

        if (args.length != 1)
        {
            System.out.println(
                "PronghornTestPerf requires an argument for number " +
                "of writes to run.");
            assert(false);
        }
        
        int num_ops_to_perform =
            Integer.parseInt(args[NUM_OPS_TO_PERFORM_INDEX]);

        PerfClock clock = new PerfClock();
        
        try
        {
            Pronghorn endpt = new Pronghorn(
                new RalphGlobals(),
                new SingleSideConnection());

            String switch_id = "some_switch";
            endpt.add_switch(switch_id);
            
            for (int i = 0; i < num_ops_to_perform; ++i)
                endpt.single_op(switch_id);
            
            // non-atomic number writes
            clock.tic();
            for (int i = 0; i < num_ops_to_perform; ++i)
                endpt.single_op(switch_id);
            clock.toc(num_ops_to_perform,"Switch op:\t");
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            assert(false);
        }
    }
}