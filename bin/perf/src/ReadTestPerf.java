package performance;

import performance.ReadTest.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class ReadTestPerf
{
    private final static int NUM_READS_TO_PERFORM_INDEX = 0;

    private static class MyClock
    {
        private final static double NANO_TO_S_MULTIPLIER = 1./1000000000.;
        
        private long start_time;
        public void tic()
        {
            start_time = System.nanoTime();
        }
        public double toc(int num_ops)
        {
            return toc(num_ops,null);
        }
        
        public double toc(int num_ops,String header)
        {
            long elapsed = System.nanoTime() - start_time;

            double ops_per_nano = ((double)num_ops)/((double) elapsed);
            double ops_per_s = ops_per_nano / NANO_TO_S_MULTIPLIER;
            if (header != null)
                System.out.println(header + " " + Double.toString(ops_per_s));

            return ops_per_s;
        }
    }
        
        
    
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

        MyClock clock = new MyClock();
        
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