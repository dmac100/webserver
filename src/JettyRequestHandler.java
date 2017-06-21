import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.AbstractTemplateLoader;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;

public class JettyRequestHandler {
	public static class Handler {
		private final String requestMethod;
		private final String pathTemplate;
		private final Runnable callback;
		
		public Handler(String requestMethod, String pathTemplate, Runnable callback) {
			this.requestMethod = requestMethod;
			this.pathTemplate = pathTemplate;
			this.callback = callback;
		}

		public String getRequestMethod() {
			return requestMethod;
		}

		public String getPathTemplate() {
			return pathTemplate;
		}

		public Runnable getCallback() {
			return callback;
		}
	}
	
	private static class ResourceFileSystemTemplateLoader extends AbstractTemplateLoader {
		public ResourceFileSystemTemplateLoader(String prefix, String suffix) {
			setPrefix(prefix);
			setSuffix(suffix);
		}
		
		public TemplateSource sourceAt(String uri) throws IOException {
			String location = resolve(normalize(uri));
			
			try(InputStream inputStream = ResourceFileSystem.getInputStream(location)) {
				return new StringTemplateSource(location, IOUtils.toString(inputStream, Charsets.UTF_8));
			}
		}
	}
	
	private static final String STATIC_PATH = "/webcontent";
	private static final String TEMPLATE_PATH = "/templates";
	
	private final List<Handler> handlers = new ArrayList<>();
	
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected Map<String, String> pathVariables = new HashMap<>();
	
	public void get(String path, Runnable handler) {
		handlers.add(new Handler("GET", path, handler));
	}
	
	public void post(String path, Runnable handler) {
		handlers.add(new Handler("POST", path, handler));
	}
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		this.request = request;
		this.response = response;
		
		Handler defaultHandler = new Handler("GET", target, () -> {
			try {
				renderStatic(target, response.getOutputStream());
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		Iterable<Handler> currentHandlers = Iterables.concat(handlers, Collections.singleton(defaultHandler));

		for(Handler handler:currentHandlers) {
			if(request.getMethod().equals(handler.getRequestMethod()) && matchPath(target, handler.getPathTemplate())) {
				try {
					response.setStatus(HttpServletResponse.SC_OK);
					handler.getCallback().run();
					baseRequest.setHandled(true);
					return;
				} catch(Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
	private boolean matchPath(String target, String pathTemplate) {
		pathVariables.clear();
		
		String[] splitTarget = target.split("/");
		String[] splitPathTemplate = pathTemplate.split("/");
		
		if(splitTarget.length != splitPathTemplate.length) {
			return false;
		}
		
		for(int i = 0; i < splitTarget.length; i++) {
			if(splitPathTemplate[i].startsWith(":")) {
				pathVariables.put(splitPathTemplate[i].substring(1), splitTarget[i]);
			} else if(!splitTarget[i].equals(splitPathTemplate[i])) {
				return false;
			}
		}
		
		return true;
	}

	protected void renderJson(Object context) {
		try {
			response.setContentType("application/json");
			response.getWriter().write(new Gson().toJson(context));
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	protected void renderTemplate(String name, Object context) {
		try {
			TemplateLoader loader = new ResourceFileSystemTemplateLoader(TEMPLATE_PATH, ".hbs");
			Handlebars handlebars = new Handlebars(loader);
			
			String content = handlebars.compile(name).apply(context);
			Map<String, String> contentMap = singletonMap("content", content);
			handlebars.compile("layout").apply(contentMap, response.getWriter());
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	protected void renderStatic(String path, OutputStream outputStream) {
		try {
			String contentType = MimeTypes.getDefaultMimeByExtension(path.replaceAll(".*\\.", "."));
			if(contentType != null) {
				response.setContentType(contentType);
			}
			
			try(InputStream inputStream = ResourceFileSystem.getInputStream(STATIC_PATH + path)) {
				IOUtils.copy(inputStream, outputStream);
			}
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	protected void sendRedirect(String path) {
		try {
			response.sendRedirect(path);
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}