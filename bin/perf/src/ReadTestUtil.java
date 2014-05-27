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

import ralph.RalphGlobals;
import ralph.UUIDGenerators;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import ralph.ThreadPool;

import performance.ReadTest.Tester;
import performance.PerfUtil.PerfClock;


public class ReadTestUtil
{
    // Which operations to benchmark
    private final static String ATOM_NUM_CMD_LINE = "atom_num_reads";
    private final static String ATOM_MAP_CMD_LINE = "atom_map_reads";
    private final static String NON_ATOM_NUM_CMD_LINE = "non_atom_num_reads";
    private final static String NON_ATOM_MAP_CMD_LINE = "non_atom_map_reads";

    // Experiment setup params
    private final static String READS_PER_THREAD_CMD_LINE = "reads_per_thread";
    private final static String NUM_THREADS_CMD_LINE = "num_threads";
    private final static String WOUND_WAIT_CMD_LINE = "wound_wait";
    private final static String MAX_THREAD_POOL_THREADS_CMD_LINE =
        "max_thread_pool_threads";
    private final static String PERSISTENT_THREAD_POOL_THREADS_CMD_LINE =
        "persistent_thread_pool_threads";
    private final static String UUID_GENERATOR_CMD_LINE =
        "uuid_generator";
    private final static String READS_ON_OTHER_ATOM_NUM_CMD_LINE =
        "reads_on_other_atom_num";
    
    // Display help
    private final static String HELP_CMD_LINE = "help";
    
    public static class Parameters
    {
        public final static int NOT_SET_SENTINEL = -1;
        public int reads_per_thread;
        public int num_threads;
        public boolean wound_wait = false;
        public boolean uuid_generator = false;
        
        public int persistent_thread_pool_threads = NOT_SET_SENTINEL;
        public int max_thread_pool_threads = NOT_SET_SENTINEL;

        public boolean reads_atom_num = false;
        public boolean reads_atom_map = false;
        public boolean reads_non_atom_num = false;
        public boolean reads_non_atom_map = false;
        public boolean reads_on_other_atom_num = false;
    }

    public static Parameters get_parameters(String [] args)
    {
        // SET UP ARGUMENT PARSER
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
        Option persistent_thread_pool_threads_option = 
            new Option(
                "p",PERSISTENT_THREAD_POOL_THREADS_CMD_LINE,true,
                "Number of persistent thread pool threads");
        Option max_thread_pool_threads_option = 
            new Option(
                "m",MAX_THREAD_POOL_THREADS_CMD_LINE,true,
                "Maximum number of thread pool threads");        
        Option uuid_generator_option =
            new Option(
                "u",UUID_GENERATOR_CMD_LINE,false,
                "Use real uuid generator.");
        Option atom_num_option =
            new Option(
                "an",ATOM_NUM_CMD_LINE,false,
                "Perform reads on atomic number.");
        Option atom_map_option =
            new Option(
                "am",ATOM_MAP_CMD_LINE,false,
                "Perform reads on atomic map.");        
        Option non_atom_num_option =
            new Option(
                "nan",NON_ATOM_NUM_CMD_LINE,false,
                "Perform reads on non-atomic number.");
        Option non_atom_map_option =
            new Option(
                "nam",NON_ATOM_MAP_CMD_LINE,false,
                "Perform reads on non-atomic map.");
        Option reads_on_other_atom_num_option =
            new Option(
                "oan",READS_ON_OTHER_ATOM_NUM_CMD_LINE,false,
                "Perform reads on other atomic number.");
        
        Options options = new Options();
        options.addOption(help_option);
        options.addOption(num_reads_per_thread_option);
        options.addOption(num_threads_option);
        options.addOption(wound_wait_option);
        options.addOption(persistent_thread_pool_threads_option);
        options.addOption(max_thread_pool_threads_option);
        options.addOption(uuid_generator_option);
        options.addOption(atom_num_option);
        options.addOption(atom_map_option);
        options.addOption(non_atom_num_option);
        options.addOption(non_atom_map_option);
        options.addOption(reads_on_other_atom_num_option);
        
        // PARSE ARGUMENTS
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

        // if help, ignore other arguments and return
        if (command_line.hasOption(HELP_CMD_LINE))
        {
            print_usage(options);
            System.exit(0);
        }

        // Check required arguments
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
        
        // SET PARAMETERS
        Parameters to_return = new Parameters();
        
        // get optional arguments        
        if (command_line.hasOption(WOUND_WAIT_CMD_LINE))
            to_return.wound_wait = true;
        if (command_line.hasOption(UUID_GENERATOR_CMD_LINE))
            to_return.uuid_generator = true;
        
        to_return.reads_per_thread =
            Integer.parseInt(
                command_line.getOptionValue(READS_PER_THREAD_CMD_LINE));
        to_return.num_threads =
            Integer.parseInt(command_line.getOptionValue(NUM_THREADS_CMD_LINE));

        if (command_line.hasOption(PERSISTENT_THREAD_POOL_THREADS_CMD_LINE))
        {
            to_return.persistent_thread_pool_threads =
                Integer.parseInt(
                    command_line.getOptionValue(
                        PERSISTENT_THREAD_POOL_THREADS_CMD_LINE));
        }
        if (command_line.hasOption(MAX_THREAD_POOL_THREADS_CMD_LINE))
        {
            to_return.persistent_thread_pool_threads =
                Integer.parseInt(
                    command_line.getOptionValue(
                        MAX_THREAD_POOL_THREADS_CMD_LINE));
        }

        // which experiment to run
        if (command_line.hasOption(ATOM_NUM_CMD_LINE))
            to_return.reads_atom_num = true;
        if (command_line.hasOption(READS_ON_OTHER_ATOM_NUM_CMD_LINE))
            to_return.reads_on_other_atom_num = true;
        if (command_line.hasOption(ATOM_MAP_CMD_LINE))
            to_return.reads_atom_map = true;
        if (command_line.hasOption(NON_ATOM_NUM_CMD_LINE))
            to_return.reads_non_atom_num = true;
        if (command_line.hasOption(NON_ATOM_MAP_CMD_LINE))
            to_return.reads_non_atom_map = true;
        
        return to_return;
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

    public static void run_condition(
        Tester endpt,Parameters params, AtomicBoolean had_exception)
        throws InterruptedException
    {
        // actually run
        if (params.reads_atom_num)
        {
            run_single_condition(
                endpt,ReadThreadType.ATOMIC_NUMBER_READ,params,
                had_exception);
        }
        if (params.reads_non_atom_num)
        {
            run_single_condition(
                endpt,ReadThreadType.NON_ATOMIC_NUMBER_READ,params,
                had_exception);
        }
        if (params.reads_atom_map)
        {
            run_single_condition(
                endpt,ReadThreadType.ATOMIC_MAP_READ,params,
                had_exception);
        }
        if (params.reads_non_atom_map)
        {
            run_single_condition(
                endpt,ReadThreadType.NON_ATOMIC_MAP_READ,params,
                had_exception);
        }
    }
    
    
    /**
       Run an experiment for a single condition: create a bunch of
       threads and then start them and join them.
     */
    private static void run_single_condition(
        Tester endpt, ReadThreadType thread_type,Parameters params,
        AtomicBoolean had_exception)
        throws InterruptedException
    {
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0; i < params.num_threads; ++i)
        {
            boolean perform_read_on_other_atom_num = false;
            if (params.reads_on_other_atom_num)
                perform_read_on_other_atom_num = (i % 2) == 0;
            threads.add(
                new ReadThread(
                    endpt,thread_type,params.reads_per_thread,
                    perform_read_on_other_atom_num,had_exception));
        }
        PerfClock clock = new PerfClock();    
        clock.tic();
        for (Thread t : threads)
            t.start();
        for (Thread t : threads)
            t.join();
        clock.toc(
            params.num_threads*params.reads_per_thread,
            thread_type.toString() + "\t");
        threads.clear();
    }

    
    public static RalphGlobals.Parameters ralph_params_from_read_test_params(
        Parameters read_test_params)
    {
        RalphGlobals.Parameters rg_params = new RalphGlobals.Parameters();
        if (read_test_params.wound_wait)
        {
            rg_params.deadlock_avoidance_algorithm =
                DeadlockAvoidanceAlgorithm.WOUND_WAIT;
        }
        if (read_test_params.uuid_generator)
        {
            rg_params.uuid_generator =
                UUIDGenerators.REAL_UUID_GENERATOR;
        }
        
        // check threadpool parameters
        ThreadPool.Parameters tp_params = new ThreadPool.Parameters();
        if (read_test_params.persistent_thread_pool_threads !=
            Parameters.NOT_SET_SENTINEL)
        {
            tp_params.persistent_num_threads =
                read_test_params.persistent_thread_pool_threads;
        }
        if (read_test_params.max_thread_pool_threads !=
            Parameters.NOT_SET_SENTINEL)
        {
            tp_params.max_num_threads =
                read_test_params.max_thread_pool_threads;
        }
        rg_params.threadpool_params = tp_params;

        return rg_params;
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
        private final boolean read_other_atom_num;
        private final AtomicBoolean had_exception;
        
        public ReadThread(
            Tester _endpt, ReadThreadType _read_thread_type, int _num_ops,
            boolean _read_other_atom_num, AtomicBoolean _had_exception)
        {
            endpt = _endpt;
            read_thread_type = _read_thread_type;
            num_ops = _num_ops;
            read_other_atom_num = _read_other_atom_num;
            had_exception = _had_exception;
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
                        if (read_other_atom_num)
                            endpt.read_other_atomic_number();
                        else
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