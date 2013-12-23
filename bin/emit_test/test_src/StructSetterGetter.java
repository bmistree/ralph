package emit_test_harnesses;

import emit_test_package.StructTest.SetterGetter;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class StructSetterGetter
{
    public static void main(String[] args)
    {
        if (StructSetterGetter.run_test())
            System.out.println("\nSUCCESS in StructSetterGetter\n");
        else
            System.out.println("\nFAILURE in StructSetterGetter\n");
    }

    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            SetterGetter endpt = new SetterGetter(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            // initialize struct on endpoint
            endpt.initialize(new Double(0),"",new Boolean(false));
            
            // testing numbers
            double original_internal_number = endpt.get_number().doubleValue();
            for (int i = 0; i < 20; ++i)
            {
                double new_number = original_internal_number + ((double)1);
                endpt.set_number(new_number);
                double gotten_number = endpt.get_number().doubleValue();
                if (gotten_number != new_number)
                    return false;
            }

            // testing texts
            String original_internal_text = endpt.get_text();
            for (int i = 0; i < 20; ++i)
            {
                String new_text = original_internal_text + "hello";
                endpt.set_text(new_text);
                String gotten_text = endpt.get_text();
                if (! new_text.equals(gotten_text))
                    return false;
            }

            // testing tfs
            boolean original_internal_bool = endpt.get_tf().booleanValue();
            boolean new_boolean = original_internal_bool;
            for (int i = 0; i < 20; ++i)
            {
                new_boolean = ! new_boolean;
                endpt.set_tf(new_boolean);
                boolean gotten_boolean = endpt.get_tf().booleanValue();
                if (gotten_boolean != new_boolean)
                    return false;
            }
            
            // // testing creating a new struct and assigning to it.
            // new_number = 3.3;
            // new_text = "wow";
            // new_boolean = false;
            // endpt.new_struct(
            //     new Double(new_number), new_text, new Boolean( new_boolean));

            // double gotten_number = endpt.get_number().doubleValue();
            // if (gotten_number != new_number)
            //     return false;
            
            // String gotten_text = endpt.get_text();
            // if (! new_text.equals(gotten_text))
            //     return false;
            
            // boolean gotten_boolean = endpt.get_tf().booleanValue();
            // if (gotten_boolean != new_boolean)
            //     return false;
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}