package dotest.module.frame.debug;

import android.app.Application;

public class DoApplication extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		doext.app.do_Alipay1_App.getInstance().onCreate(this);
	}	
}
