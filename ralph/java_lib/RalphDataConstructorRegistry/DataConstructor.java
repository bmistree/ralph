package RalphDataConstructorRegistry;

import ralph_protobuffs.VariablesProto;
import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.ActiveEvent;

public interface DataConstructor
{
    public RalphObject construct(
        VariablesProto.Variables.Any any,RalphGlobals ralph_globals);
}