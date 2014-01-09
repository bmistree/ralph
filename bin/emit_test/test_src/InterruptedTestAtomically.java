package emit_test_harnesses;

import emit_test_package.InterruptedAtomically.InterruptedAtomic;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class InterruptedTestAtomically
{
    public static void main(String[] args)
    {
        if (InterruptedTestAtomically.run_test())
            System.out.println("\nSUCCESS in InterruptedTestAtomically\n");
        else
            System.out.println("\nFAILURE in InterruptedTestAtomically\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            InterruptedAtomic endpt = new InterruptedAtomic(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            int NUM_TO_INITIALLY_APPEND = 500;
            for (int i = 0; i < NUM_TO_INITIALLY_APPEND; ++i)
            {
                double diff = endpt.long_event_and_append().doubleValue();
                if (diff != 0.)
                    return false;
            }

            if (! endpt.list_size().equals((double)NUM_TO_INITIALLY_APPEND))
                return false;
            
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }

        return true;
    }
}