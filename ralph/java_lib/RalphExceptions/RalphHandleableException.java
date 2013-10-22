package RalphExceptions;

public class RalphHandleableException extends Exception {

    public String trace;
	
    public RalphHandleableException(String _trace)
    {
        trace = _trace;
    }
}
