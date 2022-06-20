import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class SimpleHTTPServer {
    ServerSocket ss;

    public SimpleHTTPServer() throws IOException {
        ss = new ServerSocket(Config.PORT);
        System.out.println("Server started.\nListening for connections on port : " + Config.PORT + " ...\n");
    }

    public void run() throws IOException {
        while (true) {
            Socket s = ss.accept();
            new SimpleHTTPServerThread(s);
        }
    }

    public static void main(String[] args) throws IOException {
        File rootDirectory = new File(Config.ROOT_DIR);
        if (!rootDirectory.exists()) {
            rootDirectory.mkdir();
        }

        SimpleHTTPServer server = new SimpleHTTPServer();
        server.run();
    }
}
