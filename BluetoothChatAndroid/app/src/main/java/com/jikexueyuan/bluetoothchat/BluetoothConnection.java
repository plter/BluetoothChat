package com.jikexueyuan.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by plter on 11/30/15.
 */
public class BluetoothConnection {


    private static final String NAME = "BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("aae8841b-5ecd-4650-aed7-4714042d268a");
    private final Activity context;
    private AcceptThread acceptThread = null;
    private ManageConnectionThread manageConnectionThread = null;
    private OnReadNewLineListener onReadNewLineListener = null;


    public BluetoothConnection(Activity context) {
        this.context = context;
    }

    public void startServerSocket() {
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public void stopServerSocket() {
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
    }

    public void manageConnection(BluetoothSocket socket) {
        manageConnectionThread = new ManageConnectionThread(socket);
        manageConnectionThread.start();
    }


    public OnReadNewLineListener getOnReadNewLineListener() {
        return onReadNewLineListener;
    }

    public void setOnReadNewLineListener(OnReadNewLineListener onReadNewLineListener) {
        this.onReadNewLineListener = onReadNewLineListener;
    }

    public void sendLine(String line) {
        if (manageConnectionThread != null) {
            manageConnectionThread.sendLine(line);
        }
    }

    public void connect(BluetoothDevice device) {
        stopServerSocket();

        new ConnectThread(device).start();
    }


    class ConnectThread extends Thread {

        private final BluetoothDevice device;
        private BluetoothSocket socket = null;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if (socket == null) {
                return;
            }


            try {
                socket.connect();
                manageConnection(socket);

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "成功连接设备", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();

                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "无法创建连接", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


    class ManageConnectionThread extends Thread {


        private BluetoothSocket socket;
        private InputStream in;
        private OutputStream out;

        public ManageConnectionThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();

            try {
                out = socket.getOutputStream();
                in = socket.getInputStream();

                BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line = null;

                while ((line = br.readLine()) != null) {
                    if (getOnReadNewLineListener() != null) {
                        getOnReadNewLineListener().onRead(line, socket.getRemoteDevice());
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            cancel();

            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "已经断开连接", Toast.LENGTH_SHORT).show();
                }
            });
        }

        private void cancel() {
            try {
                socket.close();
                manageConnectionThread = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendLine(String line) {
            line += "\n";
            try {
                out.write(line.getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class AcceptThread extends Thread {

        private BluetoothAdapter bluetoothAdapter;
        private BluetoothServerSocket serverSocket = null;
        private boolean listenning = true;

        public AcceptThread() {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            try {
                listenning = true;
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(BluetoothConnection.NAME, BluetoothConnection.MY_UUID);

                System.out.println("Success to listen");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();

            if (serverSocket == null) {
                return;
            }

            BluetoothSocket socket = null;
            while (listenning) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();

                    if (listenning) {
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(context, "无法接受连接", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                if (socket != null) {
                    cancel();
                    manageConnection(socket);
                }
            }
        }


        public void cancel() {
            if (serverSocket == null) {
                return;
            }

            listenning = false;

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    interface OnReadNewLineListener {
        void onRead(String line, BluetoothDevice remoteDevice);
    }
}
