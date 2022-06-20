import java.io.File;
import java.util.Scanner;

public class Client {

    public void run() {
        Scanner sc = new Scanner(System.in);
        String input;

        while (true) {
            input = sc.nextLine();
            if (input.equalsIgnoreCase("q")) break;

            if (input.startsWith("upload")) {
                String[] splits = input.split(" ");
                if (splits.length < 2) {
                    System.out.println("please provide file name");
                    continue;
                }
                File file = new File(splits[1]);

                if (file.exists() && file.isFile()) {
                    new ClientConnectorThread(file);
                } else {
                    System.out.println("file does not exist or is a directory");
                }

            } else if (input.startsWith("ls")) {
                String[] splits = input.split(" ");
                String pathName = ".";
                if (splits.length >= 2)
                    pathName = splits[1];

                File file = new File(pathName);

                for (String s : file.list()) {
                    System.out.println(s);
                }
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}

