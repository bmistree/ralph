package RalphCallResults;

public class ApplicationExceptionCallResult
    extends MessageCallResultObject implements HandleableCallResult 
{
    private String trace = "";
    public ApplicationExceptionCallResult(String _trace)
    {
        trace = _trace;
    }
    @Override
    public String get_trace()
    {
        return trace;
    }
}
