package RalphDataConstructorRegistry;

import java.util.List;

import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.AtomicList;
import ralph_protobuffs.VariablesProto;
import static ralph.Variables.AtomicListVariable;
import static ralph.Variables.NonAtomicNumberVariable;
import RalphAtomicWrappers.BaseAtomicWrappers;
import ralph.Util;
import ralph.NonAtomicActiveEvent;


public class BasicContainerDataConstructors
{
    protected static NonAtomicActiveEvent dummy_deserialization_active_event()
    {
        // FIXME: must finish
        Util.logger_assert(
            "Must create dummy non-atomic events for dserialization");
        return null;
    }

    
    /** List deserializers*/
    private final static AtomNumListConstructor dummy_atom_list_constructor =
        AtomNumListConstructor.get_instance();
    private static class AtomNumListConstructor implements DataConstructor
    {
        private final static AtomNumListConstructor singleton =
            new AtomNumListConstructor();
        
        protected AtomNumListConstructor()
        {
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.label, BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL);

            deserializer.register(label,this);
        }
        public static AtomNumListConstructor get_instance()
        {
            return singleton;
        }
        
        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<Double,Double> to_return =
                new AtomicListVariable<Double,Double>(
                    false,BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
                    ralph_globals);

            NonAtomicActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || (! any.getIsTvar()))
                Util.logger_assert("Cannot deserialize list without list field");
            //// END DEBUG

            VariablesProto.Variables.List list_message = any.getList();
            List<VariablesProto.Variables.Any> any_list =
                list_message.getListValuesList();

            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            for (VariablesProto.Variables.Any list_element : any_list)
            {
                try
                {
                    NonAtomicNumberVariable non_atom_number =
                        (NonAtomicNumberVariable)
                        deserializer.deserialize(list_element,ralph_globals);
                    to_return.get_val(evt).append(evt,non_atom_number.get_val(evt));
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            
            return to_return;
        }
    }
}