package ralph;

public interface IEndpointMap
{
    public void add_endpoint(Endpoint endpoint);
    public void remove_endpoint_if_exists(Endpoint endpoint);
    public Endpoint get_endpoint_if_exists(String uuid);
}