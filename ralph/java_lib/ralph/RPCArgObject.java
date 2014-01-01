package ralph;


public class RPCArgObject<T,D>
{
    public RalphObject<T,D> arg_to_pass;
    public boolean is_reference;

    public RPCArgObject(RalphObject<T,D> _arg_to_pass, boolean _is_reference)
    {
        arg_to_pass = _arg_to_pass;
        is_reference = _is_reference;
    }

}