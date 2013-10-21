package RalphCallResults;

public class NetworkFailureCallResult extends MessageCallResultObject 
   implements HandleableCallResult
{
    private String trace = "";
	
    public NetworkFailureCallResult(String _trace)
    {
        trace = _trace;
    }
	

    @Override
    public String get_trace() 
    {
        return trace;
    }
}
