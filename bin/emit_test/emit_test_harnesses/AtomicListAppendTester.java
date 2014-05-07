package emit_test_harnesses;

import ralph_emitted.AtomicListAppendJava.TVarListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class AtomicListAppendTester
{
    private final static int NUM_APPENDS_TO_RUN = 20;
    
    public static void main(String[] args)
    {
        if (AtomicListAppendTester.run_test())
            System.out.println("\nSUCCESS in AtomicListAppendTester\n");
        else
            System.out.println("\nFAILURE in AtomicListAppendTester\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            TVarListEndpoint endpt = new TVarListEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            for (int i = 0; i < NUM_APPENDS_TO_RUN; ++i)
                endpt.append_number(new Double(i));

            if (endpt.get_size().doubleValue() != ((double) NUM_APPENDS_TO_RUN))
                return false;
            
            for (int i = 0; i < NUM_APPENDS_TO_RUN; ++i)
            {
                int internal_val = endpt.get_val_at_index((double)i).intValue();

                if (internal_val != i)
                    return false;
            }
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}