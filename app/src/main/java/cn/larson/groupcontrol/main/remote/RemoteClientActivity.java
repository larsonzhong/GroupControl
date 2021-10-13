package cn.larson.groupcontrol.main.remote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.larson.remotedisplay.RemoteDisplayManager;

import java.lang.ref.WeakReference;

import larson.groupcontrol.app.connection.Constants;
import cn.larson.groupcontrol.R;
import cn.larson.groupcontrol.main.ServerSdk;
import cn.larson.groupcontrol.main.msgbody.MiniTouchMsgBody;

/**
 * @author Administrator
 */
@SuppressLint("NewApi")
public class RemoteClientActivity extends Activity implements View.OnTouchListener, View.OnClickListener {
    public static final String KEY_ADDRESS_EXTRA = "address";
    public static final String KEY_VID_EXTRA = "vid";
    private static final float MINITAP_MOVE_LIMIT = 0.01f;
    private String mAddress;
    private short vid;
    private int deviceWidth;
    private int deviceHeight;
    private long lastDownTime;
    private float downX, downY;
    private ServerSdk serverSdk;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_display_client);
        mSurfaceView = findViewById(R.id.main_surface_view);
        mSurfaceView.setOnTouchListener(this);
        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_home).setOnClickListener(this);
        findViewById(R.id.btn_power).setOnClickListener(this);
        findViewById(R.id.btn_reset).setOnClickListener(this);
        serverSdk = ServerSdk.getInstance();
        /**
         * 连接服务器的地址和端口
         * 端口要与启动服务器的端口一致
         */
        String address = "192.168.43.1";
        mAddress = getIntent().getStringExtra(KEY_ADDRESS_EXTRA);
        vid = getIntent().getShortExtra(KEY_VID_EXTRA, (short) 0x00);
        RemoteDisplayManager.startClient(mSurfaceView, TextUtils.isEmpty(mAddress) ? address : mAddress);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        deviceWidth = mSurfaceView.getWidth();
        deviceHeight = mSurfaceView.getHeight();
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RemoteDisplayManager.stopClient();
        new ExitTask(RemoteClientActivity.this, vid).execute();
    }

    /**
     * 绘制触摸滑动路径
     *
     * @param motionEvent MotionEvent
     * @return true
     */
    @Override
    public boolean onTouch(View v, MotionEvent motionEvent) {
        if (v.getId() == R.id.main_surface_view) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastDownTime = System.currentTimeMillis();
                    downX = motionEvent.getX() / deviceWidth;
                    downY = motionEvent.getY() / deviceHeight;
                    break;

                case MotionEvent.ACTION_UP:
                    float upX = motionEvent.getX() / deviceWidth;
                    float upY = motionEvent.getY() / deviceHeight;
                    long lastUpTime = System.currentTimeMillis();
                    if (Math.abs(upX - downX) < MINITAP_MOVE_LIMIT && Math.abs(upY - downY) < MINITAP_MOVE_LIMIT) {
                        serverSdk.sendMiniTouchEvent(
                                new MiniTouchMsgBody.Builder(Constants.CODE_REQUEST_TAP).point1(upX, upY).build(),
                                vid);
                    } else {
                        //点击和抬起在不同点
                        serverSdk.sendMiniTouchEvent(
                                new MiniTouchMsgBody.Builder(Constants.CODE_REQUEST_SWIPE).point1(downX, downY).
                                        point2(upX, upY).duration((short) (lastUpTime - lastDownTime)).build(),
                                vid);
                    }
                    break;
                default:
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        byte keyCode;
        if (v.getId() == R.id.btn_back) {
            keyCode = KeyEvent.KEYCODE_BACK;
        } else if (v.getId() == R.id.btn_home) {
            keyCode = KeyEvent.KEYCODE_HOME;
        } else if (v.getId() == R.id.btn_power) {
            keyCode = KeyEvent.KEYCODE_POWER;
        } else if (v.getId() == R.id.btn_reset) {
            RemoteDisplayManager.resetClient();
            return;
        } else {
            throw new RuntimeException("编码错误，不支持的按键类型");
        }
        MiniTouchMsgBody body = new MiniTouchMsgBody.Builder(Constants.CODE_REQUEST_KEYEVENT).keyCode(keyCode).build();
        serverSdk.sendMiniTouchEvent(body, vid);
    }


    private static class ExitTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<RemoteClientActivity> activityReference;
        private short vid;

        /**
         * only retain a weak reference to the activity
         */
        ExitTask(RemoteClientActivity context, short vid) {
            activityReference = new WeakReference<>(context);
            this.vid = vid;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return ServerSdk.getInstance().toggleScreenControl(false, vid);
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            Activity activity = activityReference.get();
            if (isSuccess) {
                activity.finish();
            } else {
                Toast.makeText(activity, "exit control failed ", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
