import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class SimpleHTTPServer {
    static final int PORT = 8080;
    ServerSocket ss;

    public SimpleHTTPServer() throws IOException {
        ss = new ServerSocket(PORT);
        System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
    }

    public void run() throws IOException {

        while (true) {
            Socket s = ss.accept();
            new SimpleHTTPServerThread(s);
        }
    }

    public static void main(String[] args) throws IOException {
        SimpleHTTPServer server = new SimpleHTTPServer();
        server.run();
    }
}
