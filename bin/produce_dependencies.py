import os

from bin.preprocessor_parse_file import preprocessor_parse

from ralph.common.compiler_exceptions import CompilerException
from ralph.preprocessor_parse.preprocessor_node import IncludeStatementNode


class DependencyException(CompilerException):
    def __init__(self,fq_requesting_filename,what_requesting):
        self.fq_requesting_filename = fq_requesting_filename
        self.what_requesting = what_requesting

    def __str__(self):
        return (
            'Error: %s requesting an include from unknown %s ' %
            (self.fq_requesting_filename,self.what_requesting))


def dependencies_list(input_filename,lib_dir_list):
    '''
    Args:
        input_filename: {String} Can be relative or absolute.

        lib_dir_list: {list} Each element is a string.  The
        directories that we should look for libraries in.  Note that
        if two directories contain the same file, we'll import the one
        that appears earlier in the list.  Paths in lib_dir_list may
        be absolute or relative.
        
    Returns:
        {list} --- Should parse files from lowest index to highest
        index.  Does not include input_filename.
    '''
    fq_input_filename = os.path.abspath(input_filename)

    fq_lib_dir_list = map(
        lambda path: os.path.abspath(path),
        lib_dir_list)

    to_return = dfs_include(fq_input_filename,fq_lib_dir_list)
    # Dependency list should not include input filename.
    to_return = to_return[:-1]
    return to_return

def dfs_include(fq_filename,lib_dir_list,already_added_set = None):
    '''
    Args:
        fq_filename: {string} fully-qualified name of file to get
        dependencies for.

        lib_dir_list: {list} Each element is a string representing a
        folder to search for dependency libraries in.  Note: the order
        for searching for includes is 1) relative to current folder of
        file looking for includes for, 2) ascending indices of
        lib_dir_list.

        already_added_set: {set or None} None if invoked from first,
        non-recursive caller.  A set of dependencies that we have
        already added to list of files to download.
        
    Returns:
        An ordered list of strings.  Filenames with earlier indices
        should be downloaded first.
    '''
    # list to return... lower indices mean to download sooner.
    ordered_download_list = []
    
    if already_added_set is None:
        already_added_set = set()

    dependency_set = single_file_deps(fq_filename,lib_dir_list)
    for dependency in dependency_set:
        if dependency in already_added_set:
            continue

        ordered_download_list += dfs_include(
            dependency,lib_dir_list,already_added_set)
        already_added_set.add(dependency)

    ordered_download_list.append(fq_filename)
    already_added_set.add(fq_filename)

    return ordered_download_list
    
    


def single_file_deps(fq_filename,lib_dir_list):
    '''
    Args:
        fq_filename: {string} fully-qualified name of file to get
        dependencies for.

        lib_dir_list: {list} Each element is a string representing a
        folder to search for dependency libraries in.  Note: the order
        for searching for includes is 1) relative to current folder of
        file looking for includes for, 2) ascending indices of
        lib_dir_list.
        
    Returns:
        A set of strings.  Each string is an included file we depend
        on.
    '''
    # ensures uniqueness of dependencies.
    dep_set = set()
    
    fq_input_dir = os.path.dirname(fq_filename)
    
    preprocessor_statement_list_node = preprocessor_parse(fq_filename)
    for node in preprocessor_statement_list_node.statement_list:
        # only care about include nodes
        if isinstance(node,IncludeStatementNode):
            # the relative filename of the ralph library we're trying
            # to import.
            next_to_try_to_include = node.where_to_include_from

            found_dependency = False
            
            # Look first relative to existing file's location, and
            # then relative to other library directories
            
            # FIXME: we're making an assumption that we won't
            # have circular dependencies.
            for folder in [fq_input_dir] + lib_dir_list:
                fq_next_to_try_to_include = os.path.join(
                    folder,next_to_try_to_include)

                if os.path.exists(fq_next_to_try_to_include):
                    dep_set.add(fq_next_to_try_to_include)
                    found_dependency = True
                    break

            if not found_dependency:
                raise DependencyException(
                    fq_input_filename,next_to_try_to_include)

    return dep_set
