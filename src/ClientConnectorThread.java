import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ClientConnectorThread implements Runnable{
    File file;

    public ClientConnectorThread(File file) {
        this.file = file;
        (new Thread(this)).start();
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket("localhost", Config.PORT);
            System.out.println("connected");

            OutputStream sos = socket.getOutputStream();
            InputStream sis = socket.getInputStream();
            PrintWriter pr = new PrintWriter(sos);

            String bodySegment = "------WebKitFormBoundaryfjLKV3YROsLnvR1Q\r\n" +
                    "Content-Disposition: form-data; name=\"uploadedfile\"; filename=\"" + file.getName() + "\"\r\n" +
                    "Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n" +
                    "\r\n";

            String tailSegment = "------WebKitFormBoundaryfjLKV3YROsLnvR1Q--\r\n\r\n";

            int contentLength = (int) (file.length() + bodySegment.getBytes(StandardCharsets.ISO_8859_1).length +
                    tailSegment.getBytes(StandardCharsets.ISO_8859_1).length);

            String header = "POST / HTTP/1.1\r\n" +
                    "Content-Length: " + contentLength + "\r\n" +
                    "Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryfjLKV3YROsLnvR1Q\r\n" +
                    "\r\n";

            pr.write(header);
            pr.write(bodySegment);
            pr.flush();

            FileInputStream fis = new FileInputStream(file);
            int read = 0;
            byte[] buff = new byte[Config.BUFFER_SIZE];

            while ((read = fis.read(buff)) >= 0) {
                sos.write(buff, 0, read);
                sos.flush();
            }

            pr.write(tailSegment);
            pr.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(sis));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                if (line.equals("")) break;
            }

            fis.close();
            br.close();
            sos.close();
            sis.close();
            socket.close();

            System.out.println("disconnected");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
