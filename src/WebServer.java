import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import workbook.Workbook;

public class WebServer {
	public static void main(String[] args) throws Exception {
		Server server = new Server(8080);
		server.setHandler(new AbstractHandler() {
			public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
				new CustomRequestHandler().handle(target, baseRequest, request, response);
			}
		});
		
		server.start();
		
		Workbook workbook = new Workbook();
		workbook.setVariable("server", server);
		workbook.setVariable("overrides", ResourceFileSystem.getOverrides());
		workbook.setVariable("db", new Database());
		workbook.waitForExit();
		
		server.stop();
	}
}