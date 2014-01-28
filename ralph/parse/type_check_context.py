from ralph.parse.parse_util import TypeCheckException
from ralph.parse.parse_util import InternalTypeCheckException

class Scope(object):
    def __init__(self):
        # from ralph variable name to ast_nodes 
        self.var_dict = {}
        
    def add_var(self,ralph_var_name,ast_node):
        self.var_dict[ralph_var_name] = ast_node

    def get_internal_name(self,ralph_var_name):
        '''
        @returns {AstNode} --- None if variable isn't in scope.
        '''
        return self.var_dict.get(ralph_var_name,None)

class _FixupObject(object):
    def __init__(
        self,struct_to_fixup_name,field_struct_to_fixup,
        to_fixup_with_fixupable_object):
        '''
        @param {FixupableObject} to_fixup_with_fixupable_object ---
        Keeps track of whether the field we're trying to fixup is for
        an endpoint or a struct and what that endpoint'/struct's name
        is.        
        '''
        self.struct_to_fixup_name = struct_to_fixup_name
        self.field_struct_to_fixup = field_struct_to_fixup
        self.to_fixup_with_fixupable_object = to_fixup_with_fixupable_object


class AliasContext(object):
    def __init__(self):
        self.struct_names_to_alias_names_dict = {}
        self.endpoint_names_to_alias_names_dict = {}
    def add_struct_alias(self,struct_name, struct_alias):
        self.struct_names_to_alias_names_dict[struct_name] = struct_alias
    def add_endpoint_alias(self,endpoint_name, endpoint_alias):
        self.endpoint_names_to_alias_names_dict[endpoint_name] = endpoint_alias
    def get_struct_alias(self,struct_name):
        return self.struct_names_to_alias_names_dict.get(struct_name,None)
    def get_endpoint_alias(self,endpoint_name):
        return self.endpoint_names_to_alias_names_dict.get(
            endpoint_name,None)
                                                           

class FixupableObject(object):
    FIXUPABLE_TYPE_STRUCT = 0
    FIXUPABLE_TYPE_ENDPOINT = 1
    
    def __init__(self,fixupable_type, fixupable_name,is_tvar):
        self.fixupable_type = fixupable_type
        self.fixupable_name = fixupable_name
        self.is_tvar = is_tvar

    def perform_fixup(self,struct_types_ctx):
        '''Return a cloned type object associated with this fixupable
        object.
        '''
        if self.fixupable_type == FixupableObject.FIXUPABLE_TYPE_STRUCT:
            type_to_fixup_with = (
                struct_types_ctx.struct_name_to_type_obj_dict[self.fixupable_name])
        elif self.fixupable_type == FixupableObject.FIXUPABLE_TYPE_ENDPOINT:
            type_to_fixup_with = (
                struct_types_ctx.endpoint_name_to_type_obj_dict[self.fixupable_name])
        #### DEBUG
        else:
            raise InternalTypeCheckException(
                'Unknown fixupable object type when performing fixups.')
        #### END DEBUG

        return type_to_fixup_with.clone(self.is_tvar)

        
    
class StructTypesContext(object):
    """Maintains a dict from struct names to their type objects.
    """
    
    def __init__(self,alias_ctx):
        self.struct_name_to_type_obj_dict = {}
        self.endpoint_name_to_type_obj_dict = {}
        # each element is a FixupObject
        self.list_to_fixup = []
        self.alias_ctx = alias_ctx

    def to_fixup(
        self,struct_to_fixup_name,field_struct_to_fixup,
        to_fixup_with_fixupable_object):
        '''When a struct contains another struct, sometimes cannot
        specify the full type of the struct because the other struct
        isn't defined.  Eg.,

        Struct Outer
        {
            Struct Inner inner;
        }

        Struct Inner
        {
            Number num;
        }

        Cannot fix full type of Struct Outer until have defined
        Struct Inner.


        @param {FixupableObject} to_fixup_with_fixupable_object ---
        Keeps track of whether the field we're trying to fixup is for
        an endpoint or a struct and what that endpoint'/struct's name
        is.
        '''
        self.list_to_fixup.append(
            _FixupObject(
                struct_to_fixup_name,field_struct_to_fixup,
                to_fixup_with_fixupable_object))

    def perform_fixups(self):
        for fixup_obj in self.list_to_fixup:
            struct_type_to_fixup = (
                self.struct_name_to_type_obj_dict[fixup_obj.struct_to_fixup_name])

            type_to_fixup_with = (
                fixup_obj.to_fixup_with_fixupable_object.perform_fixup(self))
            
            to_fixup_field_type_dict = struct_type_to_fixup.name_to_field_type_dict
            to_fixup_field_type_dict[fixup_obj.field_struct_to_fixup] = (
                type_to_fixup_with)
        self.list_to_fixup = []

        
    def get_type_obj_from_struct_name(self,name):
        '''
        Returns:
           TypeObject or None (if type name does not exist).
        '''
        return self.struct_name_to_type_obj_dict.get(name,None)
    
    def add_struct_type_obj_for_name(self,name,type_obj,line_number):
        if name in self.struct_name_to_type_obj_dict:
            raise TypeCheckException(
                line_number,
                'Already have a type named %s' % name)
        self.struct_name_to_type_obj_dict[name] = type_obj

    def add_endpoint_type_obj_for_name(self,name,type_obj,line_number):
        self.endpoint_name_to_type_obj_dict[name] = type_obj
    def get_endpoint_type_obj_for_name(self,name,type_obj,line_number):
        return self.endpoint_name_to_type_obj_dict[name] 
        
    def __iter__(self):
        return iter(self.struct_name_to_type_obj_dict.keys())
        
    
class TypeCheckContext(object):
    """Tracks information necessary for type checking as type
    checking.  (Eg., variables previously declared in this lexical
    scope, etc.
    """

    def __init__(self,endpoint_name,struct_types_ctx):
        '''
        Args:
            endpoint_name: {String} name of endpoint
            struct_types: {StructTypesContext object}
        '''
        # end of list is more recent scope
        self.scope_stack = []
        self.endpoint_name = endpoint_name
        self.struct_types_ctx = struct_types_ctx

    def push_scope(self):
        self.scope_stack.append(Scope())

    def pop_scope(self):
        self.scope_stack.pop()

    def add_var_name(self,ralph_var_name,ast_node):
        '''
        @param {AstNode} ast_node --- Either a declaration node or a
        type node.
        '''
        last_valid_index = len(self.scope_stack) - 1
        top_scope = self.scope_stack[last_valid_index]
        top_scope.add_var(ralph_var_name,ast_node)
        
    def lookup_internal_ast_node(self,ralph_var_name):
        '''
        @returns {AstNode or None} --- None if variable is endpoint
        global
        '''
        # search through in reverse order to get most recent variable
        # in case of shadowing.
        for scope in reversed(self.scope_stack):
            ast_node = scope.get_internal_name(ralph_var_name)
            if ast_node is not None:
                return ast_node

        return None
