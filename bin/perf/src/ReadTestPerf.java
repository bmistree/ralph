package performance;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;
import java.util.List;

import ralph.RalphGlobals;
import RalphConnObj.SingleSideConnection;

import performance.ReadTest.Tester;
import performance.IReadTestJava.IReadTest;

public class ReadTestPerf
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
            Tester endpt = new Tester(
                new RalphGlobals(rg_params),
                new SingleSideConnection());

            // warm up 
            for (int i = 0; i < params.reads_per_thread; ++i)
            {
                if (params.reads_atom_num)
                    endpt.read_atomic_number();
                if (params.reads_non_atom_num)
                    endpt.read_number();
                if (params.reads_atom_map)
                    endpt.read_atomic_map();
                if (params.reads_non_atom_map)
                    endpt.read_map();
            }

            List<IReadTest> endpt_list = new ArrayList<IReadTest>();
            for (int i =0; i < params.num_threads; ++i)
                endpt_list.add(endpt);
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