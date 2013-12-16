
class Type(object):
    pass

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
    def __init__(self,from_type_node,to_type_node,is_tvar):
        self.from_type_node = from_type_node
        self.to_type_node = to_type_node
        self.is_tvar = is_tvar

    
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
