package com.example.demo.thread;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class SimpleHttpServer {
    //处理HttpRequest的线程池
    static DefaultThreadPool<HttpRequestHandler> threadPool = new DefaultThreadPool<HttpRequestHandler>(1);
    //SimpleHttpServer的跟路径
    static String basePath = "C:\\Users\\huni\\Desktop";
    static ServerSocket serverSocket;
    //服务监听端口
    static int port = 8080;
    public static void setPort ( int port) {
        if (port > 0) {
            SimpleHttpServer.port = port;
        }
    }
    public static void setBasePath (String basePath) {
        if (basePath != null && new File(basePath).exists() && new File(basePath).isDirectory()){
            SimpleHttpServer.basePath = basePath;
        }
    }
    //启动SimpleHttpServer
    public static void start() throws Exception {
        serverSocket = new ServerSocket(port);
        Socket socket = null;
        while ((socket = serverSocket.accept()) != null){
             //接受一个客户端Socket，生成一个HttpRequestHandler，放入线程池执行
            System.out.println("execute");
            threadPool.execute(new HttpRequestHandler(socket));
        }
        serverSocket.close();
    }
    static class HttpRequestHandler implements Runnable {
        private Socket socket;
        public HttpRequestHandler(Socket socket){
            this.socket = socket;
        }
        @Override
        public void run() {
            String line = null;
            BufferedReader br = null;
            BufferedReader reader = null;
            PrintWriter out = null;
            InputStream in = null;
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String header = reader.readLine();
                //由相对路径计算出决定路径
                String filePath = basePath + header.split(" ")[1];
                System.out.println(filePath);
                out = new PrintWriter(socket.getOutputStream());
                //如果请求资源的后缀为ipg或者ico，则读取资源并输出
                if (filePath.endsWith(".png") || filePath.endsWith("ico")) {
                    in = new FileInputStream(filePath);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] array = new byte[1000];
                    int n = 0;
                    int c = 0;
                    while ((n = in.read(array)) != -1){
                        //System.out.println("read " + n + " bytes."+ c++);
                        baos.write(array,0,n);
                        //System.out.println("write " + n + " bytes.");
                    }
                    byte[] arr = baos.toByteArray();

                    //System.out.println(array.length);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: image/jpeg");
                    out.println("Content-Length: " + arr.length);
                    out.println("");
                    out.flush();
                    System.out.println("this picture has" +arr.length +" bytes");
                    socket.getOutputStream().write(arr,0,arr.length);
                } else {
                    br =new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
                    out = new PrintWriter(socket.getOutputStream());
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Molly");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println("");
                    while ((line = br.readLine()) != null){
                        out.println(line);
                    }
                    out.flush();
                }
                //out.flush();
            } catch (IOException e) {
                out.println("HTTP/1.1 500");
                out.println("");
                out.flush();
            }finally{
               /* try {
                    in.close();
                    reader.close();
                    out.close();
                    socket.close();
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                close(br, in, reader, out, socket);
            }
        }
    }
    private static void close(Closeable ... closeables) {
        if (closeables != null) {
            for(Closeable closeable : closeables){
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (IOException e) {
                   // e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            SimpleHttpServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
