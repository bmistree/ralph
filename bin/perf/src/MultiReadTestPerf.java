package performance;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

import ralph.RalphGlobals;
import RalphConnObj.SingleSideConnection;
import performance.MultiReadTestJava.Tester;
import performance.IReadTestJava.IReadTest;

public class MultiReadTestPerf
{
    private final static AtomicBoolean had_exception =
        new AtomicBoolean(false);

    
    public static void main(String[] args)
    {
        ReadTestUtil.Parameters params = ReadTestUtil.get_parameters(args);
        try
        {
            RalphGlobals.Parameters rg_params =
                ReadTestUtil.ralph_params_from_read_test_params(
                    params);
            // set up experiment
            List<IReadTest> endpt_list = new ArrayList<IReadTest>();
            // keeping track of parent so can perform reads on shared
            // state.
            Tester master = null;
            RalphGlobals rg = new RalphGlobals(rg_params);
            for (int i = 0; i < params.num_threads; ++i)
            {
                Tester endpt = new Tester(
                    rg, new SingleSideConnection());
                
                if (master == null)
                    master = endpt;
                else
                    endpt.set_from_other(master);
                endpt_list.add(endpt);
            }

            if (master == null)
            {
                System.out.println("Must run with at least one thread");
                assert(false);
            }
            
            // warm up 
            for (int i = 0; i < params.reads_per_thread; ++i)
            {
                if (params.reads_atom_num)
                    master.read_atomic_number();
                if (params.reads_non_atom_num)
                    master.read_number();
                if (params.reads_atom_map)
                    master.read_atomic_map();
                if (params.reads_non_atom_map)
                    master.read_map();
            }

            ReadTestUtil.run_condition(endpt_list,params,had_exception);
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
}