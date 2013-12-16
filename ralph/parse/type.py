import ralph.parse.ast_labels as ast_labels
from ralph.parse.parse_util import InternalParseException,ParseException

class Type(object):
    def dict_dot_fields(self):
        """Returns dict of dot can access on type.

        Eg., a map type has built in .size, .set, .get, and .contains
        methods.  These would be returned as a dict.
        """
        return {}

class BasicType(Type):
    def __init__(self,basic_type,is_tvar):
        self.basic_type = basic_type
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
        return prefix + self.basic_type

class MapType(Type):
    SIZE_METHOD_NAME = 'size'
    CONTAINS_METHOD_NAME = 'contains'
    GET_METHOD_NAME = 'get'
    SET_METHOD_NAME = 'set'
    
    def __init__(self,from_type_node,to_type_node,is_tvar):
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

        # contains returns a number and takes no arguments
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

        self.dot_dict_methods = {
            MapType.SIZE_METHOD_NAME: size_method_type,
            MapType.CONTAINS_METHOD_NAME: contains_method_type,
            MapType.GET_METHOD_NAME: get_method_type,
            MapType.SET_METHOD_NAME: set_method_type
            }
        
    def dict_dot_fields(self):
        return self.dot_dict_methods
        
    
class MethodType(Type):
    def __init__(self,returns_type,arg_type_list):
        self.returns_type = returns_type
        self.arg_type_list = arg_type_list
        
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
