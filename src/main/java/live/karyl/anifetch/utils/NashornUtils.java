package live.karyl.anifetch.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class NashornUtils {

	public static String decryptAnime47(String value, String key) throws ScriptException {
		String jsCode = ("var value = atob(\"%s\");".formatted(key) +
				"var result = JSON.parse(CryptoJS.AES.decrypt(value, \"%s\", { format: CryptoJSAesJson }).toString(CryptoJS.enc.Utf8));")
						.formatted(key);
		ScriptEngineManager engineManager = new ScriptEngineManager();
		ScriptEngine engine = engineManager.getEngineByName("nashorn");
		Object result = engine.eval(jsCode);
		return result.toString();
	}
}
