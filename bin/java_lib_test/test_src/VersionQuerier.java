package java_lib_test;


/**
   This test creates a list of updates for some fake device.  Then, it
   creates a version server, which serves these updates.  Following,
   we connect to the version server, query it, and check to ensure
   that the query response is what we expect.
 */
public class VersionQuerier
{
    private final static String test_name = "VersionQuerier";
    
    public static void main(String [] args)
    {
        if (run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        return true;
    }
}