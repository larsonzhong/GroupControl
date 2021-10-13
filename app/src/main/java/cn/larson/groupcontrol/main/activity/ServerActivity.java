package cn.larson.groupcontrol.main.activity;

import android.content.Context;
import android.content.Intent;
import android.net.LocalSocketAddress;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import larson.groupcontrol.app.SocketConfiguration;
import larson.groupcontrol.app.connection.ConnectOption;
import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.intf.IStateChangeListener;
import larson.groupcontrol.app.listener.IMessageListener;
import cn.larson.groupcontrol.main.BackgroundServiceManager;
import cn.larson.groupcontrol.main.IClientStateCallback;
import cn.larson.groupcontrol.R;
import cn.larson.groupcontrol.main.ServerSdk;
import cn.larson.groupcontrol.main.bean.ClientInfo;
import cn.larson.groupcontrol.main.msgbody.ExampleTextMessageBody;
import cn.larson.groupcontrol.main.remote.RemoteClientActivity;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.DeviceInfoUtils;
import larson.groupcontrol.app.util.LogUtils;

import static android.widget.Toast.LENGTH_SHORT;
import static cn.larson.groupcontrol.main.activity.MainActivity.MAX_PRESSURE_TEST_TIMES;

/**
 * @author Rony
 * @date 2018/7/27
 */

public class ServerActivity extends AppCompatActivity implements View.OnClickListener, IClientStateCallback, IMessageListener, IStateChangeListener, BackgroundServiceManager.GroupControlServerListener, View.OnLongClickListener {
    private static final byte MSG_SHOW_TOAST = 1;
    private static final byte MSG_REFRESH_RECIVE = 2;
    private static final byte MSG_UPDATE_CLIENT = 3;


    private Context mContext;
    private ServerSdk skClient;
    private boolean isBinderControlServer;
    private BackgroundServiceManager mBackgroundServiceManager;

    private Map<Integer, ClientInfo> clientInfoMap = new LinkedHashMap<>();
    private List<ClientInfo> clientInfoList = new ArrayList<>();
    private ClientAdapter clientAdapter;
    private int currentCheckIndex = -1;

    private EditText etServerPort;
    private EditText etLocalName;
    private TextView tvResult;
    private EditText edSend;
    private Button btEnable;
    private Handler handler;

    private static class MyHandler extends Handler {
        private WeakReference<ServerActivity> activityRef;

        MyHandler(ServerActivity activityRef) {
            this.activityRef = new WeakReference<>(activityRef);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_REFRESH_RECIVE:
                    String content = (String) msg.obj;
                    activityRef.get().tvResult.setText(content);
                    break;
                case MSG_SHOW_TOAST:
                    //show toast
                    String text = (String) msg.obj;
                    Toast.makeText(activityRef.get().mContext, text, LENGTH_SHORT).show();
                    break;
                case MSG_UPDATE_CLIENT:
                    //client state change
                    ClientInfo info = (ClientInfo) msg.getData().getSerializable("info");
                    if (info == null) {
                        return;
                    }
                    activityRef.get().clientInfoList.clear();
                    activityRef.get().clientInfoList.addAll(new ArrayList<>(activityRef.get().clientInfoMap.values()));
                    activityRef.get().clientAdapter.notifyDataSetChanged();
                    activityRef.get().showToast(String.format("[%s]%s", info.getStateString(), info.toString()));
                    break;
                default:
            }
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        mContext = getApplicationContext();
        handler = new MyHandler(ServerActivity.this);
        initView();
        initData();
        startNativeService();
    }

    private void initData() {
        try {
            isBinderControlServer = false;
            mBackgroundServiceManager = new BackgroundServiceManager(this, this);
            skClient = ServerSdk.getInstance();
            skClient.setup(getConfiguration());
            skClient.addConnectListener(this);
            skClient.addClientStateCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e("初始化失败");
        }
    }

    private void initView() {
        btEnable = findViewById(R.id.btEnable);
        Button btSend = findViewById(R.id.btSend);
        edSend = findViewById(R.id.edSend);
        tvResult = findViewById(R.id.tvResult);
        Button btMutiSend = findViewById(R.id.btMutiSend);

        btEnable.setOnClickListener(this);
        btSend.setOnClickListener(this);
        btMutiSend.setOnClickListener(this);
        etLocalName = findViewById(R.id.etLocalName);
        etServerPort = findViewById(R.id.etServerPort);

        ListView listView = findViewById(R.id.listview);
        clientAdapter = new ClientAdapter(mContext, 1, clientInfoList);
        listView.setAdapter(clientAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentCheckIndex = position;
                clientAdapter.notifyDataSetChanged();
            }
        });

        btSend.setOnLongClickListener(this);
        btMutiSend.setOnLongClickListener(this);
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
            mBackgroundServiceManager.bindService();
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
        //todo 以下的各项配置，特别是SocketName以及重连，建议通过SP来配置
        String clientID = DeviceInfoUtils.getDeviceInfo(mContext);
        LogUtils.d("clientID: " + clientID);
        ConnectOption option = new ConnectOption.Builder(clientID)
                .setSocketTimeout(3000)
                .setReconnectAllowed(true)
                .setReconnectMaxAttemptTimes(4)
                .setReconnectInterval(10 * 1000)
                .setPulseFrequency(1000)
                .setDebug(true)
                .build();
        return new SocketConfiguration.Builder(true, "GroupControl")
                .setSkSocketOption(option)
                .setNameSpace(LocalSocketAddress.Namespace.ABSTRACT)
                .build();
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
                prepareSendMessage(edSend.getText().toString().trim(), false, false);
                break;
            case R.id.btMutiSend:
                prepareSendMessage(edSend.getText().toString().trim(), true, false);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.btSend) {
            String msg = edSend.getText().toString().trim();
            prepareSendMessage(msg, false, true);
            return true;
        }

        if (v.getId() == R.id.btMutiSend) {
            String msg = edSend.getText().toString().trim();
            prepareSendMessage(msg, true, true);
            LogUtils.e("群发完毕");
            return true;
        }

        return false;
    }

    /**
     * Connect to native socket
     */
    private void connect() {
        if (!isBinderControlServer) {
            showToast("Service is not Binder, Please wait and press again!");
            startNativeService();
            return;
        }

        startLocalServer();
        skClient.connect(false);
        btEnable.setEnabled(false);
    }

    private boolean startLocalServer() {
        String localName = etLocalName.getText().toString().trim();
        String serverPort = etServerPort.getText().toString().trim();
        if (TextUtils.isEmpty(localName)) {
            localName = "GroupControl";
        }
        if (TextUtils.isEmpty(serverPort)) {
            serverPort = "12345";
        }
        int port = Integer.parseInt(serverPort);
        if (mBackgroundServiceManager.startServer(localName, port) > 0) {
            return true;
        }
        showToast("start background server failed !!!");
        return false;
    }

    private void disconnect() {
        skClient.disConnect();
        btEnable.setText(R.string.text_disconnecting);
        btEnable.setEnabled(true);

        clientInfoMap.clear();
        clientInfoList.clear();
        clientAdapter.notifyDataSetChanged();
    }

    /**
     * Send an asynchronous message and send it directly
     * 发送非同步消息，直接发送
     *
     * @param msg 需要发送的消息
     */
    private void prepareSendMessage(String msg, boolean isBatchSend, boolean isPressure) {
        if (!skClient.isConnected()) {
            showToast("未连接,请先连接");
            return;
        }
        if (TextUtils.isEmpty(msg.trim())) {
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            int times = isPressure ? MAX_PRESSURE_TEST_TIMES : 1;
            for (int i = 0; i < times; i++) {
                if (isBatchSend) {
                    for (ClientInfo client : clientInfoList) {
                        sendMessage(msg, client);
                    }
                    showToast("send to all");
                } else if (currentCheckIndex == -1) {
                    showToast("请选择客户端");
                    return;
                } else {
                    ClientInfo clientInfo = clientInfoList.get(currentCheckIndex);
                    sendMessage(msg, clientInfo);
                    showToast("send to vid:" + clientInfo.getVid());
                }
            }
            long endTime = System.currentTimeMillis();
            edSend.setText(String.format(Locale.CHINA, "总耗时：%d，消息长度：%d，发送次数：%d", endTime - startTime, msg.length(), times));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a message to the specified client
     *
     * @param msg    message to send
     * @param client target client
     * @throws Exception exception when build message
     */
    private void sendMessage(String msg, ClientInfo client) throws Exception {
        short vid = client.getVid();
        ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder().text(msg).build();
        Message message = new Message.Builder(ExampleTextMessageBody.ID)
                .setAck(true)
                .setType(Message.MSG_TYPE_CONTROL)
                .setBody(messageBody.getBodyBytes())
                .setVid(vid)
                .build();
        skClient.sendMessage(message);
    }

    /**
     * Display the text type message received on the interface
     *
     * @param vid  ClientID
     * @param text msg content
     */
    private void refreshReceive(final short sn, final short vid, final String text) {
        LogUtils.e(String.format("[%s]%s:%s", sn + "", vid, text));
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_REFRESH_RECIVE;
        msg.obj = String.format("[%s]%s:%s", sn + "", vid, text);
        handler.sendMessage(msg);
    }

    private void showToast(final String text) {
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_SHOW_TOAST;
        msg.obj = text;
        handler.sendMessage(msg);
    }

    @Override
    public void processMessage(Message msg) {
        //处理从服务器发过来的消息
        if (ExampleTextMessageBody.ID == msg.getMsgId()) {
            String text = new String(msg.getBody());
            refreshReceive(msg.getPackets()[0].getSn(), msg.getVid(), text);
        } else if (ExampleTextMessageBody.ID_SYNC == msg.getMsgId()) {
            //response 消息不需要回复
            try {
                String text = new String(msg.getBody());
                //在收到同步类消息的时候，需要设置sn为原来的sn
                ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder()
                        .text(String.format("%s[收到控制机回复]:%s", msg.getPackets()[0].getSn() + "", text)).build();
                Message message = new Message.Builder(msg.getMsgId(), msg.getPackets()[0].getSn())
                        .setType(Message.MSG_TYPE_CONTROL)
                        .setBody(messageBody.getBodyBytes())
                        .setVid(msg.getVid())
                        .build();
                skClient.sendMessage(message);
                refreshReceive(msg.getPackets()[0].getSn(), msg.getVid(), text);
                LogUtils.e("[sendSyncMessage]:" + message.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (skClient.isConnected()) {
            skClient.disConnect();
            btEnable.setEnabled(true);
            showToast("stop server");
        }
        skClient.removeClientStateCallback(this);
        skClient.removeConnectListener(this);
        if (mBackgroundServiceManager != null) {
            mBackgroundServiceManager.stopServer(0);
            mBackgroundServiceManager.unbind();
        }
        skClient.onDestroy();
        super.onBackPressed();
    }

    @Override
    public void onConnectStateChange(final int stateCode, final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String msg;
                switch (stateCode) {
                    case STATE_CONNECT_SUCCESSFUL:
                        msg = "connect success";
                        btEnable.setText(getString(R.string.stop));
                        btEnable.setEnabled(true);
                        showToast(msg);
                        skClient.addMessageListener(ServerActivity.this, new MessageIdFilter((ExampleTextMessageBody.ID)));
                        skClient.addMessageListener(ServerActivity.this, new MessageIdFilter((ExampleTextMessageBody.ID_SYNC)));
                        break;
                    case STATE_REACH_MAX_RECONNECT_TIME:
                        showToast("达到最大重连次数,重启服务" + e.getMessage());
                        break;
                    case STATE_SOCKET_CLOSE_FAILED:
                        showToast("断开连接");
                        btEnable.setText(getString(R.string.start));
                        btEnable.setEnabled(true);
                        break;
                    case STATE_CONNECT_FAILED:
                    case STATE_SOCKET_CLOSE_SUCCESSFUL:
                    case STATE_LOGIN_TIMEOUT:
                    case STATE_LOGIN_REFUSED:
                        btEnable.setText(getString(R.string.start));
                        btEnable.setEnabled(true);
                        if(stateCode == STATE_SOCKET_CLOSE_SUCCESSFUL) {
                            showToast("disconnect success");
                        } else {
                            showToast("state code:" + stateCode + "  msg=" + (e != null ? e.toString() : ""));
                        }
                        break;
                    default:
                        showToast("unrecognized connect state ,stateCode:" + stateCode);
                }
            }
        });
    }

    @Override
    public void onClientStateChange(final ClientInfo info) {
        LogUtils.e(info.toString());
        //只要不是客户端登出，那么只需要更新列表即可
        if (info.getConnectState() == Constants.STATE_CLIENT_DISCONNECTED) {
            clientInfoMap.remove(info.getPid());
        } else if (info.getConnectState() == Constants.STATE_CLIENT_CONNECTED) {
            clientInfoMap.put(info.getPid(), info);
        } else {
            //更新列表
            clientInfoMap.put(info.getPid(), info);
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable("info", info);
        android.os.Message msg = new android.os.Message();
        msg.what = MSG_UPDATE_CLIENT;
        msg.setData(bundle);
        handler.sendMessage(msg);
    }


    @Override
    public void onBinderState(int state) {
        isBinderControlServer = state == BackgroundServiceManager.BINDER_SUCCEED;
    }


    class ClientAdapter extends ArrayAdapter<ClientInfo> {

        private LayoutInflater inflater;

        ClientAdapter(Context context, int res, List<ClientInfo> list) {
            super(context, res, list);
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_client_list, parent, false);
            }
            final TextView name = convertView.findViewById(R.id.name);
            final TextView state = convertView.findViewById(R.id.tvState);
            final CheckBox checkBox = convertView.findViewById(R.id.checkbox1);

            if (position < getCount()) {
                ClientInfo info = getItem(position);
                if (info != null) {
                    name.setText(info.getIp());
                    state.setText(info.getStateString());
                }
            } else {
                //压力测试下，容易发生下标越界，请注意
                LogUtils.e("Illegal access with 0 size list !!!");
            }


            checkBox.setChecked(position == currentCheckIndex);
            return convertView;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String content = edSend.getText().toString();
        int id = item.getItemId();

        if (!skClient.isConnected()) {
            showToast("未连接,请先连接");
            return false;
        }

        if (R.id.action_find_device != id && R.id.action_control_device != id &&
                R.id.action_fetch_clients != id) {
            //针对发送消息做清空判断
            if (TextUtils.isEmpty(content.trim())) {
                return false;
            } else {
                LogUtils.e("非法操作，请检查代码逻辑！！");
            }
        }

        if (currentCheckIndex == -1) {
            //在这里，除了多选外都需要选择客户端
            if (R.id.action_send_sync_muti != id && R.id.action_send_sync_muti_pressure != id
                    && R.id.action_fetch_clients != id) {
                showToast("请选择客户端");
                return false;
            } else {
                LogUtils.e("非法操作，请检查代码逻辑！！");
            }
        }
        LogUtils.e("current index = " + currentCheckIndex + ",clientInfoMap.size=" + clientInfoList.size());

        switch (id) {
            case R.id.action_send_sync_muti:
                for (ClientInfo client : clientInfoList) {
                    new SendSyncTask(ServerActivity.this, content, client, false).execute();
                }
                showToast("send sync to all");
                break;
            case R.id.action_send_sync_muti_pressure:
                //请注意这里的代码在实际开发中应该是每个测试机执行一轮，执行200轮
                for (ClientInfo client : clientInfoList) {
                    new SendSyncTask(ServerActivity.this, content, client, true).execute();
                }
                showToast("send sync pressure to all");
                break;
            case R.id.action_send_sync:
                new SendSyncTask(ServerActivity.this, content, clientInfoList.get(currentCheckIndex), false).execute();
                break;
            case R.id.action_send_sync_pressure:
                new SendSyncTask(ServerActivity.this, content, clientInfoList.get(currentCheckIndex), true).execute();
                break;
            case R.id.action_control_device:
                boolean isOpen = skClient.toggleScreenControl(true, clientInfoList.get(currentCheckIndex).getVid());
                if (isOpen) {
                    Intent startIntent = new Intent(ServerActivity.this, RemoteClientActivity.class);
                    startIntent.putExtra(RemoteClientActivity.KEY_ADDRESS_EXTRA, clientInfoList.get(currentCheckIndex).getIp());
                    startIntent.putExtra(RemoteClientActivity.KEY_VID_EXTRA, clientInfoList.get(currentCheckIndex).getVid());
                    startActivity(startIntent);
                }
                break;
            case R.id.action_find_device:
                return skClient.findPhone(Constants.CODE_REQUEST_FIND_INTERVAL, clientInfoList.get(currentCheckIndex).getVid());
            case R.id.action_fetch_clients:
                skClient.fetchClientInfos();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 由于发送同步消息会阻塞线程，所以把同步类消息放在子线程发送
     */
    private static class SendSyncTask extends AsyncTask<String, Void, Boolean> {
        private WeakReference<ServerActivity> activityReference;
        private String content;
        private boolean isPressureTest;
        private ClientInfo info;

        /**
         * only retain a weak reference to the activity
         */
        SendSyncTask(ServerActivity activity, String content, ClientInfo clientInfo, boolean isPressureTest) {
            activityReference = new WeakReference<>(activity);
            this.content = content;
            this.isPressureTest = isPressureTest;
            this.info = clientInfo;
        }

        @Override
        protected Boolean doInBackground(String... str) {
            //发送消息
            int maxTimes = isPressureTest ? 200 : 1;
            for (int i = 0; i < maxTimes; i++) {
                activityReference.get().sendSyncMessage(content, info);
                //发送完成后清空输入框
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            activityReference.get().edSend.setText("");
        }
    }


    private void sendSyncMessage(String content, ClientInfo info) {
        try {
            ExampleTextMessageBody messageBody = new ExampleTextMessageBody.Builder().text(content).build();
            Message message = new Message.Builder(ExampleTextMessageBody.ID_SYNC)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setVid(info.getVid())
                    .setBody(messageBody.getBodyBytes())
                    .build();
            Message msg = skClient.sendSyncMessage(message, 5000);
            if (msg != null) {
                String replyStr = new String(msg.getBody());
                refreshReceive(msg.getPackets()[0].getSn(), msg.getVid(), String.format("%s[reply]:", replyStr));
            } else {
                LogUtils.e("read reply msg timeout");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
