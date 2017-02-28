package doext.implement;

import java.util.Map;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import com.alipay.sdk.app.AuthTask;
import com.alipay.sdk.app.PayTask;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import doext.alipay1.AuthResult;
import doext.alipay1.PayResult;
import doext.define.do_Alipay1_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Alipay1_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Alipay1_Model extends DoSingletonModule implements do_Alipay1_IMethod {

	private static final int SDK_PAY_FLAG = 1;
	private static final int SDK_AUTH_FLAG = 2;

	private DoIScriptEngine scriptEngine;
	private String callbackFuncName;
	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SDK_PAY_FLAG: {
				PayResult payResult = new PayResult((Map<String, String>) msg.obj);
				/**
				 * 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
				 */
				// String resultInfo = payResult.getResult();// 同步返回需要验证的信息
				String resultStatus = payResult.getResultStatus();
				// 判断resultStatus 为9000则代表支付成功
				if (TextUtils.equals(resultStatus, "9000")) {
					alipayResult((Map<String, String>) msg.obj);
					// 该笔订单是否真实支付成功，需要依赖服务端的异步通知。
					// 支付成功
				} else {
					// 该笔订单真实的支付结果，需要依赖服务端的异步通知。
					// 支付失败
					alipayResult((Map<String, String>) msg.obj);

				}
				break;
			}
			case SDK_AUTH_FLAG: {
				AuthResult authResult = new AuthResult((Map<String, String>) msg.obj, true);
				String resultStatus = authResult.getResultStatus();

				// 判断resultStatus 为“9000”且result_code
				// 为“200”则代表授权成功，具体状态码代表含义可参考授权接口文档
				if (TextUtils.equals(resultStatus, "9000") && TextUtils.equals(authResult.getResultCode(), "200")) {
					// 获取alipay_open_id，调支付时作为参数extern_token 的value
					// 传入，则支付账户为该授权账户
					alipayResult((Map<String, String>) msg.obj);
					// 授权成功
				} else {
					// 其他状态值则为授权失败
					alipayResult((Map<String, String>) msg.obj);
					// 授权失败
				}
				break;
			}
			default:
				break;
			}
		};
	};

	private void alipayResult(@SuppressWarnings("rawtypes") Map map) {
		DoInvokeResult _invokeResult = new DoInvokeResult(do_Alipay1_Model.this.getUniqueKey());
		JSONObject _jsonObject = new JSONObject(map);
		_invokeResult.setResultNode(_jsonObject);
		scriptEngine.callback(callbackFuncName, _invokeResult);
	}

	public do_Alipay1_Model() throws Exception {
		super();
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		// ...do something
		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		// ...do something
		if ("pay".equals(_methodName)) {
			this.pay(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}

		if ("auth".equals(_methodName)) {
			this.auth(_dictParas, _scriptEngine, _callbackFuncName);
			return true;
		}
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 支付；
	 * 
	 * @throws Exception
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_callbackFuncName 回调函数名
	 */
	@Override
	public void pay(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		final String _orderStr = DoJsonHelper.getString(_dictParas, "orderStr", "");// orderStr
		if (TextUtils.isEmpty(_orderStr))
			throw new Exception("orderStr 不能为空");
		final Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		Runnable payRunnable = new Runnable() {
			@Override
			public void run() {
				PayTask alipay = new PayTask(_activity);
				Map<String, String> result = alipay.payV2(_orderStr, true);
				Message msg = new Message();
				msg.what = SDK_PAY_FLAG;
				msg.obj = result;
				mHandler.sendMessage(msg);
			}
		};
		// 必须异步调用
		Thread payThread = new Thread(payRunnable);
		payThread.start();
	}

	@Override
	public void auth(JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		final String _authInfo = DoJsonHelper.getString(_dictParas, "authInfo", "");// authInfo
		if (TextUtils.isEmpty(_authInfo))
			throw new Exception("authInfo 不能为空");
		final Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		this.scriptEngine = _scriptEngine;
		this.callbackFuncName = _callbackFuncName;
		Runnable authRunnable = new Runnable() {

			@Override
			public void run() {
				// 构造AuthTask 对象
				AuthTask authTask = new AuthTask(_activity);
				// 调用授权接口，获取授权结果
				Map<String, String> result = authTask.authV2(_authInfo, true);

				Message msg = new Message();
				msg.what = SDK_AUTH_FLAG;
				msg.obj = result;
				mHandler.sendMessage(msg);
			}
		};

		// 必须异步调用
		Thread authThread = new Thread(authRunnable);
		authThread.start();
	}
}