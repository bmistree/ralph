package emit_test_harnesses;

import ralph_emitted.AtomicMapTest.TVarMapEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class AtomicMapTester
{
    public static void main(String[] args)
    {
        if (AtomicMapTester.run_test())
            System.out.println("\nSUCCESS in AtomicMapTester\n");
        else
            System.out.println("\nFAILURE in AtomicMapTester\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            TVarMapEndpoint endpt = new TVarMapEndpoint(
                new RalphGlobals(),
                new SingleSideConnection());

            double map_size = endpt.get_size().doubleValue();
            if (map_size != 0.)
                return false;

            for (int i = 0; i < 20; ++i)
            {
                boolean contains =
                    endpt.contains_index(new Double((double)i)).booleanValue();
                if (contains)
                    return false;
            }

            for (int i = 0; i < 20; ++i)
            {
                Double index_number = new Double((double)i);
                Double value_number = new Double((double)i);
                endpt.put_number(index_number,value_number);
                double gotten = endpt.get_number(index_number).doubleValue();
                if (gotten != value_number.doubleValue())
                    return false;
            }

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
            Double index =  new Double(23);
            Double new_value = new Double(5093);
            endpt.test_change_in_method_call(index,new_value);

            Double received_double = endpt.get_number(index);
            if (!received_double.equals(new_value))
                return false;
            
            // test remove
            endpt.remove(index);
            if (endpt.contains_index(index).booleanValue())
                return false;

            // test clear map
            endpt.clear_map();
            map_size = endpt.get_size().doubleValue();
            if (map_size != 0.)
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