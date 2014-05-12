package RalphDeserializer;

import ralph_protobuffs.VariablesProto;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.ActiveEvent;

public interface DataDeserializer
{
    public RalphObject deserialize(
        VariablesProto.Variables.Any any,RalphGlobals ralph_globals);
}