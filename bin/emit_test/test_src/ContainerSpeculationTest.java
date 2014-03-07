package emit_test_harnesses;

import emit_test_package.ContainerSpeculationJava.ContainerSpeculation;
import emit_test_package.BasicSpeculation.SpeculativeInterface;
import ralph.EndpointConstructorObj;

public class ContainerSpeculationTest
{
    public static void main(String[] args)
    {
        if (ContainerSpeculationTest.run_test())
            System.out.println("\nSUCCESS in ContainerSpeculationTest\n");
        else
            System.out.println("\nFAILURE in ContainerSpeculationTest\n");
    }
    
    public static boolean run_test()
    {
        EndpointConstructorObj constructor_obj = ContainerSpeculation.factory;
        return BasicSpeculationTest.all_unmixed_speculation_uninterrupted(constructor_obj) &&
            BasicSpeculationTest.all_unmixed_speculation_interrupted(constructor_obj) &&
            BasicSpeculationTest.all_mixed_speculation_tests(constructor_obj);
    }
}