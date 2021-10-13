package cn.larson.groupcontrol.main.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.LocalSocketAddress;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.larson.remotedisplay.RemoteDisplayManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.SocketConfiguration;
import larson.groupcontrol.app.connection.ConnectOption;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.intf.IStateChangeListener;
import larson.groupcontrol.app.listener.IMessageListener;
import cn.larson.groupcontrol.main.BackgroundServiceManager;
import cn.larson.groupcontrol.R;
import cn.larson.groupcontrol.main.ServerSdk;
import cn.larson.groupcontrol.main.listener.MiniTouchMsgListener;
import cn.larson.groupcontrol.main.msgbody.ExampleTextMessageBody;
import cn.larson.groupcontrol.main.msgbody.MiniTouchMsgBody;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.DeviceInfoUtils;
import larson.groupcontrol.app.util.LogUtils;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * @author Rony
 * @date 2018/7/27
 */
public class ClientActivity extends AppCompatActivity implements View.OnClickListener, IMessageListener,
        IStateChangeListener, BackgroundServiceManager.GroupControlServerListener, View.OnLongClickListener {
    private Button btEnable;
    private EditText edSend;
    private EditText etLocalName;
    private EditText edServerIp;
    private EditText etServerPort;
    private ArrayAdapter<String> arrayAdapter;

    private boolean isLogged;
    private int pid;
    private Context mContext;
    private ServerSdk skClient;
    private boolean isBinderControlServer;
    private List<String> adapterData = new ArrayList<>();
    private BackgroundServiceManager mBackgroundServiceManager;

    /**
     * 录制屏幕需要的参数
     */
    static MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private static final int REQUEST_MEDIA_PROJECTION = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mContext = getApplicationContext();
        initView();
        initData();
        startNativeService();
        startScreenCapture();
    }

    private void initView() {
        btEnable = findViewById(R.id.btEnable);
        Button btSend = findViewById(R.id.btSend);
        edSend = findViewById(R.id.edSend);
        Button btSendSync = findViewById(R.id.btSendSync);

        btEnable.setOnClickListener(this);
        btSendSync.setOnClickListener(this);
        btSend.setOnClickListener(this);
        etLocalName = findViewById(R.id.etLocalName);
        edServerIp = findViewById(R.id.edServerIp);
        etServerPort = findViewById(R.id.etServerPort);

        ListView listView = findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<>(ClientActivity.this,
                android.R.layout.simple_list_item_1, adapterData);
        listView.setAdapter(arrayAdapter);

        btSend.setOnLongClickListener(this);
        btSendSync.setOnLongClickListener(this);
    }


    /**
     * 初始化相关的变量及服务
     */
    private void initData() {
        try {
            isBinderControlServer = false;
            mBackgroundServiceManager = new BackgroundServiceManager(this, this);
            skClient = ServerSdk.getInstance();
            skClient.setup(getConfiguration());
            skClient.addConnectListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            showToast("初始化失败");
        }
    }

    /**
     * Configure Socket connection Sdk, including connection options, message listening, here is static monitoring,
     * Dynamic sniffing is added by {@link ServerSdk#addMessageListener(IMessageListener, MessageFilter)}
     * <p>
     * 配置Socket连接Sdk,包括连接选项，消息监听,这里是静态监听，
     * 动态监听通过 {@link ServerSdk#addMessageListener(IMessageListener, MessageFilter)}添加
     *
     * @return 连接需要的配置
     * @throws Exception 在获取clientID的时候可能会抛出异常
     */
    private SocketConfiguration getConfiguration() throws Exception {
        String clientID = DeviceInfoUtils.getDeviceInfo(mContext);
        LogUtils.v("clientID: " + clientID);
        ConnectOption option = new ConnectOption.Builder(clientID)
                .setSocketTimeout(3000)
                .setReconnectAllowed(true)
                .setReconnectMaxAttemptTimes(4)
                .setReconnectInterval(10 * 1000)
                .setPulseFrequency(1000)
                .setDebug(true)
                .build();
        Point point = getScreenSize(mContext);
        return new SocketConfiguration.Builder(false, "GroupControl")
                .setSkSocketOption(option)
                .setNameSpace(LocalSocketAddress.Namespace.ABSTRACT)
                .addMessageListener(new MessageIdFilter(Constants.MSG_MINICAP_CONTROL), this)
                .addMessageListener(new MessageIdFilter(MiniTouchMsgBody.ID), new MiniTouchMsgListener(point.x, point.y))
                .build();
    }

    private Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            throw new RuntimeException("Failed to initialize WindowManager, WindowManager is null");
        }
        Display mDisplay = wm.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        mDisplay.getMetrics(dm);
        Point point = new Point();
        point.x = dm.widthPixels;
        point.y = dm.heightPixels;
        return point;
    }


    /**
     * Enable the socket service of the Native layer. The socket of the Native
     * layer exists independently of the APP process. When the App exits,
     * if there is no active kill, it will always exist in the JVM.
     * <p>
     * 开启Native层的socket服务,Native层的socket独立于APP进程存在，
     * App退出时，如果没有主动杀死则会一直存在JVM中
     */
    private void startNativeService() {
        if (mBackgroundServiceManager != null) {
            boolean isBind = mBackgroundServiceManager.bindService();
            LogUtils.d("bind service :" + isBind);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mMediaProjectionManager == null) {
                LogUtils.e("mMediaProjectionManager=null,startScreenCapture failed !!");
                return;
            }
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        } else {
            LogUtils.e("unsupported android device ,startScreenCapture failed !!! ");
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                // Even if the user cancels the authorization, we can still request permission here because we have to use it
                Toast.makeText(getApplicationContext(), "User cancelled the access", Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            } else {
                LogUtils.e("unsupported android sdk level ,request media projection failed !!! ");
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btEnable:
                if (!skClient.isConnected()) {
                    connect();
                } else {
                    disconnect();
                }
                break;
            case R.id.btSend:
                prepareSendMessage(edSend.getText().toString(), false);
                break;
            case R.id.btSendSync:
                prepareSendSyncMessage(edSend.getText().toString(), false);
                break;
            default:
                break;
        }
    }

    private void connect() {
        if (!isBinderControlServer) {
            showToast("Service is not Binder, Please wait and press again!");
            startNativeService();
            return;
        }
        startLocal();
        skClient.connect(false);
        setLogged(1);
    }

    private int startLocal() {
        String localName = etLocalName.getText().toString().trim();
        String serverPort = etServerPort.getText().toString().trim();
        String serverIp = edServerIp.getText().toString().trim();
        int port = Integer.parseInt(serverPort);

        if (TextUtils.isEmpty(localName)) {
            localName = "GroupControl";
        }
        if (TextUtils.isEmpty(serverPort)) {
            port = 12345;
        }
        if (TextUtils.isEmpty(serverIp)) {
            serverIp = "192.168.43.1";
        }

        pid = mBackgroundServiceManager.startClient(localName, serverIp, port);
        return pid;
    }


    private void disconnect() {
        skClient.disConnect();
        setLogged(3);
        adapterData.clear();
        arrayAdapter.notifyDataSetChanged();
    }

    private void prepareSendMessage(String msg, boolean isPressureTest) {
        //发送检测
        if (!skClient.isConnected()) {
            showToast("未连接,请先连接");
        }
        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }

        //发送消息
        int maxTimes = isPressureTest ? 500 : 1;
        for (int i = 0; i < maxTimes; i++) {
            sendMessage(msg);
            //发送完成后清空输入框
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        edSend.setText("");
    }

    /**
     * Send an asynchronous message and send it directly
     * 发送非同步消息，直接发送
     *
     * @param msg 需要发送的消息
     */
    private void sendMessage(String msg) {
        //发送消息
        try {
            ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder().text(msg).build();
            Message message = new Message.Builder(ExampleTextMessageBody.ID)
                    .setAck(false)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(messageBody.getBodyBytes())
                    .build();
            skClient.sendMessage(message);
            //发送完成后清空输入框
            edSend.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送的这条消息需要服务端反馈相同msgID且SN相同
     *
     * @param content content
     */
    private void prepareSendSyncMessage(String content, boolean isPressureTest) {
        //发送检测
        if (!skClient.isConnected()) {
            showToast("未连接,请先连接");
        }
        if (TextUtils.isEmpty(content.trim())) {
            return;
        }

        new SendSyncTask(this, content, isPressureTest).execute();
    }

    private void sendSyncMessage(String content) {
        try {
            ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder().text(content).build();
            Message message = new Message.Builder(ExampleTextMessageBody.ID_SYNC)
                    .setAck(false)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(messageBody.getBodyBytes())
                    .build();
            Message msg = skClient.sendSyncMessage(message, 5000);
            if (msg != null) {
                String replyStr = new String(msg.getBody());
                refreshReceive(String.format("%s[reply]:", replyStr));
            } else {
                LogUtils.w("read reply msg timeout");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void refreshReceive(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapterData.add(string);
                arrayAdapter.notifyDataSetChanged();
            }
        });
    }


    @Override
    public void processMessage(Message msg) {
        if (Constants.MSG_MINICAP_CONTROL == msg.getMsgId()) {
            byte reqCode = msg.getBody()[0];
            if (Constants.CODE_REQUEST_MINICAP_OPEN == reqCode) {
                if (mMediaProjection != null) {
                    RemoteDisplayManager.startServer(ClientActivity.this, mMediaProjection);
                    skClient.sendMiniCapControlReply(Constants.STATE_MINICAP_STARTED, msg.getVid(), msg.getPackets()[0].getSn());
                } else {
                    skClient.sendMiniCapControlReply(Constants.STATE_MINICAP_FAILED, msg.getVid(), msg.getPackets()[0].getSn());
                }
            } else if (Constants.CODE_REQUEST_MINICAP_CLOSE == reqCode) {
                RemoteDisplayManager.stopServer(ClientActivity.this);
                skClient.sendMiniCapControlReply(Constants.STATE_MINICAP_STOPED, msg.getVid(), msg.getPackets()[0].getSn());
            } else {
                LogUtils.e("illegal option when exec Msg_MiniCap_Control!!!");
            }
            return;
        }

        //处理从服务器发过来的消息
        if (ExampleTextMessageBody.ID == msg.getMsgId()) {
            String text = new String(msg.getBody());
            refreshReceive(String.format("[%s]%s", msg.getPackets()[0].getSn(), text));
        } else if (ExampleTextMessageBody.ID_SYNC == msg.getMsgId()) {
            //response 消息不需要回复
            try {
                String text = new String(msg.getBody());
                refreshReceive(text);
                //在收到同步类消息的时候，需要设置sn为原来的sn
                ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder()
                        .text(String.format("[收到测试机回复]:%s", text)).build();
                Message message = new Message.Builder(ExampleTextMessageBody.ID_SYNC, msg.getPackets()[0].getSn())
                        .setAck(false)
                        .setType(Message.MSG_TYPE_CONTROL)
                        .setBody(messageBody.getBodyBytes())
                        .setVid(msg.getVid())
                        .build();
                skClient.sendMessage(message);
                LogUtils.e("[sendSyncMessage]:" + message.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void showToast(final String text) {
        Toast.makeText(mContext, text, LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (skClient.isConnected()) {
            skClient.disConnect();
            btEnable.setEnabled(true);
            showToast("stop server");
        }
        skClient.removeConnectListener(this);
        if (mBackgroundServiceManager != null) {
            mBackgroundServiceManager.stopClient(0);
        }
        skClient.onDestroy();
        super.onBackPressed();
    }


    @Override
    public void onConnectStateChange(final int stateCode, final Exception e) {
        LogUtils.e("onConnect state change :" + stateCode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (stateCode) {
                    case IStateChangeListener.STATE_CONNECT_SUCCESSFUL:
                        setLogged(2);
                        skClient.addMessageListener(ClientActivity.this, new MessageIdFilter(ExampleTextMessageBody.ID));
                        skClient.addMessageListener(ClientActivity.this, new MessageIdFilter((ExampleTextMessageBody.ID_SYNC)));
                        showToast("connect remote success ");
                        break;
                    case STATE_REACH_MAX_RECONNECT_TIME:
                        mBackgroundServiceManager.stopClient(pid);
                        setLogged(4);
                        showToast("达到最大重连次数,需要在这里重启服务");
                        break;
                    case STATE_SOCKET_CLOSE_SUCCESSFUL:
                    case STATE_CONNECT_REMOTE_FAILED:
                    case STATE_SOCKET_CLOSE_FAILED:
                    case STATE_LOGIN_TIMEOUT:
                        setLogged(4);
                        adapterData.clear();
                        int retCode = mBackgroundServiceManager.stopClient(pid);
                        LogUtils.e(stateCode + "<<stopClient:retCode=" + retCode);
                        break;
                    case STATE_CONNECT_FAILED:
                    case STATE_LOGIN_REFUSED:
                    case STATE_REMOTE_SOCKET_CLOSED:
                        if (!isLogged) {
                            mBackgroundServiceManager.stopClient(pid);
                            showToast("state code:" + stateCode + "  msg=" + (e != null ? e.toString() : ""));
                            setLogged(4);
                        } else {
                            LogUtils.w("not connected !!!");
                        }
                        break;
                    default:
                        showToast(e != null ? e.toString() : "unrecognized connect state ,stateCode:" + stateCode);
                }
            }
        });
    }

    private void setLogged(int connectState) {
        switch (connectState) {
            case 1:
                //正在登录
                btEnable.setText(R.string.text_connecting);
                btEnable.setEnabled(false);
                break;
            case 2:
                isLogged = true;
                btEnable.setText(getString(R.string.stop));
                btEnable.setEnabled(true);
                break;
            case 3:
                btEnable.setText(R.string.text_disconnecting);
                btEnable.setEnabled(false);
                break;
            case 4:
                isLogged = false;
                btEnable.setText(getString(R.string.start));
                btEnable.setEnabled(true);
                break;
            default:
        }
    }

    @Override
    public void onBinderState(int state) {
        isBinderControlServer = state == BackgroundServiceManager.BINDER_SUCCEED;
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.btSend) {
            try {
                String msg = edSend.getText().toString().trim();
                prepareSendMessage(msg, true);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (v.getId() == R.id.btSendSync) {
            prepareSendSyncMessage(edSend.getText().toString().toLowerCase(), true);
            return true;
        }
        return false;
    }

    /**
     * 由于发送同步消息会阻塞线程，所以把同步类消息放在子线程发送
     */
    private static class SendSyncTask extends AsyncTask<String, Void, Boolean> {
        private String content;
        private boolean isPressureTest;
        private WeakReference<ClientActivity> activityWeakReference;

        /**
         * only retain a weak reference to the activity
         */
        SendSyncTask(ClientActivity activity, String content, boolean isPressureTest) {
            this.content = content;
            this.isPressureTest = isPressureTest;
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected Boolean doInBackground(String... str) {
            //发送消息
            int maxTimes = isPressureTest ? 200 : 1;
            for (int i = 0; i < maxTimes; i++) {
                activityWeakReference.get().sendSyncMessage(content);
                //发送完成后清空输入框
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            activityWeakReference.get().edSend.setText("");
        }
    }

}
