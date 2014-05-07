package emit_test_harnesses;

import ralph_emitted.AtomicListTestJava.TVarListEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class AtomicListTester
{
    public static void main(String[] args)
    {
        if (AtomicListTester.run_test())
            System.out.println("\nSUCCESS in AtomicListTester\n");
        else
            System.out.println("\nFAILURE in AtomicListTester\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            TVarListEndpoint endpt = new TVarListEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            double list_size = endpt.get_size().doubleValue();
            if (list_size != 0)
                return false;
            
            for (int i = 0; i < 20; ++i)
            {
                boolean contains =
                    endpt.contains_index(new Double((double)i)).booleanValue();
                if (contains)
                    return false;
            }

            // append to end of list
            for (int i = 5; i < 20; ++i)
            {
                // uses -1 in order to just append to end of list
                Double index_number = new Double(-1.0);
                Double value_number = new Double((double)i);
                endpt.put_number(index_number,value_number);
            }
            // insert into front of list
            for (int i = 0; i < 5; ++i)
            {
                Double index_number = new Double((double)i);
                Double value_number = new Double((double)i);
                endpt.put_number(index_number,value_number);

                double gotten = endpt.get_number(index_number).doubleValue();
                if (gotten != value_number.doubleValue())
                    return false;
            }

            // overwrite previous values with new values.
            for (int i = 0; i < 20; ++i)
            {
                Double index_number = new Double((double)i);
                Double value_number = new Double((double) i+ 30);
                endpt.put_number(index_number,value_number);

                double gotten = endpt.get_number(index_number).doubleValue();
                if (gotten != value_number.doubleValue())
                    return false;
            }

            // test that pass map through in method call as reference
            Double index =  new Double(0);
            Double new_value = new Double(5093);
            endpt.test_change_in_method_call(index,new_value);

            Double received_double = endpt.get_number(index);
            if (!received_double.equals(new_value))
                return false;

            // remove two values from beginning of list and insure that the
            // later values get moved down.  before remove, list had 20
            // elements.
            Double last_value = endpt.get_number(new Double(19));
            endpt.remove(new Double(1));
            
            // last index should now be in cell 18
            Double new_last_value = endpt.get_number(new Double(18));
            if (!last_value.equals(new_last_value))
                return false;
            
            // should not have affected first index
            received_double = endpt.get_number(new Double(0));
            if (!received_double.equals(new_value))
                return false;

            int int_list_size = endpt.get_size().intValue();
            if (int_list_size != 19)
                return false;

            // test clear list
            endpt.clear_list();
            list_size = endpt.get_size().doubleValue();
            if (list_size != 0.)
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