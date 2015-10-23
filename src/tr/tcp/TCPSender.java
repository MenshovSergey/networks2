package tr.tcp;

import tr.Main;
import tr.broadcast.ErrorConectionMessage;
import tr.broadcast.Message;
import tr.broadcast.TypeMessage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ss.menshov on 21.10.2015.
 */
public class TCPSender implements Runnable {

    final ConcurrentLinkedQueue<Message> sQueue;
    final ConcurrentLinkedQueue<TypeMessage> rQueue;
    int portToSend;


    public TCPSender(ConcurrentLinkedQueue<Message> sQueue, int portToSend, ConcurrentLinkedQueue<TypeMessage> r) {
        this.sQueue = sQueue;
        this.portToSend = portToSend;
        rQueue = r;
    }

    @Override
    public void run() {
        while (true) {
            if (sQueue.isEmpty()) {
                try {
                    sQueue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Message message = sQueue.poll();
                try {
                    //TODO Maybe open coonection with target
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.bind(new InetSocketAddress(message.getDestinationAddress(), portToSend));
                    Socket socket = serverSocket.accept();
                    OutputStream out = socket.getOutputStream();
                    out.write(message.getBytes());
                    out.flush();
                } catch (IOException e) {
                    rQueue.add(new ErrorConectionMessage());
                }
            }
        }
    }
}
