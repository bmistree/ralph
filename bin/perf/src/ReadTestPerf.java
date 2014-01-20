package performance;

import performance.ReadTest.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import performance.PerfUtil.PerfClock;

public class ReadTestPerf
{
    private final static int NUM_READS_TO_PERFORM_INDEX = 0;

    public static void main(String[] args)
    {

        if (args.length != 1)
        {
            System.out.println(
                "ReadTest requires an argument for number " +
                "of reads to run.");
            assert(false);
        }
        
        int num_reads_to_perform =
            Integer.parseInt(args[NUM_READS_TO_PERFORM_INDEX]);

        PerfClock clock = new PerfClock();
        
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";
            Tester endpt = new Tester(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());


            // non-atomic number reads
            clock.tic();
            for (int i = 0; i < num_reads_to_perform; ++i)
                endpt.read_number();
            clock.toc(num_reads_to_perform,"Non-atomic number read:\t");

            // non-atomic number reads
            clock.tic();
            for (int i = 0; i < num_reads_to_perform; ++i)
                endpt.read_atomic_number();
            clock.toc(num_reads_to_perform,"Atomic number read:\t\t");
            
            // non-atomic map reads
            clock.tic();
            for (int i = 0; i < num_reads_to_perform; ++i)
                endpt.read_map();
            clock.toc(num_reads_to_perform,"Non-atomic map read:\t");
            
            // atomic map reads
            clock.tic();
            for (int i = 0; i < num_reads_to_perform; ++i)
                endpt.read_atomic_map();
            clock.toc(num_reads_to_perform,"Atomic map read:\t\t");
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            assert(false);
        }
    }
}