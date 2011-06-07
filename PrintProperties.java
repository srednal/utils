import java.util.Enumeration;

/**
 * Print all the system properties.
 * <p>
 * Usage:
 * <pre>
 *  java PrintProperties
 * </pre>
 *
 * @author Dave Landers
 */
public class PrintProperties
{
    public static void main( String[] argsIgnored )
    {
        for ( Enumeration<?> names = System.getProperties().propertyNames(); 
              names.hasMoreElements(); )
        {
            String key = (String) names.nextElement();
            String val = System.getProperty(key, "<UNDEFINED>");
            System.out.println(key + " = " + val);
        }
    }
}
