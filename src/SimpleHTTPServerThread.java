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
    private final File rootDirectory;

    public SimpleHTTPServerThread(Socket s) throws IOException {
        socket = s;
        os = socket.getOutputStream();
        ais = new AwesomeInputStream(new BufferedInputStream(socket.getInputStream()));
        pr = new PrintWriter(os);
        log = new PrintWriter(new FileWriter(Config.LOG_FILE, true));
        rootDirectory = new File(Config.ROOT_DIR);
        (new Thread(this)).start();
    }

    private String getFilePathFromRequestPath(String request) {
        return request.split(" ")[1].replace("%20", " ");
    }

    @Override
    public void run() {
        try {
            String input = ais.readLine();

            if (input.length() <= 0) {
                return;
            }

            System.out.println(input);
            log.write(input + '\n');

            if (input.startsWith("GET")) {
                // print first line get request to log
                String filePath = getFilePathFromRequestPath(input);
                File file = new File(Config.ROOT_DIR + filePath);

                // print rest of the get request to log
                do {
                    input = ais.readLine();
                    log.write(input + '\n');
                } while (!input.equals("\r") && !input.equals(""));
                log.flush();

                if (file.exists()) {
                    if (file.isDirectory()) {
                        respondWithHtml(prepareHTMLFromDirectory(file));
                    } else if (file.isFile()) {
                        respondWithFile(file);
                    }
                } else {
                    respondWithNotFound();
                }

            } else if (input.startsWith("POST")) {
                String filePath = getFilePathFromRequestPath(input);

                String boundary = "";
                int contentLength = 0;

                while (true) {
                    input = ais.readLine();
                    log.write(input + '\n');
                    if (input.equals("\r") || input.equals("")) break;

                    if (input.contains("boundary")) {
                        String[] split = input.split("boundary=");
                        boundary = split[split.length - 1].trim();
                    } else if (input.contains("Content-Length")) {
                        String[] split = input.split("Content-Length: ");
                        try {
                            contentLength = Integer.parseInt(split[1].trim());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Could not parse: " + split[1]);
                        }
                    }
                }
                log.flush();

                boundary = "--" + boundary;
                String fileName = "";
                int extraBytes = 0;

                while (true) {
                    input = ais.readLine();
                    String s = input + '\n';
                    extraBytes += s.getBytes(StandardCharsets.ISO_8859_1).length;
                    log.write(s);
                    if (input.equals("\r") || input.equals("")) break;

                    if (input.contains("name=\"uploadedfile\"")) {
                        fileName = input.split("filename=")[1].trim();
                        fileName = fileName.substring(1, fileName.length() - 1);
                    }
                }
                if (fileName.length() > 0 && contentLength - extraBytes > 0) {
                    File out = new File( Config.ROOT_DIR+ "/" + filePath + "/" + fileName);
                    if (!out.createNewFile()) {
                        System.out.println("file already exists");
                        respondWithConflict();
                    } else {
                        System.out.println("creating new file");
                        FileOutputStream fos = new FileOutputStream(out);

                        // delete end boundary
                        String s = (boundary + "--\r\n\r\n");
                        extraBytes += s.getBytes(StandardCharsets.ISO_8859_1).length;
                        contentLength -= extraBytes;

                        byte[] buff = new byte[Config.BUFFER_SIZE];
                        int read = 0;
                        while (contentLength > 0 && ((read = ais.read(buff))) > 0) {
                            fos.write(buff, 0, Math.min(read, contentLength));
                            contentLength -= read;
                        }
                        fos.flush();

                        System.out.println("Done writing");
                        respondWithHtml(prepareHTMLFromDirectory(rootDirectory));
                    }
                } else {
                    respondWithNoContent();
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

    private String getRelativePathWrtRoot(File file) {
        return rootDirectory.toPath().relativize(file.toPath()).toString();
    }


    private String prepareHTMLFromDirectory(File file) {

        StringBuilder sb = new StringBuilder();

        for (File f : file.listFiles()) {

            if (f.isFile()) {
                sb.append("<a href=/" +
                        getRelativePathWrtRoot(f).replace(" ", "%20") +
                        ">" + f.getName() + "</a><br>");
            } else if (f.isDirectory()) {
                sb.append("<a href=/" +
                        getRelativePathWrtRoot(f).replace(" ", "%20") +
                        "><strong>" + f.getName() + "</strong></a><br>");
            }
        }

        sb.append("<br><br><hr><form enctype=\"multipart/form-data\" action=\"/\" method=\"POST\">\n" +
                "Choose a file to upload: <input name=\"uploadedfile\" type=\"file\" /><br />\n" +
                "<input type=\"submit\" value=\"Upload File\" />\n" +
                "</form>");

        return sb.toString();
    }

    private void respondWithText(String responseCode, String body) {
        String content = "HTTP/1.1 " + responseCode + "\r\n" +
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

    private void respondWithFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
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

            int read = 0;
            byte[] buff = new byte[Config.BUFFER_SIZE];

            while ((read = fis.read(buff)) > 0) {
                os.write(buff, 0, read);
                os.flush();
            }
        } catch (FileNotFoundException e) {
            respondWithNoContent();
        }
    }

    private void respondWithNoContent() {
        respondWithText("204 No Content", prepareHTMLFromDirectory(rootDirectory));
    }

    private void respondWithConflict() {
        respondWithText("409 Conflict", prepareHTMLFromDirectory(rootDirectory));
    }

    private void respondWithHtml(String html) {
        respondWithText("200 OK", html);
    }

    private void respondWithNotFound() {
        respondWithText("404 Not Found", "");
    }
}
