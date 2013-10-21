package WaldoCallResults;


public class EndpointCompleteCallResult extends EndpointCallResultObject 
{
	public Object result;
	
	/**
	 *  When an active event issues an endpoint call, it blocks on reading a
		threadsafe queue.  If the invalidation event has been backed out
		before the endpoint call completes, we put a
		_BackoutBeforeEndpointCallResult into the queue so that the event
		knows not to perform any additional work.  Otherwise, put an
		_EndpointCallResult in, which contains the returned values.
	 * @param result
	 */
	public EndpointCompleteCallResult (Object _result)
	{
		result = _result;
	}
}
