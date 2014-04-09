package RalphDataConstructorRegistry;

import java.util.List;

import ralph.RalphObject;
import ralph.RalphGlobals;
import ralph.AtomicList;
import ralph.NonAtomicList;
import ralph_protobuffs.VariablesProto;
import static ralph.Variables.AtomicListVariable;
import static ralph.Variables.NonAtomicListVariable;
import static ralph.Variables.NonAtomicNumberVariable;
import static ralph.Variables.NonAtomicTextVariable;
import static ralph.Variables.NonAtomicTrueFalseVariable;
import RalphAtomicWrappers.BaseAtomicWrappers;
import ralph.Util;
import ralph.ActiveEvent;
import static RalphDataConstructorRegistry.DataConstructorRegistry.dummy_deserialization_active_event;
import RalphAtomicWrappers.EnsureAtomicWrapper;

public class BasicListDataConstructors
{
    // only used to force populating internal static fields
    private final static BasicListDataConstructors instance =
        new BasicListDataConstructors();
    protected BasicListDataConstructors()
    {}
    public static BasicListDataConstructors get_instance()
    {
        return instance;
    }

    // Note: Atomic lists still have non-atomic internal elements.
    private final static AtomListConstructor<Double> atom_num_list_constructor =
        new AtomListConstructor<Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);
    private final static AtomListConstructor<String> atom_text_list_constructor =
        new AtomListConstructor<String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER);
    private final static AtomListConstructor<Boolean> atom_tf_list_constructor =
        new AtomListConstructor<Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER);
    // non-atomic list constructors
    private final static NonAtomListConstructor<Double> non_atom_num_list_constructor =
        new NonAtomListConstructor<Double>(
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);
    private final static NonAtomListConstructor<String> non_atom_text_list_constructor =
        new NonAtomListConstructor<String>(
            BaseAtomicWrappers.NON_ATOMIC_TEXT_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TEXT_WRAPPER);
    private final static NonAtomListConstructor<Boolean> non_atom_tf_list_constructor =
        new NonAtomListConstructor<Boolean>(
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_LABEL,
            BaseAtomicWrappers.NON_ATOMIC_TRUE_FALSE_WRAPPER);
    
    
    private static class AtomListConstructor<ElementType>
        implements DataConstructor
    {
        private final EnsureAtomicWrapper wrapper;
        
        public AtomListConstructor(String element_label,EnsureAtomicWrapper _wrapper)
        {
            wrapper = _wrapper;
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                AtomicList.deserialization_label,
                element_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            AtomicListVariable<ElementType,ElementType> outer_list =
                new AtomicListVariable<ElementType,ElementType>(
                    false,wrapper,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

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
                    RalphObject non_atom_element =
                        deserializer.deserialize(list_element,ralph_globals);
                    non_atom_element.get_val(evt);
                    outer_list.get_val(evt).append(
                        evt,
                        (ElementType)non_atom_element.get_val(evt));

                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list
            return to_return;
        }
    }
    

    private static class NonAtomListConstructor<ElementType>
        implements DataConstructor
    {
        private final EnsureAtomicWrapper wrapper;
        
        public NonAtomListConstructor(String element_label,EnsureAtomicWrapper _wrapper)
        {
            wrapper = _wrapper;
            DataConstructorRegistry deserializer =
                DataConstructorRegistry.get_instance();
            
            String label = deserializer.merge_labels(
                NonAtomicList.deserialization_label,
                element_label);
            deserializer.register(label,this);
        }

        @Override
        public RalphObject construct(
            VariablesProto.Variables.Any any,RalphGlobals ralph_globals)
        {
            // create an atomic list variable, then, independently
            // populate each of its fields.
            NonAtomicListVariable<ElementType,ElementType> outer_list =
                new NonAtomicListVariable<ElementType,ElementType>(
                    false,wrapper,ralph_globals);
            RalphObject to_return = null;
            ActiveEvent evt = dummy_deserialization_active_event();

            //// DEBUG
            if ((! any.hasList()) || ( any.getIsTvar()))
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
                    RalphObject non_atom_element =
                        deserializer.deserialize(list_element,ralph_globals);
                    non_atom_element.get_val(evt);
                    outer_list.get_val(evt).append(
                        evt,
                        (ElementType)non_atom_element.get_val(evt));

                    to_return = outer_list.get_val(null);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    Util.logger_assert(
                        "Should never be backed out when deserializing");
                }
            }
            // return internal list
            return to_return;
        }
    }
}