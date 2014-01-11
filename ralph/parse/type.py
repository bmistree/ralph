import ralph.parse.ast_labels as ast_labels
from ralph.parse.parse_util import InternalParseException,ParseException

class Type(object):
    def dict_dot_fields(self):
        """Returns dict of dot can access on type.

        Eg., a map type has built in .size, .set, .get, and .contains
        methods.  These would be returned as a dict.
        """
        return {}

class StructType(object):
    def __init__(
            self,struct_name,name_to_field_type_dict=None,is_tvar=None,
            alias_name=None):
        '''@see constructor for map type
        '''
        self.struct_name = struct_name
        if name_to_field_type_dict is not None:
            self.update_struct_type(
                name_to_field_type_dict,is_tvar,alias_name)

    def set_alias_name(self,new_alias_name):
        self.alias_name = new_alias_name
            
    def update_struct_type(
            self,name_to_field_type_dict,is_tvar,alias_name):
        self.name_to_field_type_dict = name_to_field_type_dict
        self.is_tvar = is_tvar
        self.alias_name = alias_name
        if self.alias_name is not None:
            self.struct_name = alias_name

    def prepend_to_name(self,to_prepend):
        '''Frequently use struct_name during emitting. If struct is
        aliased though, then do not always want to prepend in front of
        struct name.  This is because alias as form a.b.c.d; actually
        want to prepend in front of d.
        '''
        front = ''
        struct_name = self.struct_name
        if self.alias_name is not None:
            # means that we're dealing with an aliased struct.  Format of
            # alias commands (currently) can be a.b.c.actual_struct_name.
            # need to insert _Internal after last . if it exists
            last_index_of = struct_name.rfind('.')
            if last_index_of != -1:
                front = struct_name[0:last_index_of+1]
                struct_name = struct_name[last_index_of+1:]

        return front + to_prepend + struct_name


        
    def clone(self,is_tvar):
        '''Each user-defined struct has one canonical type.  When
        assigning a node this type, use the clone method on that
        canonical type to make a copy of it and assign that copy to
        that node's type field.
        '''
        return StructType(
            self.struct_name,dict(self.name_to_field_type_dict),
            is_tvar,self.alias_name)

    def __str__(self):
        tvar_string = 'TVar'
        if not self.is_tvar:
            tvar_string = ''
            
        return (
            'Struct %s %s' % (tvar_string,self.struct_name))
    
    def dict_dot_fields(self):
        """Returns dict of dot can access on type.

        Should just be the user-defined fields of the struct.
        """
        return dict(self.name_to_field_type_dict)


class BasicType(Type):
    def __init__(self,basic_type,is_tvar):
        self.basic_type = basic_type

        #### DEBUG
        if self.basic_type not in ast_labels.BASIC_TYPES_LIST:
            import pdb
            pdb.set_trace()
            raise InternalParseException('Unknown type in basic type')
        #### END DEBUG
        
        self.is_tvar = is_tvar

    def __ne__(self,other_type):
        return not self == other_type
        
    def __eq__(self,other_type):
        if not isinstance(other_type,BasicType):
            return False
        
        return self.basic_type == other_type.basic_type
        # return ((self.basic_type == other_type.basic_type) and
        #         (self.is_tvar == other_type.is_tvar))

    def __str__(self):
        prefix = 'TVar ' if self.is_tvar else ''
        return prefix + str(self.basic_type)


class ListType(Type):
    SIZE_METHOD_NAME = 'size'
    GET_METHOD_NAME = 'get'
    SET_METHOD_NAME = 'set'
    APPEND_METHOD_NAME = 'append'
    INSERT_METHOD_NAME = 'insert'
    CONTAINS_METHOD_NAME = 'contains'
    REMOVE_METHOD_NAME = 'remove'
    CLEAR_METHOD_NAME = 'clear'
    
    def __init__(self,element_type_node=None,is_tvar=None):
        if element_type_node is not None:
            self.update_element_type_tvar(element_type_node,is_tvar)

    def __str__(self):
        tvar_string = 'TVar'
        if not self.is_tvar:
            tvar_string = ''
        element_type_string = str(self.element_type_node.type)
        return (
            'List %s { element: %s}' %
            (tvar_string, element_type_string))
            
    def update_element_type_tvar(self,element_type_node,is_tvar):
        self.element_type_node = element_type_node
        self.is_tvar = is_tvar
        self._create_dot_dict_methods()
        
    def _create_dot_dict_methods(self):
        # size returns a number and takes no arguments
        size_method_type = MethodType(
            # returns number
            BasicType(
                ast_labels.NUMBER_TYPE,
                False),
            # takes no arguments
            [])

        # get returns an element type and takes a single key argument
        get_method_type = MethodType(
            self.element_type_node.type,
            [BasicType(ast_labels.NUMBER_TYPE,False)])

        # contains returns a boolean and takes no arguments
        contains_method_type = MethodType(
            # returns boolean
            BasicType(
                ast_labels.BOOL_TYPE,
                False),
            # takes single key argument
            [self.element_type_node.type])
        
        
        # set returns an element type node and takes a single key argument
        set_method_type = MethodType(
            self.element_type_node.type,
            [BasicType(ast_labels.NUMBER_TYPE,False), # list index
             self.element_type_node.type])

        # insert key, value argument
        insert_method_type = MethodType(
            self.element_type_node.type,
            [BasicType(ast_labels.NUMBER_TYPE,False), # list index
             self.element_type_node.type])

        # append: value
        append_method_type = MethodType(
            self.element_type_node.type,
            [self.element_type_node.type])

        # get returns an element type and takes a single key argument
        remove_method_type = MethodType(
            self.element_type_node.type,
            [BasicType(ast_labels.NUMBER_TYPE,False)])

        # clear doesn't return anything and takes no arguments
        clear_method_type = MethodType(None,[])

        self.dot_dict_methods = {
            ListType.SIZE_METHOD_NAME: size_method_type,
            ListType.GET_METHOD_NAME: get_method_type,
            ListType.SET_METHOD_NAME: set_method_type,
            ListType.CONTAINS_METHOD_NAME: contains_method_type,
            ListType.INSERT_METHOD_NAME: insert_method_type,
            ListType.APPEND_METHOD_NAME: append_method_type,
            ListType.REMOVE_METHOD_NAME: remove_method_type,
            ListType.CLEAR_METHOD_NAME: clear_method_type
            }
        
    def dict_dot_fields(self):
        return self.dot_dict_methods

    
class MapType(Type):
    SIZE_METHOD_NAME = 'size'
    CONTAINS_METHOD_NAME = 'contains'
    GET_METHOD_NAME = 'get'
    SET_METHOD_NAME = 'set'
    REMOVE_METHOD_NAME = 'remove'
    CLEAR_METHOD_NAME = 'clear'

    def __init__(self,from_type_node=None,to_type_node=None,is_tvar=None):
        '''Allow constructing empty map types so that can assign a
        provisional type to maps.  Can fill in from and to later.
        This allows having nested types, eg.,

        Struct Tree
        {
            Map(from: Number, to: Struct Tree) children;
        }

        This way, when create Struct Tree, can say that it has type of
        (empty) map, and then update the empty map with the correct
        fields.
        '''
        if from_type_node is not None:
            self.update_from_to_tvar(from_type_node,to_type_node,is_tvar)

    def __str__(self):
        tvar_string = 'TVar'
        if not self.is_tvar:
            tvar_string = ''
        from_type_string = str(self.from_type_node.type)
        # to_type_string = str(self.to_type_node.type)
        to_type_string = ''
        return (
            'Map %s { from: %s, to: %s}' %
            (tvar_string, from_type_string,to_type_string))
            
    def update_from_to_tvar(self,from_type_node,to_type_node,is_tvar):
        self.from_type_node = from_type_node
        self.to_type_node = to_type_node
        self.is_tvar = is_tvar
        self._create_dot_dict_methods()
        
    def _create_dot_dict_methods(self):
        # size returns a number and takes no arguments
        size_method_type = MethodType(
            # returns number
            BasicType(
                ast_labels.NUMBER_TYPE,
                False),
            # takes no arguments
            [])

        # contains returns a boolean and takes no arguments
        contains_method_type = MethodType(
            # returns boolean
            BasicType(
                ast_labels.BOOL_TYPE,
                False),
            # takes single key argument
            [self.from_type_node.type])

        # get returns a from type and takes a single key argument
        get_method_type = MethodType(
            self.to_type_node.type,
            [self.from_type_node.type])

        # set returns a from type and takes a single key argument
        set_method_type = MethodType(
            self.to_type_node.type,
            [self.from_type_node.type, self.to_type_node.type])

        # remove returns a from type and takes a single key argument
        remove_method_type = MethodType(
            self.to_type_node.type,
            [self.from_type_node.type])

        # clear doesn't return anything and takes no arguments
        clear_method_type = MethodType(None,[])

        
        self.dot_dict_methods = {
            MapType.SIZE_METHOD_NAME: size_method_type,
            MapType.CONTAINS_METHOD_NAME: contains_method_type,
            MapType.GET_METHOD_NAME: get_method_type,
            MapType.SET_METHOD_NAME: set_method_type,
            MapType.REMOVE_METHOD_NAME: remove_method_type,
            MapType.CLEAR_METHOD_NAME: clear_method_type
            }
        
    def dict_dot_fields(self):
        return self.dot_dict_methods
        
    
class MethodType(Type):
    def __init__(self,returns_type,arg_type_list):
        self.returns_type = returns_type
        self.arg_type_list = arg_type_list

    def num_arguments(self):
        return len(self.arg_type_list)
        
    def __ne__(self,other_type):
        return not self == other_type
        
    def __eq__(self,other_type):
        if not isinstance(other_type,MethodType):
            return False

        if self.returns_type != other_type.returns_type:
            return False

        if len(self.arg_type_list) != len(other_type.arg_type_list):
            return False
        
        for i in range(0,len(self.arg_type_list)):
            arg_type = self.arg_type_list[i]
            other_arg_type = other_type.arg_list[i]

            if arg_type != other_arg_type:
                return False
        return True
    
    def __str__(self):
        # FIXME: add method args and return type
        return 'method'

    def dict_dot_fields(self):
        if self.returns_type is None:
            raise InternalParseException('Cannot get dot methods on None')
        
        return self.returns_type.dict_dot_methods()
