package com.bos.bce.baidu.meituapp.server;

import org.eclipse.jetty.server.Server;

public class MeituAppServer{

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new MeituAppServerHandler());

        server.start();
        server.join();
    }

}
