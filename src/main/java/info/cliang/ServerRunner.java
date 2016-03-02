package info.cliang;

import java.io.IOException;

public class ServerRunner {
    public static void main(String args[]) {
        try {
            TinyServer tinyServer = new TinyServer(8080, "/Users/lcchen/dev/site");
            tinyServer.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
