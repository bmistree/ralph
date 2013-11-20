
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
