package emit_test_harnesses;

import ralph_emitted.AtomicListJava.TVarListEndpoint;
import ralph.RalphGlobals;

public class ManyListOps
{
    public static final int NUMBER_OPS_TO_RUN = 100000;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ManyListOps\n");
        else
            System.out.println("\nFAILURE in ManyListOps\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            TVarListEndpoint endpt =
                TVarListEndpoint.create_single_sided( new RalphGlobals());

            for (int i = 0; i < NUMBER_OPS_TO_RUN; ++i)
            {
                endpt.put_number(-1.,3.);
                endpt.remove(0.);
            }

            if (endpt.get_size().intValue() != 0)
                return false;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }
}