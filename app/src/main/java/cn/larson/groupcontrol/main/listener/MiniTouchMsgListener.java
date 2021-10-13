package cn.larson.groupcontrol.main.listener;

import android.annotation.TargetApi;
import android.os.Build;

import com.vivo.api.zxwy.interfaces.DeviceAssistant;

import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.listener.IMessageListener;
import cn.larson.groupcontrol.main.msgbody.MiniTouchMsgBody;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.ExecUtils;
import larson.groupcontrol.app.util.LogUtils;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/9/8     @author : larsonzhong
 * <p>
 * miniTouch的消息监听，在这做了解析，直接给上边回调事件，方便上边调用。
 * 事件包括tap，swipe，longClick，keyEvent
 */
public class MiniTouchMsgListener implements IMessageListener {
    public static final short ID = Constants.MSG_MINICAP_INPUT;
    private final int deviceWidth;
    private final int deviceHeight;
    private ExecUtils mExecUtils;
    private boolean isVivo;

    public MiniTouchMsgListener(int deviceWidth, int deviceHeight) {
        this.deviceWidth = deviceWidth;
        this.deviceHeight = deviceHeight;
        mExecUtils = ExecUtils.getInstance();
        isVivo = Build.MODEL.startsWith("vivo");
    }

    @Override
    public void processMessage(Message msg) {
        MiniTouchMsgBody body = new MiniTouchMsgBody(msg.getBody());
        LogUtils.e(body.toString());
        if (body.reqCode == Constants.CODE_REQUEST_TAP) {
            onTap((int)(body.x1 * deviceWidth),
                    (int)(body.y1 * deviceHeight));
        } else if (body.reqCode == Constants.CODE_REQUEST_SWIPE) {
            onSwipe((int)(body.x1 * deviceWidth),
                    (int)(body.y1 * deviceHeight),
                    (int)(body.x2 * deviceWidth),
                    (int)(body.y2 * deviceHeight),
                    (int)body.duration);
        } else if (body.reqCode == Constants.CODE_REQUEST_KEYEVENT) {
            onKeyDown(body.keyCode);
        }
    }

    private void onSwipe(int x1, int y1, int x2, int y2, int duration) {
        String cmd = (isVivo ? "" : "adb shell ") + "input swipe " + x1
                + " " + y1
                + " " + x2
                + " " + y2
                + " " + duration;
        LogUtils.e(cmd);
        execCommand(cmd);
    }

    private void onTap(int x1, int y1) {
        String cmd = (isVivo ? "" : "adb shell ") + "input tap " + x1 + " " + y1;
        LogUtils.e(cmd);
        execCommand(cmd);
    }

    private void onKeyDown(short keyCode) {
        String cmd = (isVivo ? "" : "adb shell ") + "input keyevent " + keyCode;
        LogUtils.e(cmd);
        execCommand(cmd);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void execCommand(String cmd) {
        try {
            if (isVivo) {
                DeviceAssistant deviceAssistant = new DeviceAssistant();
                deviceAssistant.runCmdAsRoot(cmd);
            } else {
                mExecUtils.execRoot(cmd);
            }

        } catch (Exception ignored) {
        }
    }
}
