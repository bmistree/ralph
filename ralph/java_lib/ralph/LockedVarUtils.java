package ralph;

public class LockedVarUtils {

    public static boolean is_non_ext_text_var(Object to_get_from)
    {
        if (Variables.SingleThreadedLockedTextVariable.class.isInstance(to_get_from) ||
            Variables.LockedTextVariable.class.isInstance(to_get_from))
            return true;
        return false;
    }


    /**
     * Returns true if the object is one of the internal references
     * @param to_get_from
     * @return
     */
    public static boolean is_reference_container(Object to_get_from) 
    {
        if (ContainerInterface.class.isInstance(to_get_from))
            return true;
			
        return false;
    }

    public static boolean is_external_text_variable(Object lhs)
    {
        return false;
    }

}
