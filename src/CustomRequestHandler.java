import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRequestHandler extends JettyRequestHandler {
	public CustomRequestHandler() {
		get("/list", () -> {
			try(Database dao = new Database()) {
				List<Map<String, Object>> context = dao.runSql("select * from t");
				renderTemplate("list", context);
			}
		});
		
		post("/new", () -> {
			String a = request.getParameter("a");
			String b = request.getParameter("b");
			String c = request.getParameter("c");
			
			try(Database dao = new Database()) {
				dao.runSql("insert into t values ( ?, ?, ? )", Integer.parseInt(a), Integer.parseInt(b), c);
				sendRedirect("/list");
			}
		});
		
		get("/json", () -> {
			Map<String, Object> context = new HashMap<>();
			context.put("a", "123");
			context.put("b", "456");
			renderJson(context);
		});
		
		get("/test/:value", () -> {
			Map<String, Object> context = new HashMap<>();
			context.put("a", Arrays.asList("111", "222", "333"));
			context.put("b", pathVariables.get("value"));
			renderTemplate("hello", context);
		});
	}
}