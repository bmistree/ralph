package emit_test_harnesses;

import ralph_emitted.BasicRalphJava.SetterGetter;
import ralph_emitted.EndpointStructListJava.EndpointUser;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class EndpointStructListTest
{
    public static void main(String[] args)
    {
        if (EndpointStructListTest.run_test())
            System.out.println("\nSUCCESS in EndpointStructListTest\n");
        else
            System.out.println("\nFAILURE in EndpointStructListTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            RalphGlobals ralph_globals = new RalphGlobals();
            SetterGetter setter_getter_endpt = new SetterGetter(
                ralph_globals, new SingleSideConnection());

            EndpointUser endpt = new EndpointUser(
                ralph_globals, new SingleSideConnection());
            endpt.add_endpoint(setter_getter_endpt);
            
            // testing numbers
            double original_internal_number = endpt.get_number_on_index(0.0).doubleValue();
            for (int i = 0; i < 20; ++i)
            {
                double new_number = original_internal_number + ((double)1);
                endpt.set_number_on_index(0.0,new_number);
                double gotten_number = endpt.get_number_on_index(0.0).doubleValue();
                if (gotten_number != new_number)
                    return false;
            }

            // testing texts
            String original_internal_text = endpt.get_text_on_index(0.0);
            for (int i = 0; i < 20; ++i)
            {
                String new_text = original_internal_text + "hello";
                endpt.set_text_on_index(0.0,new_text);
                String gotten_text = endpt.get_text_on_index(0.0);
                if (! new_text.equals(gotten_text))
                    return false;
            }

            // testing tfs
            boolean original_internal_bool = endpt.get_tf_on_index(0.0).booleanValue();
            boolean new_boolean = original_internal_bool;
            for (int i = 0; i < 20; ++i)
            {
                new_boolean = ! new_boolean;
                endpt.set_tf_on_index(0.0,new_boolean);
                boolean gotten_boolean = endpt.get_tf_on_index(0.0).booleanValue();
                if (gotten_boolean != new_boolean)
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