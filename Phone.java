
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * Do lookup by name for email and phone numbers using
 * the BEA LDAP server.
 * <br>
 * Usage:
 * <pre>
 *   java Phone searchString
 * </pre>
 * searchString is used to search for the common name (cn).
 * If searchString is not supplied, all the entries are printed.
 *
 * @author Dave Landers
 */
public class Phone
{

    /** URL to LDAP Server */
    public static final String PROVIDER_URL =
            "ldap://ldap.oracle.com:389"; // Oracle

    // Initial context factory class
    public static final String INITIAL_CONTEXT_FACTORY =
        com.sun.jndi.ldap.LdapCtxFactory.class.getName();

    // Search base
    public static final String SEARCH_BASE = 
        "dc=oracle, dc=com"; // Oracle
        // "ou=people, dc=metaview, dc=bea, dc=com"; // BEA
        // "ou=people,o=beasys.com"; // BEA - old (sf-ldap)
        // "o=avitek.com"; // Avitek

    // Search on cn (common name).  {0} is filter arg from args[0]
    public static final String FILTER_EXPR = "(cn=*{0}*)";
        // "(|(cn=*{0}*)(mail=*{0}*))";

    /** Attributes to retrieve and display (in display order) */
    public static final  Map<String,String> ATTRIBUTES = new LinkedHashMap<String,String>();
    static
    {
        ATTRIBUTES.put( "cn", "Name" );
        ATTRIBUTES.put( "uid", "User ID" );
        ATTRIBUTES.put( "mail", "Mail" );
        ATTRIBUTES.put( "telephonenumber", "Phone" );
        ATTRIBUTES.put( "ou", "Organization" );
        ATTRIBUTES.put( "title", "Title" );
    }

    /** See class docs */
    public static void main(String[] args)
    {
        try
        {
            // Establish initial context
            Hashtable<String,String> env = new Hashtable<String,String>();
            env.put( Context.PROVIDER_URL, PROVIDER_URL );
            env.put( Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY );

            DirContext ctx = new InitialDirContext(env);

            // specify search constraints to search subtree
            SearchControls constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            constraints.setReturningAttributes( ATTRIBUTES.keySet().toArray(new String[ATTRIBUTES.size()]) );

            if (args.length == 0) 
            {
                System.err.println( "Usage: java " + Phone.class.getName() + " name-or-email..." );
                System.exit(1);
            }
            
            // Loop thru each name arg
            for ( String arg : args )
            {
                // Search using args[0] as filter
                NamingEnumeration<SearchResult> results = 
                    ctx.search( SEARCH_BASE, FILTER_EXPR,
                                new String[] { arg }, constraints );
    
                // Print results
                for ( SearchResult si : Collections.list(results ) )
                {
                        Attributes attrs = si.getAttributes();
                        if (attrs != null)
                        {
                            for ( String attrName : ATTRIBUTES.keySet() )
                            {   
                                String val = getAttribute( attrs, attrName );
                                if ( val != null )
                                {
                                    String dispName = ATTRIBUTES.get( attrName );
                                    if (dispName == null) dispName = attrName;
    
                                    System.out.println( dispName + ": " + val );
                                }
                            }
                            System.out.println();
                        }
                }
            }
        }
        catch (NamingException e)
        {
            System.out.println("Error contacting LDAP server "  + PROVIDER_URL);
            e.printStackTrace();
        }
    }

    public static String getAttribute( Attributes attrs, String name )
    {
        Attribute attr = attrs.get( name );
        
        if (attr == null) return null;
                
        Object val = null;
        try
        {
            val = attr.get();
        }
        catch (NamingException ignore) { }
            
        if (val == null) return null;

        return String.valueOf( val );
    }
}

