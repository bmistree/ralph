package java_lib_test;

import ralph.RalphObject;
import ralph.Variables;
import ralph.VariableStore;
import ralph.Variables.AtomicNumberVariable;
import ralph.RalphGlobals;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.ExecutingEventContext;
import RalphConnObj.SingleSideConnection;
import RalphConnObj.SameHostConnection;
import RalphConnObj.ConnectionObj;

import ralph.Util;
import java.util.concurrent.ArrayBlockingQueue;

import RalphExceptions.ApplicationException;
import RalphExceptions.BackoutException;
import RalphExceptions.NetworkException;
import RalphExceptions.StoppedException;

import java.util.HashMap;
import ralph.Variables.AtomicMapVariable;
import ralph.Variables.AtomicListVariable;
import ralph.NonAtomicInternalMap;
import RalphAtomicWrappers.BaseAtomicWrappers;


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
        public static final String MAP_TVAR_NAME = "map_tvar";
        public static final String LIST_TVAR_NAME = "list_tvar";
        
        
        public DefaultEndpoint(
            RalphGlobals ralph_globals,
            ConnectionObj conn_obj,VariableStore vstore)
        {
            super(ralph_globals,conn_obj,vstore);
        }

        protected void _handle_rpc_call(
            String to_exec_internal_name,ActiveEvent active_event,
            ExecutingEventContext ctx,
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
                RalphObject<Double,Double> arg =
                    (RalphObject<Double,Double>)args[0];

                _test_increment_local_num(active_event,ctx,arg);
            }
            else if (to_exec_internal_name.equals("test_partner_args_method"))
            {
                RalphObject<Double,Double> num_obj =
                    (RalphObject<Double,Double>)args[0];

                RalphObject<Boolean,Boolean> bool_obj =
                    (RalphObject<Boolean,Boolean>) args[1];

                RalphObject<String,String> string_obj =
                    (RalphObject<String,String>) args[2];

                _test_partner_args_method(
                    active_event, ctx, num_obj,bool_obj,string_obj);
            }
            else
            {
                Util.logger_assert(
                    "Error handling rpc call: unknown method " +
                    to_exec_internal_name);
            }
        }

        
	public void _test_partner_method(
            ActiveEvent active_event,ExecutingEventContext ctx)
        {
        }

	public void _test_increment_local_num(
            ActiveEvent active_event,ExecutingEventContext ctx,
            RalphObject<Double,Double>to_return)
            throws BackoutException
        {
            RalphObject<Double,Double> num_obj =
                (RalphObject<Double,Double>)ctx.var_stack.get_var_if_exists(
                    NUM_TVAR_NAME);            
            
            double current_val =
                ((Double)num_obj.get_val(active_event)).doubleValue();
            Double new_val = new Double( current_val + 1);
            num_obj.set_val(active_event,new_val);

            to_return.set_val(active_event,new_val);
        }

        
	public void _test_partner_args_method(
            ActiveEvent active_event,ExecutingEventContext ctx,
            RalphObject<Double,Double> num_obj,
            RalphObject<Boolean,Boolean> bool_obj,
            RalphObject<String,String> string_obj)
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
        VariableStore vstore = new VariableStore(false);
        
        // adding a number tvar
        vstore.add_var(
            DefaultEndpoint.NUM_TVAR_NAME,
            new AtomicNumberVariable(
                false,
                DefaultEndpoint.NUM_TVAR_INIT_VAL));

        vstore.add_var(
            DefaultEndpoint.MAP_TVAR_NAME,
            new AtomicMapVariable<Double,Double,Double>(
                false,
                NonAtomicInternalMap.IndexType.DOUBLE,
                BaseAtomicWrappers.ATOMIC_NUMBER_WRAPPER));

        vstore.add_var(
            DefaultEndpoint.LIST_TVAR_NAME,
            new AtomicListVariable<Double,Double>(
                false,
                BaseAtomicWrappers.ATOMIC_NUMBER_WRAPPER));

        
        DefaultEndpoint to_return = new DefaultEndpoint(
            new RalphGlobals(),
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