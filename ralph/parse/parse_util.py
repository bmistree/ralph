from ralph.common.compiler_exceptions import CompilerException

def format_error_message(to_format):
    return '\n\n************\n' + to_format + '\n************\n\n'

class InternalParseException(CompilerException):
    def __init__(self,filename,line_number,msg):
        self.filename = filename
        self.line_number = line_number
        self.msg = msg
    def __str__(self):
        return format_error_message(
            'Error in file %s on line %i : %s ' %
            (self.filename,self.line_number,self.msg))


class ParseException(CompilerException):
    def __init__(self,filename,line_number,msg):
        self.filename = filename
        self.line_number = line_number
        self.msg = msg
    def __str__(self):
        return format_error_message(
            'Error in file %s on line %i: %s' %
            (self.filename,self.line_number,self.msg))


class TypeCheckException(CompilerException):
    def __init__(self,filename,line_number,msg):
        self.filename = filename
        self.line_number = line_number
        self.msg = msg
    def __str__(self):
        return format_error_message(
            'Error in file %s on line %i: %s' %
            (self.filename,self.line_number,self.msg))

class InternalTypeCheckException(CompilerException):
    def __init__(self,msg):
        self.msg = msg
    def __str__(self):
        return format_error_message(self.msg)

