package emit_test_harnesses;

import ralph_emitted.MultipleInterfacesJava.SetterGetter;
import ralph_emitted.MultipleInterfacesJava.INumberSetterGetter;
import ralph_emitted.MultipleInterfacesJava.ITextSetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class MultipleInterfacesTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in MultipleInterfacesTest\n");
        else
            System.out.println("\nFAILURE in MultipleInterfacesTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            SetterGetter endpt = new SetterGetter(
                new RalphGlobals(),
                new SingleSideConnection());

            // testing numbers
            INumberSetterGetter num_endpt = (INumberSetterGetter)endpt;
            double original_internal_number = num_endpt.get_number().doubleValue();
            for (int i = 0; i < 20; ++i)
            {
                double new_number = original_internal_number + ((double)1);
                num_endpt.set_number(new_number);
                double gotten_number = num_endpt.get_number().doubleValue();
                if (gotten_number != new_number)
                    return false;
            }

            // testing texts
            ITextSetterGetter text_endpt = (ITextSetterGetter)endpt;
            String original_internal_text = endpt.get_text();
            for (int i = 0; i < 20; ++i)
            {
                String new_text = original_internal_text + "hello";
                text_endpt.set_text(new_text);
                String gotten_text = text_endpt.get_text();
                if (! new_text.equals(gotten_text))
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