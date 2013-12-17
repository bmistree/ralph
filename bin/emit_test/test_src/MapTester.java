package emit_test_harnesses;

import emit_test_package.MapTest.MapEndpoint;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;

public class MapTester
{
    public static void main(String[] args)
    {
        if (MapTester.run_test())
            System.out.println("\nSUCCESS in MapTester\n");
        else
            System.out.println("\nFAILURE in MapTester\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";

            MapEndpoint endpt = new MapEndpoint(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            double map_size = endpt.get_size().doubleValue();
            if (map_size != 0)
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
            
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }
}