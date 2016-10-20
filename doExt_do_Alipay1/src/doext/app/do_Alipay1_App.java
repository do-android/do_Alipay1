package doext.app;
import android.content.Context;
import core.interfaces.DoIAppDelegate;

/**
 * APP启动的时候会执行onCreate方法；
 *
 */
public class do_Alipay1_App implements DoIAppDelegate {

	private static do_Alipay1_App instance;
	
	private do_Alipay1_App(){
		
	}
	
	public static do_Alipay1_App getInstance() {
		if(instance == null){
			instance = new do_Alipay1_App();
		}
		return instance;
	}
	
	@Override
	public void onCreate(Context context) {
		// ...do something
	}
	
	@Override
	public String getTypeID() {
		return "do_Alipay1";
	}
}
