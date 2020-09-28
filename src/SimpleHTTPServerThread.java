import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SimpleHTTPServerThread implements Runnable{
    private final Socket socket;
    private final OutputStream os;
    private final AwesomeInputStream ais;
    private final PrintWriter pr;
    private final PrintWriter log;

    private final File root = new File("./root");
    private final int BUFFER_SIZE = 8*1024;

    public SimpleHTTPServerThread(Socket s) throws IOException {
        socket = s;
        os = socket.getOutputStream();
        ais = new AwesomeInputStream(new BufferedInputStream(socket.getInputStream()));
        pr = new PrintWriter(os);
        log = new PrintWriter(new FileWriter("log.txt", true));
        (new Thread(this)).start();
    }

    @Override
    public void run() {
        try {
            String input = ais.readLine();
            System.out.println(input);
            
            if(input.length() > 0) {
                if (input.startsWith("GET")) {
                    // print first line get request to log
                    log.write(input + '\n');
                    String filePath = input.split(" ")[1].replace("%20", " ");
                    File file = new File("./root" + filePath);

                    // print rest of the get request to log
                    while (true) {
                        input = ais.readLine();
                        log.write(input + '\n');
                        if (input.equals("\r") || input.equals("")) break;
                    }
                    log.flush();

                    if (file.exists()) {
                        if (file.isDirectory()) {
                            writeHTML(prepareHTMLFromDirectory(file));
                        } else if (file.isFile()) {
                            writeFile(file);
                        }
                    } else {
                        writeNotFound();
                    }

                } else if (input.startsWith("POST")) {
                    log.write(input + '\n');
                    String filePath = input.split(" ")[1];

                    String boundary = "";
                    int contentLength = 0;

                    while (true) {
                        input = ais.readLine();
                        log.write(input + '\n');
                        if (input.equals("\r") || input.equals("")) break;

                        if (input.contains("boundary")) {
                            String [] split = input.split("boundary=");
                            boundary = split[split.length - 1].trim();
                        } else if (input.contains("Content-Length")) {
                            String [] split = input.split("Content-Length: ");
                            try {
                                contentLength = Integer.parseInt(split[1].trim());
                            } catch (Exception e) {
                                e.printStackTrace();
                                System.out.println("could not parse: " + split[1]);
                            }
                        }
                    }
                    log.flush();

                    boundary = "--" + boundary;
                    String name = "UNKNOWN", fileName = "";
                    int extraBytes = 0;

                    while (true) {
                        input = ais.readLine();
                        String s = (input + '\n');
                        extraBytes += s.getBytes(StandardCharsets.ISO_8859_1).length;
                        log.write(input + '\n');
                        if (input.equals("\r") || input.equals("")) break;

                        if (input.contains("name=\"uploadedfile\"")) {
                            fileName = input.split("filename=")[1].trim();
                            fileName = fileName.substring(1, fileName.length() - 1);
                            //System.out.println(fileName);
                            name = "UPLOADFILE";
                        }
                    }
                    if (name.equals("UPLOADFILE") && contentLength - extraBytes > 0) { // TODO : do better
                        File out = new File("./root/" + filePath + "/" + fileName);
                        if (out.exists()) {
                            System.out.println("file already exists");

                            String content = "HTTP/1.1 409 Conflict\r\n" +
                                    "Server: Java HTTP Server: 1.0\r\n" +
                                    "Date: " + new Date() + "\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    prepareHTMLFromDirectory(new File("./root/"));
                            pr.write(content);
                            pr.flush();

                            log.write(content);
                            log.flush();

                        } else {
                            System.out.println("creating new file");
                            out.createNewFile();
                            FileOutputStream fos = new FileOutputStream(out);

                            // delete end boundary
                            String s = (boundary + "--\r\n\r\n");
                            extraBytes += s.getBytes(StandardCharsets.ISO_8859_1).length;
                            contentLength -= extraBytes;

                            byte[] buff = new byte[BUFFER_SIZE];
                            int read = 0;
                            while (contentLength > 0 && ((read = ais.read(buff))) > 0) {
                                fos.write(buff, 0, Math.min(read, contentLength));
                                contentLength -= read;
                            }
                            fos.flush();

                            System.out.println("done writing");

                            String content = "HTTP/1.1 200 OK\r\n" +
                                    "Server: Java HTTP Server: 1.0\r\n" +
                                    "Date: " + new Date() + "\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    prepareHTMLFromDirectory(new File("./root/"));
                            pr.write(content);
                            pr.flush();

                            log.write(content);
                            log.flush();
                        }
                    } else {
                        // TODO : send nothing done response
                        String content = "HTTP/1.1 204 No Content\r\n" +
                                "Server: Java HTTP Server: 1.0\r\n" +
                                "Date: " + new Date() + "\r\n" +
                                "Content-Type: text/html\r\n" +
                                "\r\n" +
                                prepareHTMLFromDirectory(new File("./root/"));
                        pr.write(content);
                        pr.flush();

                        log.write(content);
                        log.flush();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private String prepareHTMLFromDirectory(File file) {

        StringBuilder sb = new StringBuilder();

        for (File f : file.listFiles()) {

            if (f.isFile()) {
                sb.append("<a href=/").append(root.toPath().relativize(f.toPath()).toString().replace(" ", "%20")).append(">").append(f.getName()).append("</a><br>");
            } else if (f.isDirectory()) {
                sb.append("<a href=/").append(root.toPath().relativize(f.toPath()).toString().replace(" ", "%20")).append("><strong>").append(f.getName()).append("</strong></a><br>");
            }
        }

        sb.append("<br><br><hr><form enctype=\"multipart/form-data\" action=\"/\" method=\"POST\">\n" +
                "Choose a file to upload: <input name=\"uploadedfile\" type=\"file\" /><br />\n" +
                "<input type=\"submit\" value=\"Upload File\" />\n" +
                "</form>");


        return sb.toString();
    }

    private void writeHTML(String body) {
        String content = "HTTP/1.1 200 OK\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                body;
        pr.write(content);
        pr.flush();

        log.write(content);
        log.flush();
    }

    private void writeFile(File file) throws IOException {
        String content = "HTTP/1.1 200 OK\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: application/force-download\r\n" +
                "Content-Length: " + file.length() + "\r\n" +
                "\r\n";
        pr.write(content);
        pr.flush();

        log.write(content);
        log.flush();

        FileInputStream fis = new FileInputStream(file);
        int read = 0;
        byte[] buff = new byte[BUFFER_SIZE];

        while ((read = fis.read(buff)) > 0) {
            os.write(buff, 0, read);
            os.flush();
        }
    }

    private void writeNotFound() {
        String content = "HTTP/1.1 404 Not Found\r\n" +
                "Server: Java HTTP Server: 1.0\r\n" +
                "Date: " + new Date() + "\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n";

        pr.write(content);
        pr.flush();

        log.write(content);
        log.flush();
    }
}
