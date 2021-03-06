package net.vinote.demo;

import org.smartboot.socket.transport.AioQuickServer;

import java.io.IOException;

/**
 * Created by 三刀 on 2017/7/12.
 */
public class IntegerServer {
    public static void main(String[] args) {
        AioQuickServer<Integer> server = new AioQuickServer<Integer>(8888, new IntegerProtocol(), new IntegerServerProcessor());
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
