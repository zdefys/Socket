package com.example.administrator.socket_01;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private TextView tv = null;
    private EditText et = null;
    private TextView IPtv = null;
    private Button btnSend = null;
    private Button btnAcept = null;
    private Socket socket;
    private ServerSocket mServerSocket = null;
    private boolean running = false;
    private AcceptThread mAcceptThread;
    private ReceiveThread mReceiveThread;
    private Handler mHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.tv);
        et = (EditText) findViewById(R.id.etSend);
        IPtv = (TextView) findViewById(R.id.tvIP);
        btnAcept = (Button) findViewById(R.id.btnAccept);
        btnSend = (Button) findViewById(R.id.btnSend);
        mHandler = new MyHandler();
        btnSend.setEnabled(false);//设置发送按键为不可见
        btnAcept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开始监听线程，监听客户端连接
                mAcceptThread = new AcceptThread();
                running = true;
                mAcceptThread.start();
                btnSend.setEnabled(true);//设置发送按键为可见
                IPtv.setText("等待连接");
                btnAcept.setEnabled(false);

            }
        });
        //发送数据按钮
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OutputStream os = null;
                try {
                    os = socket.getOutputStream();//获得socket的输出流
                    String msg = et.getText().toString()+"\n";
                    //                    System.out.println(msg);
                    os.write(msg.getBytes("utf-8"));//输出EditText的内容
                    et.setText("");//发送后输入框清0
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (NullPointerException e) {
                    displayToast("未连接不能输出");//防止服务器端关闭导致客户端读到空指针而导致程序崩溃
                }
            }
        });
    }
    //定义监听客户端连接的线程
    private class AcceptThread extends Thread{
        @Override
        public void run() {
            //            while (running) {
            try {
                mServerSocket = new ServerSocket(40012);//建立一个ServerSocket服务器端
                socket = mServerSocket.accept();//阻塞直到有socket客户端连接
                //                System.out.println("连接成功");
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Message msg = mHandler.obtainMessage();
                msg.what = 0;
                msg.obj = socket.getInetAddress().getHostAddress();//获取客户端IP地址
                mHandler.sendMessage(msg);//返回连接成功的信息
                //开启mReceiveThread线程接收数据
                mReceiveThread = new ReceiveThread(socket);
                mReceiveThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //            }
        }
    }

    //定义接收数据的线程
    private class ReceiveThread extends Thread{
        private InputStream is = null;
        private String read;
        //建立构造函数来获取socket对象的输入流
        public ReceiveThread(Socket sk){
            try {
                is = sk.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            while (running) {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    //读服务器端发来的数据，阻塞直到收到结束符\n或\r
                    read = br.readLine();
                    System.out.println(read);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    running = false;//防止服务器端关闭导致客户端读到空指针而导致程序崩溃
                    Message msg2 = mHandler.obtainMessage();
                    msg2.what = 2;
                    mHandler.sendMessage(msg2);//发送信息通知用户客户端已关闭
                    e.printStackTrace();
                    break;
                }
                //用Handler把读取到的信息发到主线程
                Message msg = mHandler.obtainMessage();
                msg.what = 1;
                msg.obj = read;
                mHandler.sendMessage(msg);

            }
        }
    }

    private void displayToast(String s)
    {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    class MyHandler extends Handler{//在主线程处理Handler传回来的message
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    String str = (String) msg.obj;
                    tv.setText(str);
                    break;
                case 0:
                    IPtv.setText("客户端"+msg.obj+"已连接");
                    displayToast("连接成功");
                    break;
                case 2:
                    displayToast("客户端已断开");
                    //清空TextView
                    tv.setText(null);//
                    IPtv.setText(null);
                    try {
                        socket.close();
                        mServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    btnAcept.setEnabled(true);
                    btnSend.setEnabled(false);
                    break;
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);//清空消息队列，防止Handler强引用导致内存泄漏
    }
}
