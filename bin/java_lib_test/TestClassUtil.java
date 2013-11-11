package java_lib_test;

import ralph.LockedObject;
import ralph.LockedVariables;
import ralph.VariableStore;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.RalphGlobals;
import ralph.Endpoint;
import ralph.LockedActiveEvent;
import ralph.ExecutingEventContext;
import RalphConnObj.SingleSideConnection;
import RalphConnObj.SameHostConnection;
import RalphConnObj.ConnectionObj;

public class TestClassUtil
{    
    public static void print_success(String test_name)
    {
        System.out.println("Test " + test_name + " .....");
        System.out.println("    SUCCESS\n");
    }
    public static void print_failure(String test_name)
    {
        System.out.println("Test " + test_name + " .....");
        System.out.println("    FAILURE\n");
    }

    
    /**
       @returns {Endpoint} --- Contains variables in base stack frame
       with number tvar named NUM_TVAR_NAME and init val
       NUM_TVAR_INIT_VAL.
     */
    public static Endpoint create_default_single_endpoint()
    {
        return build_default_endpoint(new SingleSideConnection());
    }

    public static class DefaultEndpoint extends Endpoint
    {
        public static final String NUM_TVAR_NAME = "num_tvar";
        public static final Double NUM_TVAR_INIT_VAL = new Double (5);
        
        public DefaultEndpoint(
            RalphGlobals ralph_globals,String host_uuid,
            ConnectionObj conn_obj,VariableStore vstore)
        {
            super(ralph_globals,host_uuid,conn_obj,vstore);
        }

	public void _partner_endpoint_msg_func_call_prefix__waldo__test_partner_method(
            LockedActiveEvent active_event,ExecutingEventContext ctx) throws Exception
        {
            try{}
            catch (Exception _ex)
            {
                //# ApplicationExceptions should be backed
                //# out and the partner should be
                //# notified
                active_event.put_exception(_ex);
                throw _ex;
            }

            ctx.hide_sequence_completed_call(this, active_event);
        }

	public void _partner_endpoint_msg_func_call_prefix__waldo__test_partner_args_method(
            LockedActiveEvent active_event,ExecutingEventContext ctx,
            Object ... args) throws Exception

        {
            try
            {
                LockedObject<Double,Double> num_obj =
                    (LockedObject<Double,Double>)args[0];
                    
                LockedObject<Boolean,Boolean> bool_obj =
                    (LockedObject<Boolean,Boolean>) args[1];
                
                LockedObject<String,String> string_obj =
                    (LockedObject<String,String>) args[2];

                Double num = num_obj.get_val(active_event);
                num_obj.set_val(active_event,new Double(num.doubleValue() + 1));

                Boolean bool = bool_obj.get_val(active_event);
                bool_obj.set_val(active_event,new Boolean(! bool.booleanValue() ));

                String string = string_obj.get_val(active_event);
                string_obj.set_val(active_event,string + string);
            }
            catch (Exception _ex)
            {
                System.out.println(
                    "\nSerious error: caught error in partner args.\n\n");
                _ex.printStackTrace();
                
                //# ApplicationExceptions should be backed
                //# out and the partner should be
                //# notified
                active_event.put_exception(_ex);
                throw _ex;
            }

            ctx.hide_sequence_completed_call(this, active_event);
        }
    } // closes default endpoint

    
    private static DefaultEndpoint build_default_endpoint(
        ConnectionObj conn_obj)
    {
        String dummy_host_uuid = "dummy_host_uuid";

        VariableStore vstore = new VariableStore();
        
        // adding a number tvar
        vstore.add_var(
            DefaultEndpoint.NUM_TVAR_NAME,
            new LockedNumberVariable(
                dummy_host_uuid,false,
                DefaultEndpoint.NUM_TVAR_INIT_VAL));

        DefaultEndpoint to_return = new DefaultEndpoint(
            new RalphGlobals(),
            dummy_host_uuid,
            conn_obj,
            vstore);

        return to_return;

    }

    
    public static class ConnectedEndpointPair
    {
        public DefaultEndpoint endpta;
        public DefaultEndpoint endptb;
        public ConnectedEndpointPair(DefaultEndpoint _endpta, DefaultEndpoint _endptb)
        {
            endpta = _endpta;
            endptb = _endptb;
        }
    }
    public static ConnectedEndpointPair create_connected_endpoints()
    {
        SameHostConnection conn_obj = new SameHostConnection();
        DefaultEndpoint endpta = build_default_endpoint(conn_obj);
        DefaultEndpoint endptb = build_default_endpoint(conn_obj);
        return new ConnectedEndpointPair(endpta,endptb);
    }
}