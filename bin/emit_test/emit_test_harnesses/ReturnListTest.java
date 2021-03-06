package emit_test_harnesses;

import ralph_emitted.ReturnListJava.ListEndpoint;
import ralph.RalphGlobals;
import ralph.NonAtomicInternalList;

public class ReturnListTest
{
    public static void main(String[] args)
    {
        if (ReturnListTest.run_test())
            System.out.println("\nSUCCESS in ReturnListTest\n");
        else
            System.out.println("\nFAILURE in ReturnListTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            ListEndpoint endpt =
                ListEndpoint.create_single_sided(new RalphGlobals());

            NonAtomicInternalList<String,String> result =
                endpt.test_return_list();

            int list_size = result.get_len(null);
            if (list_size != 3)
                return false;

            String val = result.get_val_on_key(null,new Double(0));
            if (! val.equals("hello"))
                return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}