import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

class ThreadedOutputStream extends Thread {
    private final Socket socket;
    private final DataOutputStream outputStream;
    private final CountDownLatch signal;

    ThreadedOutputStream(Socket socket, CountDownLatch signal) throws IOException {
        this.socket = socket;
        this.outputStream = new DataOutputStream(this.socket.getOutputStream());
        this.signal = signal;
    }

    @Override
    public void run() {
        Scanner input = new Scanner(System.in);

        while (true) {
            String request = input.nextLine();

            try {
                outputStream.writeUTF(request);
            } catch (IOException e) {
                break;
            }
        }

        try {
            outputStream.close();
            signal.countDown();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}