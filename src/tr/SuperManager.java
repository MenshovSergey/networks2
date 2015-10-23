package tr;

import tr.broadcast.*;
import tr.tcp.TCPManager;
import tr.utils.BroadcastResult;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by ks.kochetov on 20.10.2015.
 */
public class SuperManager {
    BroadcastManager bManager;
    TCPManager tcpManager;
    StateMachine mStateMachine;
    TCPHandler tcpHandler;
    Timer mTimer = new Timer();
    InetAddrsComparator inetAddrsComparator = new InetAddrsComparator();
    ConcurrentLinkedQueue<TypeMessage> eventQueue;


    public SuperManager(StateMachine sm) throws SocketException, UnknownHostException {
        mStateMachine = sm;
        bManager = new BroadcastManager(sm);
         eventQueue = new ConcurrentLinkedQueue<>();
        tcpManager = new TCPManager(eventQueue, sm.tcpPort);
        tcpHandler = new TCPHandler(eventQueue);
        tcpHandler.run();
    }

    public void start() {
        mTimer.schedule(new ClaimTokenSendTask(System.currentTimeMillis()), mStateMachine.delay, mStateMachine.delay);
//        while (true) {
//            if (mStateMachine.hasToken) {
//                if (mStateMachine.nextStation == null) {
//                    sendSS2();
//                } else {
//                    sendSS1();
//                }
//            }
//        }

    }

    public void run() {

    }

    private void sendMessage(InetAddress to) {
        mStateMachine.nextStation = to;
        tcpManager.sendMessage(new Message(mStateMachine.myAddrs, mStateMachine.nextStation, FC.T, 0, null));
    }

    private void sendSS2() {
        bManager.sendSS2ByLeader(new BroadcastResult<List<InetAddress>>() {
            @Override
            public void onResult(List<InetAddress> resultBuffer) {
                for (InetAddress i : resultBuffer) {
                    if (inetAddrsComparator.compare(i, mStateMachine.myAddrs) == -1) {
                        mStateMachine.nextStation = i;
                        tcpManager.sendMessage(new Message(mStateMachine.myAddrs, mStateMachine.nextStation, FC.T, 0, null));
                        return;
                    }
                }
                sendMessage(resultBuffer.get(resultBuffer.size() - 1));
            }
        });
    }

    private void sendSS1() {
        bManager.sendSSByLeader(new BroadcastResult<List<InetAddress>>() {
            @Override
            public void onResult(List<InetAddress> resultBuffer) {
                if (resultBuffer.size() > 0) {
                    sendMessage(resultBuffer.get(resultBuffer.size() - 1));
                } else {
                    sendMessage(mStateMachine.nextStation);
                }
            }
        });
    }




    public void onMessageReceive(Message msg) {
        Data dataToSend = msg.data.update();
        //sendDataToSuccessor(dataToSend);
    }

    class ClaimTokenSendTask extends TimerTask {
        ClaimTokenSendTask(long l) {

        }

        @Override
        public void run() {
            long lastTime = System.currentTimeMillis();
            if (!mStateMachine.hasToken && lastTime - mStateMachine.lastTimeMessage > mStateMachine.delay) {
                bManager.sendClaimToken();

            }
        }
    }

    class TCPHandler implements Runnable {
        ConcurrentLinkedQueue<TypeMessage> rQueue;

        public TCPHandler(ConcurrentLinkedQueue<TypeMessage> rQueue) {
            this.rQueue = rQueue;
        }

        @Override
        public void run() {
            while (true) {
                if (!rQueue.isEmpty()) {
                    onReceive(rQueue.poll());
                } else {
                    try {
                        rQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void onReceive(TypeMessage message) {
            if (message instanceof ErrorConectionMessage) {
                sendSS2();
            } else {
                if (message instanceof Message) {
                    sendSS1();
                }
            }
            //onMessageReceive(message);
        }
    }

}
