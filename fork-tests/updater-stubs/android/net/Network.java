package android.net;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
public class Network {
 public URLConnection openConnection(URL url) throws IOException { return url.openConnection(); }
}
