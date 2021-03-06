package ryebread761.tickertoast;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class TickerToast implements IXposedHookZygoteInit, IXposedHookInitPackageResources {
	ArrayList<Pair<String, String>> toastsShowing;
	ArrayList<Pair<XResources, String>> packageResources;
	XModuleResources modRes;
	int x=0;

	@Override
	public void initZygote(final StartupParam startupParam) throws Throwable {
		toastsShowing = new ArrayList<Pair<String, String>>();
		packageResources = new ArrayList<Pair<XResources,String>>();
		modRes = XModuleResources.createInstance(startupParam.modulePath, null);
		XposedHelpers.findAndHookMethod(Toast.class, "show", new XC_MethodReplacement() {
			
			protected Object replaceHookedMethod(MethodHookParam param)
					throws Throwable {
				Context toastContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
				Toast theToast = (Toast) param.thisObject;
				View toastView = theToast.getView();
				TextView toastTextView;
				if (toastView instanceof TextView) {
					toastTextView = (TextView) toastView;
				} else if (toastView instanceof ViewGroup){
					ViewGroup viewGroup = (ViewGroup) toastView;
					toastTextView = getTheFrickinTextView(viewGroup);
				} else {
					//BAD CODE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
					toastTextView = null;
				}
				final NotificationManager nm = (NotificationManager) toastContext.getSystemService(Context.NOTIFICATION_SERVICE);
				PackageManager pm = toastContext.getPackageManager();
				BitmapDrawable toastBD = (BitmapDrawable) pm.getApplicationIcon(toastContext.getPackageName());
				XResources res = null;
				for (Pair<XResources, String> aPackage : packageResources) {
					if (aPackage.second.equals(toastContext.getPackageName())) {
						res = aPackage.first;
					}
				}
				
				if (res == null) {
					//Couldn't get XResources, just show original toast.
					return XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
				}
				
				final Notification n = new Notification.Builder(toastContext)
				.setContentText(toastTextView.getText())
				.setLargeIcon(toastBD.getBitmap())
				.setSmallIcon(res.addResource(toastContext.getResources(), R.drawable.ic_notif))
				.setContentTitle(toastContext.getPackageName())
				.setTicker(toastTextView.getText()).build();
				final int thisId = x;
				long thisTime = (long) (3400 * Math.ceil(toastTextView.getText().length() / 30));
				long timeToAdd = 0;
				for (Pair<String,String> thisPair : toastsShowing) {
					timeToAdd += Long.parseLong(thisPair.second);
				}
				thisTime += timeToAdd;
				new Handler().postDelayed(new Runnable() {
					
					@Override
					public void run() {
						nm.notify(0, n);
						
					}
				}, timeToAdd);
				XposedBridge.log("Time to add: " + Long.toString(timeToAdd) + "This time: " + Long.toString(thisTime));
				toastsShowing.add(new Pair<String, String>(Integer.toString(thisId), Long.toString(thisTime)));
				new Handler().postDelayed(new Runnable() {
					
					@Override
					public void run() {
						nm.cancelAll();
						for (int num = 0; 0 < toastsShowing.size(); num++) {
							if (Integer.parseInt(toastsShowing.get(num).first) == thisId) {
								toastsShowing.remove(num);
								break;
							}
						}
					}
				}, thisTime);
				++x;
				return null;
			}
		});
		
	}
	
	
	//BAD CODE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
	public TextView getTheFrickinTextView(ViewGroup viewGroup) {
		for (int i = 0; i < viewGroup.getChildCount(); ++i) {
			if (viewGroup.getChildAt(i) instanceof TextView) {
				return (TextView) viewGroup.getChildAt(i);
			} else if (viewGroup.getChildAt(i) instanceof ViewGroup) {
				return getTheFrickinTextView((ViewGroup) viewGroup.getChildAt(i));
			}
		}
		return null;
	}


	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam)
			throws Throwable {
		packageResources.add(new Pair<XResources, String>(resparam.res, resparam.packageName));
	}

}
