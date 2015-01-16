package emit_test_harnesses;

import ralph_emitted.DirectStructWrappedEndpointJava.OuterService;
import ralph.RalphGlobals;


public class DirectStructWrappedEndpoint
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in DirectStructWrappedEndpoint\n");
        else
            System.out.println("\nFAILURE in DirectStructWrappedEndpoint\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            OuterService endpt = OuterService.create_single_sided(new RalphGlobals());

            for (int i = 0; i < 100; ++i)
                endpt.increment_and_return_number();
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}
