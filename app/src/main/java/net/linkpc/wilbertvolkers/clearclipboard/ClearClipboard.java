package net.linkpc.wilbertvolkers.clearclipboard;

import java.lang.reflect.Field;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by Wilbert on 26/3/2017.
 *
 * Files and locations:
 * - app\src\main\assets\xposed_init        - xposed module marker
 * - app\build.gradle                       - app version, xposed framework version
 * - app\src\main\AndroidManifest.xml       - info
 * - app\src\main\java\net\linkpc\wilbertvolkers\clearclipboard\ClearClipboard.java
 *                                          - source
 * - app\build\outputs                      - apk
 *
 * Info:
 * - https://github.com/rovo89/XposedBridge/wiki/Development-tutorial
 * - https://github.com/hamzahrmalik/CopyToast
 * - https://github.com/Tungstwenty/SecureSamsungClipboard
 * - https://developer.android.com/guide/topics/text/copy-paste.html
 * - https://www.londonappdeveloper.com/how-to-use-git-hub-with-android-studio/
 *
 * Note:
 * - if you change the API you need to consider some dependencies in the code
 * - instant Run was disabled to make sure all classes are included in the apk:
 *      File > Settings > Build* -> Instant Run
 *
 */

public class ClearClipboard implements IXposedHookLoadPackage {

    private static final String PACKAGE_ANDROID = "android";
    private static final String ANDROID_CONTENT_CLIPBOARD_MANAGER = "android.content.ClipboardManager";
    private String msgText = "ClearClipboard: cleared";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(PACKAGE_ANDROID)) {
            //show this only once
            XposedBridge.log("ClearClipboard: loaded");
        }

        try {
            final Class<?> clipboardManagerClass = XposedHelpers.findClass(ANDROID_CONTENT_CLIPBOARD_MANAGER, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(clipboardManagerClass, "setPrimaryClip", ClipData.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                    //XposedBridge.log("ClearClipboard: hooked");

                    Field contextf = XposedHelpers.findField(clipboardManagerClass, "mContext");
                    final Context clipboardManagerContext = (Context) contextf.get(param.thisObject);

                    // handle to the clipboard service.
                    ClipboardManager clipboard = (ClipboardManager)
                            clipboardManagerContext.getSystemService(Context.CLIPBOARD_SERVICE);

                    //Note: always clipboard.getPrimaryClip().getItemCount())=1
                    //XposedBridge.log("ClearClipboard: count=" + clipboard.getPrimaryClip().getItemCount());

                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

                    // Gets the clipboard as text.
                    CharSequence pasteData = item.getText();

                    //Note: for debug purposes will work for length=0 and length=1
                    //pasteData == null --> not text

                    //Note: setPrimaryClip will cause the program to return here 20x
                    //  make sure the clipdata.length()>1 or your device may hang
                    if (pasteData != null && pasteData.length() <=1) {

                        //clear by copy 20x
                        for (int i = 20; i > 0; i--) {
                            //Copy the data to a new ClipData object
                            ClipData clip = ClipData.newPlainText("simple text", "ClrClip" + i);

                            //Set the clipboard's primary clip.
                            clipboard.setPrimaryClip(clip);
                        }
                        XposedBridge.log("ClearClipboard: cleared");

                        //Toast message from UI thread using Looper.getMainLooper()
                        new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    new Handler(Looper.getMainLooper()).post(
                                            new Runnable() {
                                                public void run() {
                                                    Toast.makeText(clipboardManagerContext, msgText, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                    );
                                }
                        }).start();
                    }
                }
            });

        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }
}
