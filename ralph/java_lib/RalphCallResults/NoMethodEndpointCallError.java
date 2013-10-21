package RalphCallResults;

public class NoMethodEndpointCallError extends EndpointCallError{
    public NoMethodEndpointCallError(String func_name)
    {
        super("No func name named " + func_name + " on endpoint.");
    }
}
