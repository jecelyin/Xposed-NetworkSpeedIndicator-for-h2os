package tw.fatminmin.xposed.networkspeedindicator.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;

public class JellyBeanPositionCallbackImpl implements PositionCallback {
    
    private static final String PKG_NAME_SYSTEM_UI = "com.android.systemui";
	private ViewGroup mStatusIcons;
	private ViewGroup mIcons;
	private View view;
	private ViewGroup mNotificationIconArea;
	
	@Override
	public final void setup(final LayoutInflatedParam liparam, final View v) {
	    view = v;
	    
	    FrameLayout root = (FrameLayout) liparam.view;
	    mStatusIcons = (ViewGroup) root.findViewById(liparam.res.getIdentifier("statusIcons", "id",
                PKG_NAME_SYSTEM_UI));
	    mIcons = (ViewGroup) root.findViewById(liparam.res.getIdentifier("icons", "id",
                PKG_NAME_SYSTEM_UI));
	    mNotificationIconArea = (ViewGroup) root.findViewById(liparam.res.getIdentifier("notification_icon_area", "id",
                PKG_NAME_SYSTEM_UI));
	    
	}
	
	@Override
	public final void setAbsoluteLeft() {
		removeFromParent();
		mNotificationIconArea.addView(view, 0);
	}

	@Override
	public final void setLeft() {
		removeFromParent();
		mIcons.addView(view, mIcons.indexOfChild(mStatusIcons));
	}

	@Override
	public final void setRight() {
		removeFromParent();
		mIcons.addView(view);
	}
	
	private final void removeFromParent() {
		if(view.getParent()!=null)
			((ViewGroup)view.getParent()).removeView(view);
	}
	
	@Override
	public final ViewGroup getClockParent() {
		return mIcons;
	}
}
