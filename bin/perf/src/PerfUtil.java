package performance;

public class PerfUtil
{
    public static class PerfClock
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
}