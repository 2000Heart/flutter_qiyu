package org.leanflutter.plugins.flutter_qiyu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.qiyukf.unicorn.api.event.EventCallback;
import com.qiyukf.unicorn.api.event.UnicornEventBase;
import com.qiyukf.unicorn.api.event.entry.RequestPermissionEventEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

public class RequestPermissionEvent implements UnicornEventBase<RequestPermissionEventEntry> {
    private final Map<String, String> h5MessageHandlerMap = new HashMap<>();

    private Context mApplicationContext;
    private EventChannel.EventSink mEventSink;

    public RequestPermissionEvent(Context context, EventChannel.EventSink eventSink) {
        mApplicationContext = context;
        mEventSink = eventSink;
        h5MessageHandlerMap.put("android.permission.RECORD_AUDIO", "麦克风");
        h5MessageHandlerMap.put("android.permission.CAMERA", "相机");
        h5MessageHandlerMap.put("android.permission.READ_EXTERNAL_STORAGE", "存储");
        h5MessageHandlerMap.put("android.permission.WRITE_EXTERNAL_STORAGE", "存储");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_AUDIO", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_IMAGES", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.READ_MEDIA_VIDEO", "多媒体文件");
        h5MessageHandlerMap.put("android.permission.POST_NOTIFICATIONS", "通知栏权限");
    }

    private String transToPermissionStr(List<String> permissionList) {
        if (permissionList == null || permissionList.size() == 0) {
            return "";
        }
        HashSet<String> set = new HashSet<>();
        for (int i = 0; i < permissionList.size(); i++) {
            if (!TextUtils.isEmpty(h5MessageHandlerMap.get(permissionList.get(i)))) {
                set.add(h5MessageHandlerMap.get(permissionList.get(i)));
            }
        }
        if (set.isEmpty()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        for (String temp : set) {
            str.append(temp);
            str.append("、");
        }
        if (str.length() > 0) {
            str.deleteCharAt(str.length() - 1);
        }
        return str.toString();
    }

    /**
     * 该方法为点击相应的权限场景,用户可以通过RequestPermissionEventEntry.getPermissionList()拿到相应的权限,根据
     * 自己APP的权限规则,作自己的处理.
     * 比如判断客户之前点击的是拒绝权限还是不再询问,可以使用 AppCompatActivity.shouldShowRequestPermissionRationale()
     * 方法等各种情况都是在这个回调中进行自己的处理.
     * 各种情况都处理完了,可以告诉SDK,是要申请权限还是拒绝,调用SDK相应的方法.
     * callback.onProcessEventSuccess(requestPermissionEventEntry):用户同意授予权限
     * callback.onInterceptEvent():用户不授予权限，SDK自己处理不授予权限的提醒;或者用户自己处理不授予权限的提醒,就不要调用这个方法了
     * @param requestPermissionEventEntry 获取权限相关的类
     * @param context  当前界面的 context 对象，使用之前请判断是否为 null
     * @param callback sdk 的回调对象  注意：如果该事件 sdk 不需要回调的时候，这个对象会为 null，所以当使用的时候需要做一下非null判断
     */
    @Override
    public void onEvent(RequestPermissionEventEntry requestPermissionEventEntry, Context context, EventCallback<RequestPermissionEventEntry> callback) {
        //申请权限的场景
        //从本地选择媒体文件(视频和图片):0
        //拍摄视频场景:1
        //保存图片到本地:2
        //保存视频到本地:3
        //选择本地视频:4
        //选择本地文件:5
        //选择本地图片:6
        //拍照:7
        //录音:8
        //视频客服:9
        //通知栏权限:10
        if (context instanceof Activity) {
            if (((Activity) context).isFinishing()) {
                return;
            }
        } else if (context instanceof ContextWrapper) {
            if (((ContextWrapper) context).getBaseContext() instanceof Activity) {
                if (((Activity) ((ContextWrapper) context).getBaseContext()).isFinishing()) {
                    return;
                }
            }
        }
        int type = requestPermissionEventEntry.getScenesType();
        if (type == 10) {
            Toast.makeText(mApplicationContext, "适配Android13,没有通知栏权限,需要给通知栏权限", Toast.LENGTH_SHORT).show();
            return;
        }
        mEventSink.success(requestPermissionEventEntry.getScenesType());
        String permissionName = transToPermissionStr(requestPermissionEventEntry.getPermissionList());
        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.AlertDialog_AppCompat_Light)).setMessage("为保证您" + type + "功能的正常使用，" + "需要使用您的：" + (TextUtils.isEmpty(permissionName) ? "相关" : permissionName) + "权限，\n" + "拒绝或取消不影响使用其他服务")
            .setPositiveButton("确定", (dialog1, which) -> {
                //如果想用户授予权限，需要调用 onProcessEventSuccess 告诉 SDK 处理成功
                callback.onProcessEventSuccess(requestPermissionEventEntry);
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //用户不授予权限，告诉 SDK 用户没有授予调用onInterceptEvent,SDK自己处理不授予权限的提醒
                    //或者用户自己处理不授予权限的提醒,就不要调用这个方法了
                    callback.onInterceptEvent();
                }
            }).create();
        dialog.show();
    }


    /**
     * 当相关权限被拒绝后,客户自己的处理
     *
     * @return 返回值默认为false，若返回值为true，则客户自己处理
     */
    @Override
    public boolean onDenyEvent(Context context, RequestPermissionEventEntry requestPermissionEventEntry) {
        return false;
    }
}
