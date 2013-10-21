package RalphCallResults;

public class EndpointCallError {
	
    /**
     * If we get an error in the midst of an endpoint call, then we
     return a subtype of this class into an endpoint's result queue.
    */
    String err;
	
    public EndpointCallError(String _err)
    {
        err = _err;
    }

}
