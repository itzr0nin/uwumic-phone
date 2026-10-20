package com.uwumic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plain TCP audio server (WO Mic style).
 * On connect it sends: "UWUM" + sampleRate(int32 BE) + channels(byte) + bits(byte),
 * then raw little-endian PCM16 mono until the client disconnects.
 */
public class TcpServer {
    private static class Client {
        final Socket socket;
        final OutputStream out;
        Client(Socket s) throws IOException { socket = s; out = s.getOutputStream(); }
    }

    private final int port;
    private ServerSocket ss;
    private volatile boolean up;
    private final List<Client> clients = new CopyOnWriteArrayList<>();

    public TcpServer(int port) { this.port = port; }

    public void start() throws IOException {
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(port));
        up = true;
        new Thread(this::acceptLoop, "accept").start();
    }

    private void acceptLoop() {
        while (up) {
            try {
                Socket s = ss.accept();
                s.setTcpNoDelay(true);
                final Client c = new Client(s);
                byte[] hdr = ByteBuffer.allocate(10).order(ByteOrder.BIG_ENDIAN)
                        .put("UWUM".getBytes("US-ASCII")).putInt(State.RATE)
                        .put((byte) 1).put((byte) 16).array();
                c.out.write(hdr);
                c.out.flush();
                for (Client old : clients) drop(old); // one PC at a time, like WO Mic
                clients.add(c);
                State.clients = clients.size();
                new Thread(() -> { // notice when the PC goes away
                    try { while (c.socket.getInputStream().read() != -1) { /* ignore */ } }
                    catch (IOException ignored) { }
                    drop(c);
                }, "reader").start();
            } catch (IOException e) {
                if (!up) return;
            }
        }
    }

    public void broadcast(byte[] data) {
        for (Client c : clients) {
            try {
                c.out.write(data);
                c.out.flush();
            } catch (IOException e) {
                drop(c);
            }
        }
    }

    private void drop(Client c) {
        clients.remove(c);
        try { c.socket.close(); } catch (IOException ignored) { }
        State.clients = clients.size();
    }

    public void stop() {
        up = false;
        try { if (ss != null) ss.close(); } catch (IOException ignored) { }
        for (Client c : clients) drop(c);
    }
}
