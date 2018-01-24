
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChatWorker implements Runnable {

    private final Socket socket;
    private final Set<ChatWorker> workers;
    private final Executor pool;
    private final Queue<String> requests;
    private final Queue<String> responses;
    private String name;

    public ChatWorker(final Socket socket, final Set<ChatWorker> workers, final Executor pool) throws IOException {
        this.socket = socket;
        this.workers = workers;
        this.pool = pool;
        this.name = "unknown";

        this.requests = new ConcurrentLinkedQueue<>();
        this.responses = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        System.out.println(String.format("Client connected: %s", this.socket.getRemoteSocketAddress()));

        try {
            final Runnable input = new InputThread(this.socket, this.requests);
            final Runnable output = new OutputThread(this.socket, this.responses);

            pool.execute(input);
            pool.execute(output);

            boolean running = true;

            while (running) {
                // get request
                String request = null;
                synchronized (requests) {
                    request = requests.poll();
                }
                if (request != null) {
                    if (request.charAt(0) == '/') {
                        if (request.startsWith("/name")) {
                            String name = request.split(" ").length > 1 ? request.split(" ")[1] : "unknown";
                            String temp = this.name;
                            this.name = name;
                            workers.stream().filter(x -> x == this).forEach(worker -> {
                                worker.addMessage(String.format("Name changed into %s", name));
                            });
                            workers.stream().filter(x -> x != this).forEach(worker -> {
                                worker.addMessage(String.format("%s renamed into %s", temp, name));
                            });
                        } else if (request.startsWith("/w")) {
                            String target = request.split(" ")[1];
                            if (request.split(" ").length > 2) {
                                checklist(target, request.split(" ")[2]);
                            } else {
                                addMessage("Error sending empty message");
                            }
                        } else {
                            switch (request) {
                                case "/r": //:thinking:
                                    break;
                                case "/stop":
                                    addMessage("bye-bye");
                                    running = false;
                                    break;
                                case "/time":
                                    addMessage(new Date().toString());
                                    break;
                                case "/clients":
                                    addMessage(workers.stream().map(x -> x.name)
                                            .collect(Collectors.joining(", ")));
                                    break;
                                case "/memory":
                                    addMessage(String.valueOf(Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
                                            .freeMemory()));
                                    break;
                                default:
                                    addMessage(String.format("No commands found for %s",request.split(" ")[0]));
                            }
                        }
                    } else if (!request.trim().equals("")) {
                        broadcast(String.format("%s: %s", this.name, request));
                    }

                }
                synchronized (requests) {
                    try {
                        requests.wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
        }

        try {
            System.out.println(String.format("Client disconnects: %s", this.socket.getRemoteSocketAddress()));
            synchronized (workers) {
                workers.remove(this);
                broadcast(String.format("%s has left", this.name));
            }
            socket.close();
        } catch (IOException e) {
        }
    }

    public void broadcast(final String message) {
        synchronized (workers) {
            workers.stream().filter(x -> x != this).forEach(worker -> {
                worker.addMessage(String.format(message));
            });
        }
    }

    public void checklist(String target, final String message) {
        Optional<ChatWorker> a = workers.stream().filter(x -> x.name.equals(target)).findFirst();
        if (a.isPresent()) {
            workers.stream().filter(x -> x.name.equals(target)).forEach(worker -> {
                worker.addMessage(String.format("Message from %s : " + message, this.name));
            });
        } else {
            //message error
            addMessage(String.format("Cannot find user with name %s", target));
        }
    }

    public void addMessage(String message) {
        synchronized (responses) {
            responses.offer(message);
            responses.notify();
        }
    }
}
//                                                  ____
//  ___                                      .-~. /_"-._
//`-._~-.                                  / /_ "~o\  :Y
//      \  \                                / : \~x.  ` ')
//      ]  Y                              /  |  Y< ~-.__j
//     /   !                        _.--~T : l  l<  /.-~
//    /   /                 ____.--~ .   ` l /~\ \<|Y
//   /   /             .-~~"        /| .    ',-~\ \L|
//  /   /             /     .^   \ Y~Y \.^>/l_   "--'  ROAR 
// /   Y           .-"(  .  l__  j_j l_/ /~_.-~    .
//Y    l          /    \  )    ~~~." / `/"~ / \.__/l_
//|     \     _.-"      ~-{__     l  :  l._Z~-.___.--~
//|      ~---~           /   ~~"---\_  ' __[>
//l  .                _.^   ___     _>-y~
// \  \     .      .-~   .-~   ~>--"  /
//  \  ~---"            /     ./  _.-'
//   "-.,_____.,_  _.--~\     _.-~
//                   ~~     (   _}      
//                      `. ~(
//                        )  \
//                  /,`--'~\--'~\