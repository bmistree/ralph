package RalphServiceActions;

public class ReceivePartnerReadyAction extends ServiceAction {

    ralph.Endpoint local_endpoint = null;
	
    public ReceivePartnerReadyAction(ralph.Endpoint _local_endpoint)
    {
        local_endpoint = _local_endpoint;
    }
	
    @Override
    public void run() {
        // TODO Auto-generated method stub
        local_endpoint._other_side_ready();

    }

}
