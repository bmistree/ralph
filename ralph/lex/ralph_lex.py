from deps.ply import lex
from ralph.common.compiler_exceptions import CompilerException

_IDENTIFIER_TOKEN = 'IDENTIFIER'
STRUCT_TYPE_TOKEN = 'Struct'
ENDPOINT_TOKEN = 'Endpoint'
SERVICE_TOKEN = 'Service'
INTERFACE_TOKEN = 'Interface'
SERVICE_FACTORY_TOKEN = 'ServiceFactory'
PRINT_TYPE_TOKEN = 'print'
SPECULATE_TYPE_TOKEN = 'speculate'
SPECULATE_ALL_TYPE_TOKEN = 'speculate_all'
VERBATIM_TOKEN = 'verbatim'

reserved = {
    'Endpoint' : 'ENDPOINT',
    'Service' : 'SERVICE',
    'Method': 'METHOD',
    'in': 'IN',
    'return': 'RETURN_OPERATOR',
    'if': 'IF',
    'elif': 'ELSE_IF',
    'else': 'ELSE',
    'not': 'NOT',
    'TrueFalse': 'BOOL_TYPE',
    'True': 'TRUE',
    'False': 'FALSE',
    'Number': 'NUMBER_TYPE',
    'Text': 'STRING_TYPE',
    'and': 'AND',
    'or': 'OR',
    'print': 'PRINT',
    'speculate': 'SPECULATE',
    'speculate_all': 'SPECULATE_ALL',
    'while': 'WHILE',
    'for': 'FOR',
    'range': 'RANGE',
    'keys': 'KEYS',
    'len': 'LEN',
    'Map': 'MAP_TYPE',
    'Struct': 'STRUCT_TYPE',
    'from': 'FROM',
    'to': 'TO',
    'try': 'TRY',
    'catch': 'CATCH',
    'finally': 'FINALLY',
    'atomically': 'ATOMICALLY',
    'parallel': 'PARALLEL',
    'returns': 'RETURNS',
    'TVar' : 'TVAR',
    'partner': 'PARTNER',
    'List': 'LIST_TYPE',
    'element': 'ELEMENT',
    'alias': 'ALIAS',
    'as': 'AS',
    'verbatim': 'VERBATIM',
    'null': 'NULL',
    'ServiceFactory' : 'SERVICE_FACTORY',
    'self': 'SELF',
    'implements': 'IMPLEMENTS',
    'Interface': 'INTERFACE'
    }


tokens = [
    #comments
    'SINGLE_LINE_COMMENT',
    "MULTI_LINE_COMMENT_BEGIN",
    "MULTI_LINE_COMMENT_END",

    'PRE_PROCESSOR', # gets treated as a comment
    
    "EQUALS",
    "BOOL_EQUALS",
    "BOOL_NOT_EQUALS",
    
    "GREATER_THAN_EQ",
    "GREATER_THAN",
    "LESS_THAN_EQ",
    "LESS_THAN",
    
    #whitespace
    "SPACE",
    "TAB",
    "NEWLINE",
    
    #other
    "SEMI_COLON",    
    "COMMA",
    "COLON",
    "AT",

    # operator=
    "PLUS_EQUAL",
    "MINUS_EQUAL",
    "DIVIDE_EQUAL",
    "MULTIPLY_EQUAL",
    
    #math operators
    "PLUS",
    "MINUS",
    "DIVIDE",
    "MULTIPLY",
    
    #brackets/braces
    "LEFT_PAREN",
    "RIGHT_PAREN",
    "LEFT_BRACKET",
    "RIGHT_BRACKET",
    "CURLY_LEFT",
    "CURLY_RIGHT",

    'DOT',
    
    "NUMBER",
    _IDENTIFIER_TOKEN,

    
    #Strings and quotes
    "SINGLE_LINE_STRING",
    
    "ALL_ELSE",
    ] + list(reserved.values())


SKIP_TOKEN_TYPE = "SPACE"

def generate_token_err_msg(toke):
    '''
    @returns {String} -- An error message based on the
    token value seen when get into ALL_ELSE.
    '''
    toke_val = repr(toke.value)
    err_msg = 'Error lexing input.  '
    err_msg += '\n' + toke_val + '\n'
    return err_msg
    

class LexStateMachine():
    def __init__ (self,lex_filename):
        '''
        Args:
            lex_filename: {String} name of file that we are lexing.
        '''
        self.filename = lex_filename
        self.in_multi_line_comment  = False
        self.in_single_line_comment = False
        self.in_multi_line_string   = False
        self.in_single_line_string  = False
        self.full_string = ''
        
        self.numEndpointsSeen    =     0
        
    def add_token(self,toke):
        toke_type = toke.type
        returner = toke

        #determine whether to skip token
        if self.in_multi_line_string:
            if toke_type != "MULTI_LINE_STRING":
                #we're still in the multi-line string
                self.full_string += str(toke.value)
                returner.type = SKIP_TOKEN_TYPE
        elif self.in_single_line_string:
            if toke_type != "SINGLE_LINE_STRING":
                #we're still in the single-line string
                self.full_string += str(toke.value)
                returner.type = SKIP_TOKEN_TYPE
        elif self.in_multi_line_comment:
            returner.type = SKIP_TOKEN_TYPE
        elif self.in_single_line_comment:
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "MULTI_LINE_COMMENT_BEGIN":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "SINGLE_LINE_COMMENT":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "PRE_PROCESSOR":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "SPACE":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "TAB":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "NEWLINE":
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == 'ALL_ELSE':
            if self.in_multi_line_comment or self.in_single_line_comment:
                returner.type = SKIP_TOKEN_TYPE
            else:
                err_msg = generate_token_err_msg(toke)
                line_num,err_msg = generate_type_error(err_msg,toke)
                raise LexException(line_num,err_msg)

        #adjust state machine
        if self.in_multi_line_string:
            #check close multi-line string
            if toke_type == "MULTI_LINE_STRING":
                #preserver token type of string on end.
                self.in_multi_line_string = False
                returner.value = self.full_string
                self.full_string = ''

        elif self.in_single_line_string:
            #check close multi-line string
            if toke_type == "SINGLE_LINE_STRING":
                #preserver token type of string on end.
                self.in_single_line_string = False
                returner.value = self.full_string
                self.full_string = ''
        elif self.in_multi_line_comment:
            if toke_type == "MULTI_LINE_COMMENT_END":
                self.in_multi_line_comment = False
        elif self.in_single_line_comment:
            if toke_type == "NEWLINE":
                self.in_single_line_comment = False
        elif toke_type == "MULTI_LINE_COMMENT_BEGIN":
            self.in_multi_line_comment = True
        elif toke_type == "SINGLE_LINE_COMMENT":
            self.in_single_line_comment = True
        elif toke_type == "PRE_PROCESSOR":
            # preprocessor is treated same as single line comments:
            # ignored.
            self.in_single_line_comment = True            
        elif toke_type == "MULTI_LINE_STRING":
            self.in_multi_line_string = True
            returner.type = SKIP_TOKEN_TYPE
        elif toke_type == "SINGLE_LINE_STRING":
            self.in_single_line_string = True
            returner.type = SKIP_TOKEN_TYPE
        else:
            if toke_type == "MULTI_LINE_COMMENT_END":
                err_msg = "Cannot lex.  multi-line comment "
                err_msg = "end occurred before multi-line begin."
                line_num, err_text = generate_type_error(err_msg,toke)
                raise LexException(line_num,err_text)

        if returner.type == SKIP_TOKEN_TYPE:
            return None
        return returner


def generate_type_error(err_msg, token):
    err_body = err_msg + ' at line number ' + str(token.lexer.lineno)
    return token.lexer.lineno,err_body


'''
Rule definitions
'''

lex_state_machine = None

#high-level structure
def t_SINGLE_LINE_COMMENT(t):
    '\/\/'
    return lex_state_machine.add_token(t)

def t_PRE_PROCESSOR(t):
    '\#'
    return lex_state_machine.add_token(t)


def t_MULTI_LINE_COMMENT_BEGIN(t):
    '\/\*'
    return lex_state_machine.add_token(t)

def t_MULTI_LINE_COMMENT_END(t):
    '\*\/'
    return lex_state_machine.add_token(t)

def t_BOOL_EQUALS(t):
    '\=\='
    return lex_state_machine.add_token(t)

def t_BOOL_NOT_EQUALS(t):
    "\!\="
    return lex_state_machine.add_token(t)

def t_LESS_THAN_EQ(t):
    "\<\="
    return lex_state_machine.add_token(t)

def t_LESS_THAN(t):
    "\<"
    return lex_state_machine.add_token(t)

def t_GREATER_THAN_EQ(t):
    "\>\="
    return lex_state_machine.add_token(t)

def t_GREATER_THAN(t):
    "\>"
    return lex_state_machine.add_token(t)

def t_EQUALS(t):
    '\='
    return lex_state_machine.add_token(t)

def t_SPACE(t):
    '[ \t]+'
    return lex_state_machine.add_token(t)

def t_NEWLINE(t):
    r"[\r]?[\n]"
    t.lexer.lineno += len(t.value)
    t.value = r"\n"
    return lex_state_machine.add_token(t)

def t_LEFT_PAREN(t):
    '\('
    return lex_state_machine.add_token(t)

def t_RIGHT_PAREN(t):
    '\)'
    return lex_state_machine.add_token(t)

def t_COMMA(t):
    '\,'
    return lex_state_machine.add_token(t)

def t_COLON(t):
    '\:'
    return lex_state_machine.add_token(t)

def t_AT(t):
    '@'
    return lex_state_machine.add_token(t)

def t_LEFT_BRACKET(t):
    '\['
    return lex_state_machine.add_token(t)

def t_RIGHT_BRACKET(t):
    '\]'
    return lex_state_machine.add_token(t)

def t_SEMI_COLON(t):
    '\;'
    return lex_state_machine.add_token(t)

def t_CURLY_LEFT(t):
    '\{'
    return lex_state_machine.add_token(t)

def t_CURLY_RIGHT(t):
    '\}'
    return lex_state_machine.add_token(t)

def t_DIVIDE_EQUAL(t):
    '\/='
    return lex_state_machine.add_token(t)

def t_MINUS_EQUAL(t):
    '-='
    return lex_state_machine.add_token(t)

def t_PLUS_EQUAL(t):
    '\+='
    return lex_state_machine.add_token(t)

def t_MULTIPLY_EQUAL(t):
    '\*='
    return lex_state_machine.add_token(t)

def t_DIVIDE(t):
    '\/'
    return lex_state_machine.add_token(t)

def t_MULTIPLY(t):
    '\*'
    return lex_state_machine.add_token(t)

def t_PLUS(t):
    '\+'
    return lex_state_machine.add_token(t)

def t_MINUS(t):
    '\-'
    return lex_state_machine.add_token(t)

def t_DOT(t):
    '[.]'
    return lex_state_machine.add_token(t)

def t_NUMBER(t):
    '\d+(\.\d*)?'
    return lex_state_machine.add_token(t)

def t_IDENTIFIER(t):
    r'[a-zA-Z][a-zA-Z_0-9_]*'
    t.type = reserved.get(t.value,_IDENTIFIER_TOKEN)    # Check for reserved words
    return lex_state_machine.add_token(t)

def t_SINGLE_LINE_STRING(t):
    r'[\']'
    return lex_state_machine.add_token(t)

def t_ALL_ELSE(t):
    '.'
    return lex_state_machine.add_token(t)

def t_error(t):
    raise LexException(
        t.lexer.lineno,
        "In file %s, unknown text '%s'  at line number '%s'" %
        (lex_state_machine.filename,t.value,t.lexer.lineno))


class LexException(CompilerException):
    pass


def construct_lexer(lex_filename):
    global lex_state_machine
    lex_state_machine = LexStateMachine(lex_filename)
    lex.lex()
    return lex

