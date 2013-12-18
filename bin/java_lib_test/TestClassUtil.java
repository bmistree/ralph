package java_lib_test;

import ralph.LockedObject;
import ralph.LockedVariables;
import ralph.VariableStore;
import ralph.LockedVariables.LockedNumberVariable;
import ralph.RalphGlobals;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.ExecutingEventContext;
import RalphConnObj.SingleSideConnection;
import RalphConnObj.SameHostConnection;
import RalphConnObj.ConnectionObj;

import ralph.Util;
import RalphCallResults.BackoutBeforeEndpointCallResult;
import RalphCallResults.EndpointCompleteCallResult;
import java.util.concurrent.ArrayBlockingQueue;
import RalphCallResults.EndpointCallResultObject;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;


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


        protected void _handle_rpc_call(
            String to_exec_internal_name,ActiveEvent active_event,
            ExecutingEventContext ctx,
            ArrayBlockingQueue<EndpointCallResultObject> result_queue,
            Object...args)
            throws ApplicationException, BackoutException, NetworkException,
            StoppedException
        {
            Object result = null;
            if (to_exec_internal_name.equals("test_partner_method"))
            {
                _test_partner_method(active_event,ctx);
            }
            else if (to_exec_internal_name.equals("test_increment_local_num"))
            {
                LockedObject<Double,Double> arg =
                    (LockedObject<Double,Double>)args[0];

                _test_increment_local_num(active_event,ctx,arg);
            }
            else if (to_exec_internal_name.equals("test_partner_args_method"))
            {
                LockedObject<Double,Double> num_obj =
                    (LockedObject<Double,Double>)args[0];

                LockedObject<Boolean,Boolean> bool_obj =
                    (LockedObject<Boolean,Boolean>) args[1];

                LockedObject<String,String> string_obj =
                    (LockedObject<String,String>) args[2];

                _test_partner_args_method(
                    active_event, ctx, num_obj,bool_obj,string_obj);
            }
            else
            {
                Util.logger_assert(
                    "Error handling rpc call: unknown method " +
                    to_exec_internal_name);
            }

            if (result_queue == null)
                return;

            boolean completed = active_event.wait_if_modified_peered();
            if (! completed)
            {
                result_queue.add(
                    new RalphCallResults.BackoutBeforeEndpointCallResult());
            }
            else
            {
                result_queue.add(new EndpointCompleteCallResult(result));
            }
        }

        
	public void _test_partner_method(
            ActiveEvent active_event,ExecutingEventContext ctx)
        {
        }

	public void _test_increment_local_num(
            ActiveEvent active_event,ExecutingEventContext ctx,
            LockedObject<Double,Double>to_return)
            throws BackoutException
        {
            LockedObject<Double,Double> num_obj =
                (LockedObject<Double,Double>)ctx.var_stack.get_var_if_exists(
                    NUM_TVAR_NAME);

            double current_val =
                ((Double)num_obj.get_val(active_event)).doubleValue();
            Double new_val = new Double( current_val + 1);
            num_obj.set_val(active_event,new_val);

            to_return.set_val(active_event,new_val);
        }

        
	public void _test_partner_args_method(
            ActiveEvent active_event,ExecutingEventContext ctx,
            LockedObject<Double,Double> num_obj,
            LockedObject<Boolean,Boolean> bool_obj,
            LockedObject<String,String> string_obj)
            throws BackoutException
        {
            Double num = num_obj.get_val(active_event);
            num_obj.set_val(active_event,new Double(num.doubleValue() + 1));

            Boolean bool = bool_obj.get_val(active_event);
            bool_obj.set_val(active_event,new Boolean(! bool.booleanValue() ));

            String string = string_obj.get_val(active_event);
            string_obj.set_val(active_event,string + string);
        }
    } // closes default endpoint

    
    private static DefaultEndpoint build_default_endpoint(
        ConnectionObj conn_obj)
    {
        String dummy_host_uuid = "dummy_host_uuid";

        VariableStore vstore = new VariableStore(false);
        
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