import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

class ThreadedInputStream extends Thread {
    private final Socket socket;
    private final DataInputStream inputStream;
    private final CountDownLatch signal;

    ThreadedInputStream(Socket socket, CountDownLatch signal) throws IOException {
        this.socket = socket;
        this.inputStream = new DataInputStream(this.socket.getInputStream());
        this.signal = signal;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String response = inputStream.readUTF();
                System.out.println(response);
            } catch (EOFException e) {
                break;
            } catch (IOException e) {
                break;
            }
        }

        try {
            this.inputStream.close();
            signal.countDown();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}