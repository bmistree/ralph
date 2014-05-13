package performance;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import performance.ReadTest.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import performance.PerfUtil.PerfClock;


public class ReadTestPerf
{
    private final static int NUM_READS_PER_THREAD_INDEX = 0;
    private final static int NUM_THREADS_INDEX = 1;
    private final static AtomicBoolean had_exception = new AtomicBoolean(false);
    
    public static void main(String[] args)
    {
        if (args.length != 2)
        {
            System.out.println(
                "ReadTest requires 2 arguments: 1) num reads per thread; " +
                "2) num threads");
            assert(false);
        }
        
        int num_reads_per_thread =
            Integer.parseInt(args[NUM_READS_PER_THREAD_INDEX]);
        int num_threads = 
            Integer.parseInt(args[NUM_THREADS_INDEX]);
        
        PerfClock clock = new PerfClock();
        
        try
        {
            Tester endpt = new Tester(
                new RalphGlobals(),
                new SingleSideConnection());

            // warm up 
            for (int i = 0; i < num_reads_per_thread; ++i)
            {
                endpt.read_number();
                endpt.read_atomic_number();
                endpt.read_map();
                endpt.read_atomic_map();
            }

            List<Thread> threads = new ArrayList<Thread>();
            
            // non-atomic number reads
            for (int i = 0; i < num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.NON_ATOMIC_NUMBER_READ,
                        num_reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(num_threads*num_reads_per_thread,"Non-atomic number read:\t");
            threads.clear();
            
            // atomic number reads
            for (int i = 0; i < num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.ATOMIC_NUMBER_READ,
                        num_reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(num_threads*num_reads_per_thread,"Atomic number read:\t\t");
            threads.clear();
            
            // non-atomic map reads
            for (int i = 0; i < num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.NON_ATOMIC_MAP_READ,
                        num_reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(num_threads*num_reads_per_thread,"Non-atomic map read:\t");
            threads.clear();
            
            // atomic map reads
            for (int i = 0; i < num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.ATOMIC_MAP_READ,
                        num_reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(num_threads*num_reads_per_thread,"Atomic map read:\t\t");
            threads.clear();
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            assert(false);
        }
        if (had_exception.get())
        {
            System.out.println(
                "\n\nWarning: times may be garbage, had an exception\n\n");
        }
    }
    
    public enum ReadThreadType
    {
        ATOMIC_NUMBER_READ, NON_ATOMIC_NUMBER_READ,
        ATOMIC_MAP_READ,NON_ATOMIC_MAP_READ
    }
    
    public static class ReadThread extends Thread
    {
        private final Tester endpt;
        private final ReadThreadType read_thread_type;
        private final int num_ops;
        
        public ReadThread(
            Tester _endpt, ReadThreadType _read_thread_type, int _num_ops)
        {
            endpt = _endpt;
            read_thread_type = _read_thread_type;
            num_ops = _num_ops;
        }

        @Override
        public void run()
        {
            try
            {
                for (int i = 0; i < num_ops; ++i)
                {
                    switch (read_thread_type)
                    {
                    case ATOMIC_NUMBER_READ:
                        endpt.read_atomic_number();
                        break;
                    case NON_ATOMIC_NUMBER_READ:
                        endpt.read_number();
                        break;
                    case ATOMIC_MAP_READ:
                        endpt.read_atomic_map();
                        break;
                    case NON_ATOMIC_MAP_READ:
                        endpt.read_map();
                        break;
                    default:
                        System.out.println("\nUnknown read type\n");
                        assert(false);
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                had_exception.set(true);
                assert(false);
            }
        }
    }
}