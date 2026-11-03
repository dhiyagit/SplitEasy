package com.example.spliteasy;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

public class SplitEasyApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private int activeActivityCount = 0;
    private boolean isInBackground = false;
    private static boolean externalActionInProgress = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    public static void setExternalActionInProgress(boolean inProgress) {
        externalActionInProgress = inProgress;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        if (activeActivityCount == 0 && isInBackground) {
            // App transitioned from background to foreground
            if (SessionManager.isLoggedIn(activity) && !externalActionInProgress) {
                SessionManager.setNeedsPinUnlock(activity, true);
            }
        }
        activeActivityCount++;
        isInBackground = false;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (externalActionInProgress) {
            // Do not trigger PIN lock while camera/gallery is active
            return;
        }

        if (SessionManager.needsPinUnlock(activity) &&
                !(activity instanceof PinLockActivity) &&
                !(activity instanceof LoginActivity) &&
                !(activity instanceof RegisterActivity) &&
                !(activity instanceof MainActivity) &&
                !(activity instanceof ForgotPinActivity)) {

            Intent intent = new Intent(activity, PinLockActivity.class);
            activity.startActivity(intent);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {
        activeActivityCount--;
        if (activeActivityCount == 0 && !externalActionInProgress) {
            isInBackground = true;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
