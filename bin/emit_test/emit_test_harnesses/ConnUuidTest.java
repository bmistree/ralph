package emit_test_harnesses;

import java.util.List;
import java.util.ArrayList;

import ralph_emitted.ConnUuidJava.ConnUuid;
import ralph.RalphGlobals;
import ralph.Ralph;

public class ConnUuidTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ConnUuidTest\n");
        else
            System.out.println("\nFAILURE in ConnUuidTest\n");
    }
    
    public static boolean run_test()
    {
        try
        {
            return linear_test(20, 25555);
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    public static boolean linear_test(int num_hosts, int base_port) throws Exception
    {
        List<GlobalsConnPair> all_conns = new ArrayList<GlobalsConnPair>();
        for (int i = 0; i < num_hosts; ++i)
            all_conns.add(GlobalsConnPair.generate(base_port + i));

        // generate connections: every other endpoint generates a
        // connection to each endpoint next to it.
        for (int i = 1; i < num_hosts; i += 2)
        {
            GlobalsConnPair prev_conn_pair = all_conns.get(i-1);
            GlobalsConnPair curr_conn_pair = all_conns.get(i);
            connect(prev_conn_pair, curr_conn_pair);
            
            if (i+1 >= num_hosts)
                break;
            
            GlobalsConnPair next_conn_pair = all_conns.get(i+1);
            connect(next_conn_pair, curr_conn_pair);
        }

        // Actually check that have correct uuids
        for (int i = 1; i < num_hosts; i += 2)
        {
            GlobalsConnPair prev_conn_pair = all_conns.get(i-1);
            GlobalsConnPair curr_conn_pair = all_conns.get(i);

            Boolean in_prev = prev_conn_pair.conn_uuid.in_conn_uuids(
                curr_conn_pair.conn_uuid.loc_uuid());

            if (! in_prev.booleanValue())
                return false;
            Boolean in_curr_one = curr_conn_pair.conn_uuid.in_conn_uuids(
                prev_conn_pair.conn_uuid.loc_uuid());
            if (! in_curr_one.booleanValue())
                return false;
            
            if (i+1 >= num_hosts)
                break;
            
            GlobalsConnPair next_conn_pair = all_conns.get(i+1);
            Boolean in_next = next_conn_pair.conn_uuid.in_conn_uuids(
                curr_conn_pair.conn_uuid.loc_uuid());
            if (! in_next.booleanValue())
                return false;
            Boolean in_curr_two = curr_conn_pair.conn_uuid.in_conn_uuids(
                next_conn_pair.conn_uuid.loc_uuid());
            if (! in_curr_two.booleanValue())
                return false;
        }
        
        return true;
    }

    public static void connect(GlobalsConnPair a, GlobalsConnPair b) throws Exception
    {
        Ralph.tcp_connect("127.0.0.1",b.port_listening_on, a.ralph_globals);
    }

    public static class GlobalsConnPair
    {
        public final RalphGlobals ralph_globals;
        public final ConnUuid conn_uuid;
        public final int port_listening_on;
        
        private GlobalsConnPair(
            RalphGlobals ralph_globals, ConnUuid conn_uuid,
            int port_listening_on)
        {
            this.ralph_globals = ralph_globals;
            this.conn_uuid = conn_uuid;
            this.port_listening_on = port_listening_on;
        }

        public static GlobalsConnPair generate (int port_listening_on) throws Exception
        {
            RalphGlobals.Parameters params = new RalphGlobals.Parameters();
            params.tcp_port_to_listen_for_connections_on = port_listening_on;
            RalphGlobals ralph_globals = new RalphGlobals(params);
            
            ConnUuid endpt =
                ConnUuid.create_single_sided(ralph_globals);

            return new GlobalsConnPair(
                ralph_globals, endpt, port_listening_on);
        }
    }
}