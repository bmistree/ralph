package RalphCallResults;

public class ApplicationExceptionEndpointCallResult
    extends EndpointCallResultObject {
	
    private String trace;
	
    public ApplicationExceptionEndpointCallResult(String _trace)
    {
        trace = _trace;
    }

    public String get_trace() {
        return trace;
    }
}
