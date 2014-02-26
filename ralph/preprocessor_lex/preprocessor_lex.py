from deps.ply import lex
from ralph.common.compiler_exceptions import CompilerException

_IDENTIFIER_TOKEN = 'IDENTIFIER'
reserved = {
    'include' : 'INCLUDE',
    'alias' : 'ALIAS',
    'as': 'AS',
    'Endpoint': 'ENDPOINT',
    'Struct': 'STRUCT'
    }


tokens = [
    #comments
    'PREPROCESSOR_BEGIN',
    #Strings and quotes
    'SINGLE_LINE_STRING',
    _IDENTIFIER_TOKEN,

    'SINGLE_LINE_COMMENT',
    'MULTI_LINE_COMMENT_BEGIN',
    'MULTI_LINE_COMMENT_END',

    
    # whitespace
    'SPACE',
    'TAB',
    'NEWLINE',
    
    'ALL_ELSE',
    ] + list(reserved.values())


SKIP_TOKEN_TYPE = 'SPACE'

def generate_token_err_msg(toke):
    '''
    @returns {String} -- An error message based on the
    token value seen when get into ALL_ELSE.
    '''
    toke_val = repr(toke.value)
    err_msg = 'Error lexing preprocessor input.  '
    err_msg += '\n' + toke_val + '\n'
    return err_msg
    
class LexStates(object):
    (
        RUNNING,
        PREPROCESSOR,
        PREPROCESSOR_STRING,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT,
        STRING
        ) = range(0,6)

class LexStateMachine():
    def __init__ (self):
        self.state = LexStates.RUNNING
        self.preprocessor_string = ''
        
    def add_token(self,toke):
        toke_type = toke.type
        to_return = toke

        if self.state == LexStates.RUNNING:
            if toke_type == 'SINGLE_LINE_STRING':
                self.state = LexStates.STRING

            if toke_type == 'SINGLE_LINE_COMMENT':
                self.state = LexStates.SINGLE_LINE_COMMENT

            if toke_type == 'MULTI_LINE_COMMENT':
                self.state = LexStates.MULTI_LINE_COMMENT
                
            if toke_type == 'PREPROCESSOR_BEGIN':
                self.state = LexStates.PREPROCESSOR

            to_return.type = SKIP_TOKEN_TYPE
            return to_return
                
        if self.state == LexStates.STRING:
            if toke_type == 'SINGLE_LINE_STRING':
                self.state = LexStates.RUNNING
            to_return.type = SKIP_TOKEN_TYPE
            return to_return

        if self.state == LexStates.SINGLE_LINE_COMMENT:
            if toke_type == 'NEWLINE':
                self.state = LexStates.RUNNING
            to_return.type = SKIP_TOKEN_TYPE
            return to_return

        if self.state == LexStates.MULTI_LINE_COMMENT:
            if toke_type == 'MULTI_LINE_COMMENT_END':
                self.state = LexStates.RUNNING
            to_return.type = SKIP_TOKEN_TYPE
            return to_return

        if self.state == LexStates.PREPROCESSOR_STRING:
            if toke_type == 'SINGLE_LINE_STRING':
                self.state = LexStates.PREPROCESSOR
                to_return.value = self.preprocessor_string
                self.preprocessor_string = ''
                return to_return
            else:
                self.preprocessor_string += str(toke.value)

            to_return.type = SKIP_TOKEN_TYPE
            return to_return

        if self.state == LexStates.PREPROCESSOR:
            if toke_type == 'SINGLE_LINE_STRING':
                self.state = LexStates.PREPROCESSOR_STRING
                to_return.type = SKIP_TOKEN_TYPE
                return to_return

            if toke_type == 'NEWLINE':
                self.state = LexStates.RUNNING
                to_return.type = SKIP_TOKEN_TYPE
            
            return to_return
        
        print ('Unknown state in preprocessor')
        assert (False)
        

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

def t_MULTI_LINE_COMMENT_BEGIN(t):
    '\/\*'
    return lex_state_machine.add_token(t)

def t_MULTI_LINE_COMMENT_END(t):
    '\*\/'
    return lex_state_machine.add_token(t)

def t_PREPROCESSOR_BEGIN(t):
    '\#'
    return lex_state_machine.add_token(t)

def t_SPACE(t):
    '[ \t]+'
    return lex_state_machine.add_token(t)

def t_NEWLINE(t):
    r'[\r]?[\n]'
    t.lexer.lineno += len(t.value)
    t.value = r'\n'
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
        "'Unknown text '%s'  at line number '%s'" % (t.value,t.lexer.lineno))

class LexException(CompilerException):
    pass


def construct_lexer():
    global lex_state_machine
    lex_state_machine = LexStateMachine()
    lex.lex()
    return lex

