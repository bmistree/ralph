package java_lib_test;

public class VersionNumber
{
    protected static String test_name = "VersionNumber";

    public static void main(String [] args)
    {
        String prefix = "Test " + test_name;
        if (run_test())
            System.out.println(prefix + " SUCCEDED");
        else
            System.out.println(prefix + " FAILED");
    }

    public static boolean run_test()
    {
        return true;
    }
}