import java.io.BufferedInputStream;
import java.io.IOException;

public class AwesomeInputStream {
    private BufferedInputStream bis;

    public AwesomeInputStream(BufferedInputStream bis) {
        this.bis = bis;
    }

    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int read = 0;
        while ((read = bis.read()) >= 0) {
            char c = (char) read;
            if (c != '\n') sb.append(c);
            else break;
        }
        return sb.toString();
    }

    public int read(byte[] buff) throws IOException {
        return bis.read(buff);
    }
}
