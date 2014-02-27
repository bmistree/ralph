from ralph.common.compiler_exceptions import CompilerException

def format_exception_message(message):
    return '\n\n************\n' + message + '\n************\n'

def indent_string(string,amt_to_indent=1):
    '''
    @param {String} string -- Each line in this string we will insert
    indentAmount number of tabs before and return the new, resulting
    string.
    
    @param {int} amt_to_indent

    @returns {String}
    '''
    split_on_newline = string.split('\n')
    indented_string = ''

    indenter = '';
    for s in range(0,amt_to_indent):
        indenter += '    ';

    for s in range(0,len(split_on_newline)):
        if len(split_on_newline[s]) != 0:
            indented_string += indenter + split_on_newline[s]
        if s != len(split_on_newline) -1:
            indented_string += '\n'

    return indented_string


class InternalEmitException(CompilerException):
    def __init__(self,filename,line_number,msg):
        self.filename = filename
        self.line_number = line_number
        self.msg = msg
    def __str__(self):
        return format_exception_message(
            'Error in file %s on line %i: %s' %
            (self.filename,self.line_number,self.msg))
