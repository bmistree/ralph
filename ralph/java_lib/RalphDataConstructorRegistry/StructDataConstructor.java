package RalphDataConstructorRegistry;

import ralph_protobuffs.VariablesProto;
import ralph.RalphObject;
import ralph.RalphGlobals;

public interface StructDataConstructor
{
    public RalphObject construct(
        RalphGlobals ralph_globals,
        VariablesProto.Variables.Struct proto_struct);
}