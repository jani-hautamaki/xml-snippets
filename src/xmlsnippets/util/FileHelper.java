//*******************************{begin:header}******************************//
//     XML Processing Snippets - https://code.google.com/p/xml-snippets/     //
//***************************************************************************//
//
//      xml-snippets:   XML Processing Snippets 
//                      with Some Theoretical Considerations
//
//      Copyright (C) 2012 Jani Hautamäki <jani.hautamaki@hotmail.com>
//
//      Licensed under the terms of GNU General Public License v3.
//
//      You should have received a copy of the GNU General Public License v3
//      along with this program as the file LICENSE.txt; if not, please see
//      http://www.gnu.org/licenses/gpl-3.0.html
//
//********************************{end:header}*******************************//
package xmlsnippets.util;

// java core imports
import java.io.File;
import java.io.IOException;

/**
 * Some helper methods for working with Java File objects.
 *
 */
public class FileHelper {


    // CONSTRUCTORS
    //==============
    
    /**
     * Construction is intentionally disabled
     */
    private FileHelper() {
    } // ctor
    
    
    // CLASS METHODS
    //===============
    
    /**
     * Returns the extension part of the file name (with the dot).
     * 
     * @param fileName the file name as a string.
     *
     * @return the extension including the dot, or an empty string
     * if there is not a single dot present in the file name.
     */
    public static String getExtension(String fileName) {
        // Find the last instance of a dot.
        int i = fileName.lastIndexOf('.');
        
        // If there's no dot in the name, use the length of the string
        // so that String.substring() will return an empty string.
        if (i == -1) {
            i = fileName.length();
        }
        
        // Return a subsequence starting at "i", the character
        // at "i" will be the first letter, ie. the dot.
        return fileName.substring(i);
    } // getExtension()

    
    /**
     * Returns the relative path to a file with respect to a parent directory.
     * The method can be used to determine whether the given File objects
     * form a parent-child relationship. If they do form such a relationship,
     * the relative path from the parent to the child is returned. If they
     * don't form a parent-child relationship, {@code null} is returned.
     *
     * @param parent the assumed parent (a directory)
     * @param child the assumed child of the parent (a directory or a file)
     *
     * @return the relative path from parent to file as {@code File} object, 
     * or {@code null} if the parent and the child are not related.
     *
     * @throws IOException If caused by {@link File#getCanonicalFile}.
     */
    public static File getRelativePath(File parent, File child) 
        throws IOException
    {
        // Canonicalize parent
        parent = parent.getCanonicalFile();
        // Canonicalize the child into a local variable
        File file = child.getCanonicalFile();
        
        // Create a relative path to the child. The relative path is
        // constructed sequentially during the loop. The construction
        // begins with the most nested directory and then adds new directory
        // the front of the existing path.
        File rval = new File(file.getName());
        
        // Repeat while the file has a parent.
        while ((file = file.getParentFile()) != null) {
            // If the current directory equals to the parent directory,
            // then we're done.
            if (parent.equals(file)) {
                return rval;
            }
            // Prepend with the current directory
            rval = new File(file.getName(), rval.getPath());
        } // while
        // The loop traversed the whole hierarchy up, and parent was not
        // encountered. It must be concluded that the parent wasn't really
        // a parent of the child.
        return null;
    } // getRelativePath()
    
    
    /**
     * Search for a given file by sequentially looking up each parent
     * directory from a certain starting directory.
     *
     * @param cwd the starting directory for the search
     * @param fileName the name of the file that is being searched for
     *
     * @return File object pointing to matching file in the first parent
     * directory, or null if there is no matching file in any parent directory.
     *
     * @throws IOException May be caused by {@link File#getCanonicalFile}.
     */
    public static File discoverFileByAscendingDirs(File cwd, String fileName) 
        throws SecurityException, IOException
    {
        if (cwd.isDirectory() == false) {
            throw new IllegalArgumentException(String.format(
                "Not a directory: %s", cwd.getPath()));
        } // if: cwd is not a directory (existing)
        
        File file = null;
        
        while (cwd != null) {
            // Translate the cwd into a canonical file.
            // Note: this works even without canonicalization.
            cwd = cwd.getCanonicalFile();
            
            //System.out.printf("canonical cwd = <%s>\n", cwd.getPath());
            
            // Create a new file for the possibly existing manifest file
            // in the current directory.
            file = new File(cwd, fileName);
            
            // Test if such a file exist, and if it does, return it 
            // immediately.
            if (file.isFile() == true) {
                return file;
            } // if
            
            // Retrieve the parent of the cwd.
            // If there is no parent, 
            cwd = cwd.getParentFile();
        } // while: has a parent directory
        
        return null; // no matching file and no more praent dirs.
    } // discoverFileByAscending()
    
} // FileHelper