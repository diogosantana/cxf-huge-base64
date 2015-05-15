package org.apache.cxf.hugebase64.jetty;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.webapp.WebAppContext;

public class LocalJettyServer {

	public static void main(String[] args) {
		LocalJettyServer s = new LocalJettyServer();
		Server server = s.starServer();
		try {
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public Server starServer() {
		Server server = new Server();

		SocketConnector socketConnector = new SocketConnector();
		socketConnector.setPort(8080);

		server.setConnectors(new Connector[] { socketConnector });

		WebAppContext webApp = new WebAppContext();
		webApp.setContextPath("/huge64");
		webApp.setDefaultsDescriptor("org/eclipse/jetty/webapp/webdefault.xml");
		webApp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
		ResourceCollection collection = new ResourceCollection(new String[] { "src/test/webapp", "src/main/webapp" });
		webApp.setBaseResource(collection);

		server.setHandler(webApp);

		try {
			server.start();
			return server;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
