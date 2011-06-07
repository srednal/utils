import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Connect an input and output stream.  Use the connect method to 
 * automatically connect the streams in a separate thread.
 */
public class StreamConnector
    implements Runnable
{
    private BufferedInputStream from;
    private BufferedOutputStream to;
    private byte[] buffer;

    public StreamConnector( InputStream from, OutputStream to, int bufferSize )
    {
        // Use buffered streams
        if ( from instanceof BufferedInputStream)
        {
            this.from = (BufferedInputStream) from;
        }
        else
        {
            this.from = new BufferedInputStream( from, bufferSize );
        }

        if ( to instanceof BufferedOutputStream )
        {
            this.to = (BufferedOutputStream) to;
        }
        else
        {
            this.to = new BufferedOutputStream( to, bufferSize );
        }

        this.buffer = new byte[ bufferSize ];
    }

    public void run()
    {
        // read/write loop
        try
        {
            while (true)
            {
                int len = from.read( buffer );
    
                if (len == -1) break;
    
                to.write( buffer, 0, len );
            }
        }
        catch (IOException e)
        {
            // TODO
        }
        finally
        {
            try
            {
                to.flush();
            }
            catch (IOException ignore)
            {
            }
        }
    }

    public static void connect( InputStream from, OutputStream to, int bufferSize )
    {
        new Thread( new StreamConnector( from, to, bufferSize ) ).start();
    }


}
