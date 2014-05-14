package performance;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import performance.ReadTest.Tester;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import performance.PerfUtil.PerfClock;


public class ReadTestPerf
{
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);

    private final static String READS_PER_THREAD_CMD_LINE = "reads_per_thread";
    private final static String NUM_THREADS_CMD_LINE = "num_threads";
    private final static String WOUND_WAIT_CMD_LINE = "wound_wait";
    private final static String HELP_CMD_LINE = "help";
    
    private static class Parameters
    {
        public int reads_per_thread;
        public int num_threads;
        public boolean wound_wait;
    }

    private static Parameters get_parameters(String [] args)
    {
        Option help_option =
            new Option(
                "h","help",false,"Help");
        Option num_reads_per_thread_option =
            new Option(
                "r",READS_PER_THREAD_CMD_LINE,true,"Number of reads per thread");
        Option num_threads_option = 
            new Option("t",NUM_THREADS_CMD_LINE,true,"Number of threads");
        Option wound_wait_option = 
            new Option(
                "w",WOUND_WAIT_CMD_LINE,false,
                "Specify if should use wound-wait");

        Options options = new Options();
        options.addOption(help_option);
        options.addOption(num_reads_per_thread_option);
        options.addOption(num_threads_option);
        options.addOption(wound_wait_option);
        GnuParser parser = new GnuParser();
        CommandLine command_line = null;
        try
        {
            command_line = parser.parse(options,args);
        }
        catch (ParseException parse_excep)
        {
            parse_excep.printStackTrace();
            print_usage(options);
            System.exit(0);
        }
        
        if (command_line.hasOption(HELP_CMD_LINE))
        {
            print_usage(options);
            System.exit(0);
        }
        if (! command_line.hasOption(READS_PER_THREAD_CMD_LINE))
        {
            print_usage(options,READS_PER_THREAD_CMD_LINE);
            System.exit(0);
        }
        if (! command_line.hasOption(NUM_THREADS_CMD_LINE))
        {
            print_usage(options,NUM_THREADS_CMD_LINE);
            System.exit(0);
        }

        Parameters to_return = new Parameters();
        to_return.wound_wait = false;
        if (command_line.hasOption(WOUND_WAIT_CMD_LINE))
            to_return.wound_wait = true;
        to_return.reads_per_thread =
            Integer.parseInt(
                command_line.getOptionValue(READS_PER_THREAD_CMD_LINE));
        to_return.num_threads =
            Integer.parseInt(command_line.getOptionValue(NUM_THREADS_CMD_LINE));

        return to_return;
    }
    
    public static void main(String[] args)
    {
        Parameters params = get_parameters(args);
        
        PerfClock clock = new PerfClock();
        try
        {
            RalphGlobals.Parameters rg_params = new RalphGlobals.Parameters();
            if (params.wound_wait)
            {
                rg_params.deadlock_avoidance_algorithm =
                    DeadlockAvoidanceAlgorithm.WOUND_WAIT;
            }
            
            Tester endpt = new Tester(
                new RalphGlobals(rg_params),
                new SingleSideConnection());

            // warm up 
            for (int i = 0; i < params.reads_per_thread; ++i)
            {
                endpt.read_number();
                endpt.read_atomic_number();
                endpt.read_map();
                endpt.read_atomic_map();
            }

            List<Thread> threads = new ArrayList<Thread>();
            
            // non-atomic number reads
            for (int i = 0; i < params.num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.NON_ATOMIC_NUMBER_READ,
                        params.reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(
                params.num_threads*params.reads_per_thread,
                "Non-atomic number read:\t");
            threads.clear();
            
            // atomic number reads
            for (int i = 0; i < params.num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.ATOMIC_NUMBER_READ,
                        params.reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(
                params.num_threads*params.reads_per_thread,
                "Atomic number read:\t");
            threads.clear();
            
            // non-atomic map reads
            for (int i = 0; i < params.num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.NON_ATOMIC_MAP_READ,
                        params.reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(
                params.num_threads*params.reads_per_thread,
                "Non-atomic map read:\t");
            threads.clear();
            
            // atomic map reads
            for (int i = 0; i < params.num_threads; ++i)
            {
                threads.add(
                    new ReadThread(
                        endpt,ReadThreadType.ATOMIC_MAP_READ,
                        params.reads_per_thread));
            }
            clock.tic();
            for (Thread t : threads)
                t.start();
            for (Thread t : threads)
                t.join();
            clock.toc(
                params.num_threads*params.reads_per_thread,
                "Atomic map read:\t");
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
    private static void print_usage(Options options)
    {
        print_usage(options,null);
    }
        
    private static void print_usage(Options options, String missing_arg)
    {
        String header = "";
        if (missing_arg != null)
            header = "Missing required argument: " + missing_arg;
        
        HelpFormatter help_formatter = new HelpFormatter();
        help_formatter.printHelp("read_perf",header,options,"",true);
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