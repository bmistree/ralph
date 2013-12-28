

ROOT_STATEMENT = 'root_statement'
ENDPOINT_DEFINITION_STATEMENT = 'endpoint_definition'
ENDPOINT_BODY = 'endpoint_body'
IDENTIFIER_EXPRESSION = 'identifier'
DECLARATION_STATEMENT = 'declaration_statement'
VARIABLE_TYPE = 'variable_type'
MAP_VARIABLE_TYPE = 'map_variable_type'
STRUCT_VARIABLE_TYPE = 'struct_variable_type'
METHOD_DECLARATION = 'method_declaration'
METHOD_SIGNATURE = 'method_signature'
METHOD_DECLARATION_ARG = 'method_declaration_arg'
ATOMICALLY = 'atomically'
SCOPE = 'scope'
PARALLEL = 'parallel'
ASSIGNMENT = 'assignment'
NOT = 'not'
DOT = 'dot'
BRACKET = 'bracket'
PARTNER_METHOD_CALL = 'partner_call'
PRINT_CALL = 'print_call'

TEXT_LITERAL = 'text_literal'
NUMBER_LITERAL = 'number_literal'
TRUE_FALSE_LITERAL = 'true_false_literal'


MULTIPLY = 'multiply'
DIVIDE = 'divide'
ADD = 'add'
SUBTRACT = 'subtract'

GREATER_THAN = 'greater_than'
GREATER_THAN_EQUALS = 'greater_than_equal'
LESS_THAN = 'less_than'
LESS_THAN_EQUALS = 'less_than_equal'

EQUALS = 'equals'
NOT_EQUALS = 'not_equals'

AND = 'and'
OR = 'or'

IN = 'in'
NOT_IN = 'not_in'

LEN = 'len'
CONDITION = 'condition'
IF = 'if'
ELIF = 'elif'
RETURN = 'return'
METHOD_CALL = 'method_call'

RANGE = 'range'

STRUCT_DEFINITION = 'struct_definition'


### intermediate ast nodes that get removed from tree ###
ENDPOINT_LIST_STATEMENT = 'endpoint_list_statement'
EMPTY_STATEMENT = 'empty_statement'
METHOD_DECLARATION_ARGS = 'method_declaration_args'
SCOPE_BODY = 'scope_body'
METHOD_CALL_ARGS = 'method_call_args'

ELSE_IFS = 'elifs'
ELSE = 'else'

STRUCT_LIST_NODE = 'struct_list_node'
STRUCT_BODY = 'struct_body'

### types ###
BOOL_TYPE = 'TrueFalse'
STRING_TYPE = 'Text'
NUMBER_TYPE = 'Number'

BASIC_TYPES_LIST = [BOOL_TYPE,STRING_TYPE,NUMBER_TYPE]
