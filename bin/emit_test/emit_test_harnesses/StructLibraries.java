package emit_test_harnesses;

import ralph_emitted.StructLibUserJava.StructUser;
import ralph.RalphGlobals;

public class StructLibraries
{
    public static void main(String[] args)
    {
        if (StructLibraries.run_test())
            System.out.println("\nSUCCESS in StructLibraries\n");
        else
            System.out.println("\nFAILURE in StructLibraries\n");
    }

    public static boolean run_test()
    {
        try
        {
            StructUser endpt =
                StructUser.create_single_sided(new RalphGlobals());

            // testing numbers
            String text_to_set = "hello";
            endpt.set_struct_text(text_to_set);

            // should get out same text put in.
            if (! endpt.get_struct_text().equals(text_to_set))
                return false;

            double num_times_to_append = 35;
            endpt.recursive_append_struct_to_list(num_times_to_append);
            if (!endpt.size_struct_list().equals(num_times_to_append))
                return false;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        
        return true;
    }
}