

import java.util.StringTokenizer;
import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;

/**
 * Search class path for some class and report where it is found.
 * The class name can be fully qualified (like java.lang.String) or
 * just the base class name (like String).
 * <p>
 * All matches are reported, in the order they are found.  Both
 * directories and jar files are searched.
 * <p>
 * Usage:
 * <pre>
 *  java FindClass SomeClassName
 *  java FindClass some.package.SomeClassName
 *  java FindClass some.package.
 * </pre>
 *
 * @author Dave Landers - dave@sredna;l.com
 * 
 * Disclaimer:  No warantee of any kind, use at your own risk.
 */
public class FindClass
{
    /**
     * If true, only reports the first match, otherwise reports all.
     * Default = false.  
     * Override with system property -DfirstOnly
     */
    public static boolean FIRST_ONLY =
        System.getProperty("firstOnly") != null;

    /**
     * If true, searches for all classes in some package, rather
     * than searching for a class somewhere.
     * Set if search name ends with "."
     */
    public static boolean PACKAGE_SEARCH = false;

    /**
     * If true, report only the class name (not the location).
     * Default = false.
     * Override with system property -DnoLocation
     */
    public static boolean REPORT_CLASS_ONLY =
        System.getProperty("noLocation") != null;
    
    public static void main(String[] args)
    {
        if (args.length == 0 || args[0].startsWith("-h"))
        {
            System.out.println("Usage:  java " 
                               + "[ -DfirstOnly ] "
                               + "[ -DnoLocation ] "
                               + FindClass.class.getName() + " name ");
            System.out.println("  name may be:" );
            System.out.println("     A fully qualified class with package" );
            System.out.println("     A plain class with no package (will search all packages)" );
            System.out.println("     A package name ending with \".\" (will print all classes in package" );
            System.out.println("  Options:");
            System.out.println("    -DfirstOnly   Only report the first class found.  The default is");
            System.out.println("                  to search entire classpath and report all matches");
            System.out.println("    -DnoLocation  Do not report location (directory or jar from");
            System.out.println("                  classpath) where class was found");
            System.exit(1);
        }

        // Get class name from args
        String className = args[0];

        // If it has package stuff, assume it's fully qualified
        boolean isFullName = className.indexOf('.') != -1;

        // Convert class name to file name
        className = className.replace('.', File.separatorChar);

        PACKAGE_SEARCH = className.endsWith( File.separator );

        // Strip off ending / in package or Add .class to file name
        if ( PACKAGE_SEARCH ) 
        {
            className = className.substring(0, className.length()-1);
        }
        else
        {
            className += ".class";
        }
        
        // Build classpath
        StringBuffer classpath = new StringBuffer();

        // Add boot classpath
        classpath.append( System.getProperty( "sun.boot.class.path", "" ) )
          .append(File.pathSeparatorChar);

        // add ext dirs
        classpath.append( getExtClasses() )
          .append(File.pathSeparatorChar);
          
        // Add user classpath
        classpath.append( System.getProperty( "java.class.path", "" ) )
          .append(File.pathSeparatorChar);

        
        // Tokenize classpath
        StringTokenizer st = new StringTokenizer( classpath.toString(),
                                                  File.pathSeparator );

        // Search path for class file
        while (st.hasMoreTokens())
        {
            String cp = st.nextToken();
            if (cp.equals("")) continue;  // empty one
            
            File f = new File( cp );

            if (f.isDirectory())
            {
                // Search directory for the class
                if ( searchDir( f, "", className, !isFullName ) )
                {
                    // Stop if that's what you want
                    if (FIRST_ONLY) break;
                }
            }
            else if (f.isFile())
            {
                try
                {
                    // Search jar for the class
                    if ( searchJar( f, new JarFile(f), className, !isFullName ) )
                      {
                          // Stop if that's what you want
                          if (FIRST_ONLY) break;
                      }
                 }
                  catch (Exception ignore) {}
            }
        }
    }
    
    /** Search directory for class file */
    public static boolean searchDir( File dir, String subDir,
                                     String name, boolean recurse )
    {        
        boolean found = false;
        File d = new File(dir, subDir );
        File f = new File( d, name );

        if (f.exists())
        {
            if ( PACKAGE_SEARCH )
            {
                String[] list = f.list();
                for (int i=0; i<list.length; ++i)
                {
                    String rptName = name + "." + list[i];

                    if ( new File( f, list[i] ).isDirectory() )
                    {
                        rptName += ".";
                    }

                    report( dir, rptName );
                }
            }
            else
            {
                report( dir, subDir + "." + name );
                found = true;
                if (FIRST_ONLY) return found;
            }
        }
        
        if (recurse)
        {
            File[] files = d.listFiles();
            for (int i=0; i<files.length; ++i)
            {
                if (files[i].isDirectory())
                {
                    if ( searchDir( dir,
                                    subDir +File.separator+ files[i].getName(),
                                    name, recurse ) )
                    {
                        found = true;
                        if (FIRST_ONLY) return found;
                    }
                }
            }
        }
        return found;
    }

    /** Search jar for class file */
    public static boolean searchJar( File cp, JarFile jar,
                                     String name, boolean recurse )
    {

        name = name.replace(File.separatorChar, '/');

        boolean found = false;
        
        Enumeration<?> e = jar.entries();
        
        while (e.hasMoreElements())
        {
            String entry = ((JarEntry) e.nextElement()).getName();
 
            if (PACKAGE_SEARCH)
            {
                if ( entry.startsWith(name) )
                {
                    String ending = entry.substring( name.length() + 1 );
                    int slash = ending.indexOf('/');
                    if ( slash == -1 || slash == ending.length()-1 )
                    {
                        report( cp, entry );
                    }
                }
            }
            else
            {
                if ( ( recurse && entry.endsWith("/"+name) )
                     || entry.equals( name ) )
                {
                    report( cp, entry );
                    found = true;
                    if (FIRST_ONLY || (!recurse)) return found;
                }
            }
        }

        return found;
    }

    /** Print report when something found */
    public static void report( File cp, String name )
    {
        // remove .class
        if ( name.endsWith(".class") )
            name = name.substring( 0, name.length()-6);

        name = name.replace('/', '.').replace(File.separatorChar, '.');

        while ( name.startsWith(".") )
            name = name.substring(1);

        System.out.print( name );
        if ( ! REPORT_CLASS_ONLY )
        {
            System.out.print( "  [" + cp + "]" );
        }
        System.out.println();
    }


    /** Get class path from ext dirs (jre/lib/ext) */
    public static String getExtClasses()
    {
        StringBuffer classpath = new StringBuffer();
        
        String extDirs = System.getProperty( "java.ext.dirs" );
        if (extDirs != null && extDirs.length() != 0)
        {
            StringTokenizer st = new StringTokenizer( extDirs,
                                                      File.pathSeparator );
            while (st.hasMoreTokens() )
            {
                String dir = st.nextToken();
                File[] f = new File( dir ).listFiles();
                if (f != null)
                {
                    for (int i=0; i<f.length; ++i)
                    {
                        classpath.append( f[i].getAbsolutePath() )
                          .append( File.pathSeparatorChar );
                    }
                }
            }
        }
        return classpath.toString();
    }
    
}
