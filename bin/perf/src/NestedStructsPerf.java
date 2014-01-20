package performance;

import performance.NestedStructs.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import performance.PerfUtil.PerfClock;

public class NestedStructsPerf
{
    private final static int NUM_OPS_TO_PERFORM_INDEX = 0;

    public static void main(String[] args)
    {

        if (args.length != 1)
        {
            System.out.println(
                "NestedStructsTest requires an argument for number " +
                "of ops to run.");
            assert(false);
        }
        
        int num_ops_to_perform =
            Integer.parseInt(args[NUM_OPS_TO_PERFORM_INDEX]);

        PerfClock clock = new PerfClock();
        
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";
            Tester endpt = new Tester(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            // warm up
            for (int i = 0; i < num_ops_to_perform; ++i)
            {
                endpt.create_external_struct();
                endpt.map_insert();
            }


            // create external struct
            clock.tic();
            for (int i = 0; i < num_ops_to_perform; ++i)
                endpt.create_external_struct();
            clock.toc(num_ops_to_perform,"External struct creation:\t");

            // map insert
            clock.tic();
            for (int i = 0; i < num_ops_to_perform; ++i)
                endpt.map_insert();
            clock.toc(num_ops_to_perform,"Map insertion:\t\t");
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            assert(false);
        }
    }
}