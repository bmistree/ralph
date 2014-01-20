package performance;

import performance.WriteTest.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import performance.PerfUtil.PerfClock;

public class WriteTestPerf
{
    private final static int NUM_WRITES_TO_PERFORM_INDEX = 0;

    public static void main(String[] args)
    {

        if (args.length != 1)
        {
            System.out.println(
                "WriteTest requires an argument for number " +
                "of writes to run.");
            assert(false);
        }
        
        int num_writes_to_perform =
            Integer.parseInt(args[NUM_WRITES_TO_PERFORM_INDEX]);

        PerfClock clock = new PerfClock();
        
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";
            Tester endpt = new Tester(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());


            // non-atomic number writes
            clock.tic();
            for (int i = 0; i < num_writes_to_perform; ++i)
                endpt.write_number();
            clock.toc(num_writes_to_perform,"Non-atomic number write:\t");

            // non-atomic number writes
            clock.tic();
            for (int i = 0; i < num_writes_to_perform; ++i)
                endpt.write_atomic_number();
            clock.toc(num_writes_to_perform,"Atomic number write:\t");
            
            // non-atomic map writes
            clock.tic();
            for (int i = 0; i < num_writes_to_perform; ++i)
                endpt.write_map();
            clock.toc(num_writes_to_perform,"Non-atomic map write:\t");
            
            // atomic map writes
            clock.tic();
            for (int i = 0; i < num_writes_to_perform; ++i)
                endpt.write_atomic_map();
            clock.toc(num_writes_to_perform,"Atomic map write:\t\t");
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            assert(false);
        }
    }
}