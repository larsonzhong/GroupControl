package cn.larson.groupcontrol.main.msgbody;

import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.message.intf.BaseMessageBody;
import larson.groupcontrol.app.util.ArrayUtils;
import larson.groupcontrol.app.util.BytesUtils;
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
 */
public class MiniTouchMsgBody extends BaseMessageBody {
    public final byte reqCode;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public short keyCode;
    public short duration;
    public static final short ID = Constants.MSG_MINICAP_INPUT;

    MiniTouchMsgBody(Builder builder) {
        this.x1 = builder.x1;
        this.y1 = builder.y1;
        this.x2 = builder.x2;
        this.y2 = builder.y2;
        this.reqCode = builder.reqCode;
        this.keyCode = builder.keyCode;
        this.duration = builder.duration;
        this.bodyBytes = builder.bodyBytes;
    }

    public MiniTouchMsgBody(byte[] bodyBytes) {
        this.bodyBytes = bodyBytes;
        this.reqCode = bodyBytes[0];
        switch (reqCode) {
            case Constants.CODE_REQUEST_TAP:
                this.x1 = BytesUtils.bytesToFloat(bodyBytes, 1);
                this.y1 = BytesUtils.bytesToFloat(bodyBytes, 5);
                break;
            case Constants.CODE_REQUEST_SWIPE:
                this.x1 = BytesUtils.bytesToFloat(bodyBytes, 1);
                this.y1 = BytesUtils.bytesToFloat(bodyBytes, 5);
                this.x2 = BytesUtils.bytesToFloat(bodyBytes, 9);
                this.y2 = BytesUtils.bytesToFloat(bodyBytes, 13);
                this.duration = BytesUtils.bytesToShort(bodyBytes, 17);
                break;
            case Constants.CODE_REQUEST_KEYEVENT:
                this.keyCode = BytesUtils.bytesToShort(bodyBytes, 1);
                break;
            default:
                LogUtils.e("无法识别的miniTouch指令");
        }
    }

    @Override
    public String toString() {
        return "MiniTouchMsgBody{" +
                "reqCode=" + reqCode +
                ", x1=" + x1 +
                ", y1=" + y1 +
                ", x2=" + x2 +
                ", y2=" + y2 +
                ", keyCode=" + keyCode +
                ", duration=" + duration +
                '}';
    }

    @Override
    public byte[] getBodyBytes() {
        return bodyBytes;
    }


    public static class Builder extends BaseMessageBody.Builder {
        float x1;
        float y1;
        float x2;
        float y2;
        byte reqCode;
        short keyCode;
        short duration;

        public Builder(byte codeRequest) {
            this.reqCode = codeRequest;
        }

        public Builder point1(float x1, float y1) {
            this.x1 = x1;
            this.y1 = y1;
            return this;
        }

        public Builder point2(float x2, float y2) {
            this.x2 = x2;
            this.y2 = y2;
            return this;
        }

        public Builder duration(short duration) {
            this.duration = duration;
            return this;
        }

        public Builder keyCode(short keyCode) {
            this.keyCode = keyCode;
            return this;
        }

        /**
         * tap:0x01 x:0x0001 y:0x0001
         * swipe:0x02 x:0x0001 y:0x0001 x1:0x0001 y1:0x0001 ms:[0x0001]
         * key event:0x03 keycode:0x0001
         */
        public MiniTouchMsgBody build() {
            //如果有多个字段，则使用数组组装把属性组装成数组封装到bodyBytes
            if (reqCode == Constants.CODE_REQUEST_TAP) {
                this.bodyBytes = ArrayUtils.concatBytes(
                        new byte[]{reqCode}
                        , BytesUtils.floatToBytes(x1)
                        , BytesUtils.floatToBytes(y1));
            } else if (reqCode == Constants.CODE_REQUEST_SWIPE) {
                this.bodyBytes = ArrayUtils.concatBytes(
                        new byte[]{reqCode}
                        , BytesUtils.floatToBytes(x1)
                        , BytesUtils.floatToBytes(y1)
                        , BytesUtils.floatToBytes(x2)
                        , BytesUtils.floatToBytes(y2)
                        , BytesUtils.shortToBytes(duration));
            } else if (reqCode == Constants.CODE_REQUEST_KEYEVENT) {
                this.bodyBytes = ArrayUtils.concatBytes(
                        new byte[]{reqCode}
                        , BytesUtils.shortToBytes(keyCode));
            }
            return new MiniTouchMsgBody(this);
        }


    }

}