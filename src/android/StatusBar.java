package com.chinamobile.status;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;


/**
 * Created by liangzhongtai on 2018/5/21.
 */
public class StatusBar extends CordovaPlugin {
    public final static String TAG = "Statusbar_Plugin";
    public final static int STATUS_FULL_SCREEN_NO = 0;
    public final static int STATUS_FULL_SCREEN    = 1;
    public final static int STATUS_HIDE           = 2;
    public final static int STATUS_SHOW           = 3;
    public final static int STATUS_HEIGHT         = 4;
    public final static int STATUS_KEYBOARD       = 5;
    public final static int FONT_COLOR_LIGHT      = 6;
    public final static int FONT_COLOR_DART       = 7;
    public final static String FIRST_INSTALL_START_APP = "first_install_start_app";


    private CordovaInterface cordova;
    private CallbackContext callbackContext;
    private CallbackContext callbackContextKB;
    private int statusType;
    private String color;
    private IKeyBoardListener kbListener;
    private boolean lastVisible = false;
    private View rootView;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG,"启动StatusBar");
        super.initialize(cordova, webView);
        this.cordova = cordova;
        this.webView = webView;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG,"执行方法Statusbar");
        if ("coolMethod".equals(action)) {
            statusType = args.getInt(0);
            if (statusType != STATUS_KEYBOARD) {
                this.callbackContext = callbackContext;
            }
            Log.d(TAG,"statusType=" + statusType);
            if (args.length() > 1) {
                color = args.getString(1);
            }
            if (statusType == FONT_COLOR_LIGHT) {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatusBarFontIconDark(cordova.getActivity(), false);
                        try {
                            if (rootView != null) {
                                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(glListener);
                            }
                            initKeyBoardListener();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else if (statusType == FONT_COLOR_DART) {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setStatusBarFontIconDark(cordova.getActivity(), true);
                        // 每次修改状态栏字体颜色时，重新设置监听
                        addOnKeyBoardListener(cordova.getActivity());
                    }
                });
            } else if (statusType == STATUS_HEIGHT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                        && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    sendStatusMessage(0, 0);
                } else {
                    Object[] objs = getStatusBarHeightObjs(cordova.getActivity());
                    if ((float) objs[1] > 0) {
                        sendStatusMessage((int) objs[0], (float) objs[1]);
                    }
                }
            } else if (statusType == STATUS_KEYBOARD) {
                callbackContextKB = callbackContext;
                Log.d(TAG,"初始化callbackContextKB=" + callbackContextKB);
                if (rootView == null || kbListener == null) {
                    initKeyBoardListener();
                }
            } else {
                this.cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setWindowStatusBarColor(cordova.getActivity());
                    }
                });
            }
            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    /**
     * 初始化监听
     * */
    private void initKeyBoardListener() {
        kbListener = new IKeyBoardListener() {
            @Override
            public void onKeyBoardVisible(boolean visible, int keyboardHeight) {
                Log.d(TAG, "callbackContextKB=" + callbackContextKB + "_visible=" + visible);
                if (callbackContextKB == null ) {
                    Log.d(TAG, "webView=" + webView);
                    if (webView == null) {
                        return;
                    }
                    String format = "onKeyBoardVisible(" + visible + ")";
                    Log.d(TAG, "format=" + format);
                    webView.loadUrl("javascript:" + format);
                    return;
                }
                // 键盘高度
                Resources res = cordova.getActivity().getBaseContext().getResources();
                float keyboardHeightDP = keyboardHeight / res.getDisplayMetrics().density;
                // 返回键盘开关状态，高度
                sendKeyboardMessage(visible, keyboardHeightDP, keyboardHeight);
            }
        };
        addOnKeyBoardListener(cordova.getActivity());
    }

    @Override
    public Bundle onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    @Override
    public void onDestroy() {
        if (rootView != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(glListener);
        }
        super.onDestroy();
    }

    private void sendStatusMessage(int px, float dp) {
        JSONArray array = new JSONArray();
        try {
            array.put(0,px);
            array.put(1,dp);
            Log.d(TAG,"返回px="+px);
            Log.d(TAG,"返回dp="+dp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK,array);
        callbackContext.sendPluginResult(result);
    }

    private void sendKeyboardMessage(boolean visible, float dp, int px) {
        Log.d(TAG, "callbackContextKB.getCallbackId=" + callbackContextKB.getCallbackId());
        JSONArray array = new JSONArray();
        try {
            array.put(0, visible);
            array.put(1, dp);
            array.put(2, px);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK,array);
        result.setKeepCallback(true);
        callbackContextKB.sendPluginResult(result);
    }

    private void setWindowStatusBarColor(Activity activity) {
        SharedPreferences preferences =
                cordova.getContext().getSharedPreferences("statusbar", Context.MODE_PRIVATE);
        if(preferences != null
           && (Build.BRAND != null && (Build.BRAND.contains("Huawei")||Build.BRAND.contains("Hono")
               ||Build.BRAND.contains("HUAWEI")||Build.BRAND.contains("HONO")
               ||Build.BRAND.contains("huawei")||Build.BRAND.contains("hono")))
           && preferences.getInt(FIRST_INSTALL_START_APP, 0) < 2) {
            int count = preferences.getInt(FIRST_INSTALL_START_APP, 0);
            SharedPreferences.Editor editor = preferences.edit();
            count++;
            editor.putInt(FIRST_INSTALL_START_APP, count);
            editor.apply();
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                if(statusType == STATUS_FULL_SCREEN_NO) {
                    View decorView = window.getDecorView();
                    int option = View.SYSTEM_UI_FLAG_VISIBLE;
                    decorView.setSystemUiVisibility(option);
                    //window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setStatusBarColor(Color.parseColor(color));
                    ActionBar actionBar = activity.getActionBar();
                    if (actionBar!=null) {
                        actionBar.show();
                    }
                } else if (statusType == STATUS_FULL_SCREEN) {
                    View decorView = window.getDecorView();
                    int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                    decorView.setSystemUiVisibility(option);
                    window.setStatusBarColor(Color.TRANSPARENT);
                    ActionBar actionBar = activity.getActionBar();
                    if (actionBar!=null) {
                        actionBar.hide();
                    }
                } else if (statusType == STATUS_HIDE) {
                    View decorView = window.getDecorView();
                    int option = View.INVISIBLE;
                    decorView.setSystemUiVisibility(option);
                    ActionBar actionBar = activity.getActionBar();
                    if (actionBar!=null) {
                        actionBar.hide();
                    }
                } else if (statusType == STATUS_SHOW) {
                    View decorView = window.getDecorView();
                    int option = View.SYSTEM_UI_FLAG_VISIBLE;
                    decorView.setSystemUiVisibility(option);
                    ActionBar actionBar = activity.getActionBar();
                    if (actionBar!=null) {
                        actionBar.show();
                    }
                }
                //底部导航栏
                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setWindowStatusBarColor(Dialog dialog, int colorResId) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = dialog.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(dialog.getContext().getResources().getColor(colorResId));
                //底部导航栏
                //window.setNavigationBarColor(activity.getResources().getColor(colorResId));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= 29) {
            Window window = activity.getWindow();
            //系统版本大于19
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                setTranslucentStatus(window,true);
            }

            //去除灰色遮罩
            //Android5.0以上
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                //Android4.4以上,5.0以下
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
            return;
        }

        // 适配7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Class decorViewClazz = Class.forName("com.android.internal.policy.DecorView");
                Field field = decorViewClazz.getDeclaredField("mSemiTransparentStatusBarColor");
                field.setAccessible(true);
                field.setInt(activity.getWindow().getDecorView(), Color.TRANSPARENT);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 适配5.1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.BRAND!=null&&(Build.BRAND.contains("Huawei")||Build.BRAND.contains("Hono")
                ||Build.BRAND.contains("HUAWEI")||Build.BRAND.contains("HONO")
                ||Build.BRAND.contains("huawei")||Build.BRAND.contains("hono")
                ||Build.BRAND.contains("Meizu")||Build.BRAND.contains("LeEco"))) {
                // 适配华为机型
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                int originStatus = activity.getWindow().getDecorView().getSystemUiVisibility();
                int deStatus = originStatus | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                activity.getWindow().getDecorView().setSystemUiVisibility(deStatus);
                activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            } else {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                View decorView = activity.getWindow().getDecorView();
                int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
            }
        }
        // 适配4.4-5.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            View statusBarView = new View(activity);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    getStatusBarHeight(activity));
            int color = Color.argb(255,255,100,97);
            statusBarView.setBackgroundColor(color);
            decorView.addView(statusBarView, lp);
        }
    }

    private static void setTranslucentStatus( Window win, Boolean on) {
        WindowManager.LayoutParams winParams = win.getAttributes();
        int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            // a|=b的意思就是把a和b按位或然后赋值给a   按位或的意思就是先把a和b都换成2进制，然后用或操作，相当于a=a|b
            winParams.flags = winParams.flags | bits;
        } else {
            //&是位运算里面，与运算  a&=b相当于 a = a&b  ~非运算符
            winParams.flags = winParams.flags & bits;
        }
        win.setAttributes(winParams);
    }

    private static int getStatusBarHeight(Activity activity) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, sbar = 0;
        float sBarDp = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = activity.getBaseContext().getResources().getDimensionPixelSize(x);
            sBarDp = (sbar/activity.getBaseContext().getResources().getDisplayMetrics().density);
            Log.d(StatusBar.TAG,"*******状态栏高度px="+sbar);
            Log.d(StatusBar.TAG,"*******状态栏高度dp="+(sbar/activity.getBaseContext().getResources().getDisplayMetrics().density));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return sbar;
    }
    
    private Object[] getStatusBarHeightObjs(Activity activity) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, sbar = 0;
        float sBarDp = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = activity.getBaseContext().getResources().getDimensionPixelSize(x);
            sBarDp = (sbar/activity.getBaseContext().getResources().getDisplayMetrics().density);
            Log.d(StatusBar.TAG,"*******状态栏高度px="+sbar);
            Log.d(StatusBar.TAG,"*******状态栏高度dp="+sBarDp);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return new Object[]{sbar,sBarDp};
    }

     public void addOnKeyBoardListener(final Activity activity) {
         // 添加键盘监听
         FrameLayout content = (FrameLayout) activity.findViewById(android.R.id.content);
         // 获取到setContentView放进去的View
         rootView = content.getChildAt(0);
         rootView.getViewTreeObserver().addOnGlobalLayoutListener(glListener);
         Log.d(TAG, "初始化监听");
    }

    /**
     * 监听视图树的变化
     * */
    public ViewTreeObserver.OnGlobalLayoutListener glListener = () -> {
        Resources resources = cordova.getActivity().getResources();
        int navigationBarResourceId = resources.getIdentifier("navigation_bar_height"
                , "dimen", "android");
        int navigationBarHeight = resources.getDimensionPixelSize(navigationBarResourceId);
        int statusBarResourceId = resources.getIdentifier("status_bar_height"
                , "dimen", "android");
        int statusBarHeight = resources.getDimensionPixelSize(statusBarResourceId);
        int totalbarHeight = navigationBarHeight + statusBarHeight;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        int screenHeight = rootView.getRootView().getHeight();
        int heightDiff = screenHeight - (r.bottom - r.top);
        Log.d(TAG, "heightDiff=" + heightDiff);
        Log.d(TAG, "totalbarHeight=" + totalbarHeight);
        Log.d(TAG, "kbListener=" + kbListener);
        Log.d(TAG, "lastVisible=" + lastVisible);
        if (kbListener == null) {
            initKeyBoardListener();
        }
        if (heightDiff > totalbarHeight) {
            int keyboardHeight = heightDiff - statusBarHeight- getVirtualHeight(cordova.getActivity());
            if (!lastVisible) {
                kbListener.onKeyBoardVisible(true,  keyboardHeight);
            }
            lastVisible = true;
        } else {
            if (lastVisible) {
                kbListener.onKeyBoardVisible(false, 0);
            }
            lastVisible = false;
        }
    };

    public interface IKeyBoardListener{
        /**
         * 键盘监听回调
         * @param visible:Boolean        键盘是否可见
         * @param keyboardHeight:px      键盘高度
         * */
        void onKeyBoardVisible(boolean visible, int keyboardHeight);
    }

    public static int getVirtualHeight(Activity activity) {
        return getHasVirtualHeight(activity) - getNoVirtualHeight(activity);
    }

    public static int getNoVirtualHeight(Activity activity) {
        return activity.getWindowManager().getDefaultDisplay().getHeight();
    }
    
    /**
     * 获取虚拟返回键的高度
     * */
    public static int getHasVirtualHeight(Activity activity) {
        int dpi = 0;
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        @SuppressWarnings("rawtypes")
        Class c;
        try {
            c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            dpi = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dpi;
    }


    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    /**
     * 小米6系统判断
     * */
    private static boolean isMiUIV6OrAbove() {
        try {
            final Properties properties = new Properties();
            properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            String uiCode = properties.getProperty(KEY_MIUI_VERSION_CODE, null);
            if (uiCode != null) {
                int code = Integer.parseInt(uiCode);
                return code >= 4;
            } else {
                return false;
            }

        } catch (final Exception e) {
            return false;
        }

    }
    /**
     * 小米7系统判断
     * */
    private static boolean isMiUIV7OrAbove() {
        try {
            final Properties properties = new Properties();
            properties.load(new FileInputStream(new File(Environment.getRootDirectory(), "build.prop")));
            String uiCode = properties.getProperty(KEY_MIUI_VERSION_CODE, null);
            if (uiCode != null) {
                int code = Integer.parseInt(uiCode);
                return code >= 5;
            } else {
                return false;
            }

        } catch (final Exception e) {
            return false;
        }

    }

    /**
     * 魅族系统判断
     * */
    private static boolean isFlymeV4OrAbove() {
        String displayId = Build.DISPLAY;
        if (!TextUtils.isEmpty(displayId) && displayId.contains("Flyme")) {
            String[] displayIdArray = displayId.split(" ");
            for (String temp : displayIdArray) {
                //版本号4以上，形如4.x.
                if (temp.matches("^[4-9]\\.(\\d+\\.)+\\S*")) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 设置文字颜色
     */
    public static void setStatusBarFontIconDark(Activity activity, boolean dark) {
        if (isMiUIV6OrAbove() && !isMiUIV7OrAbove()) {
            setMiuiUI(activity, dark);
        } else if (isFlymeV4OrAbove()) {
            setFlymeUI(activity, dark);
        } else {
            setCommonUI(activity, dark);
        }
    }

    /**
     * 设置6.0的字体
     * */
    public static void setCommonUI(Activity activity, boolean dark) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (dark) {
                activity.getWindow().getDecorView().
                        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                activity.getWindow().getDecorView().
                        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }
    }

    /**
     * 设置Flyme的字体
     * */
    public static void setFlymeUI(Activity activity,boolean dark) {
        try {
            Window window = activity.getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            Field darkFlag = WindowManager.LayoutParams.class.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
            Field meizuFlags = WindowManager.LayoutParams.class.getDeclaredField("meizuFlags");
            darkFlag.setAccessible(true);
            meizuFlags.setAccessible(true);
            int bit = darkFlag.getInt(null);
            int value = meizuFlags.getInt(lp);
            if (dark) {
                value |= bit;
            } else {
                value &= ~bit;
            }
            meizuFlags.setInt(lp, value);
            window.setAttributes(lp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置MIUI字体
     * */
    public static void setMiuiUI(Activity activity, boolean dark) {
        try {
            Window window = activity.getWindow();
            Class clazz = activity.getWindow().getClass();
            Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
            Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
            int darkModeFlag = field.getInt(layoutParams);
            Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
            //状态栏亮色且黑色字体
            if (dark) {
                extraFlagField.invoke(window, darkModeFlag, darkModeFlag);
            } else {
                extraFlagField.invoke(window, 0, darkModeFlag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
