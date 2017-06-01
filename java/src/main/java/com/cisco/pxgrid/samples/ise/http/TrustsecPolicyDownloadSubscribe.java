package com.cisco.pxgrid.samples.ise.http;

import java.net.URI;

import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.auth.Credentials;

import com.cisco.pxgrid.model.AccountState;
import com.cisco.pxgrid.model.Service;

/**
 * Demonstrates how to subscribe using REST/WS
 */
public class TrustsecPolicyDownloadSubscribe {
	// Subscribe handler class
	private static class SessionHandler implements StompSubscription.Handler {
		@Override
		public void handle(StompFrame message) {
			System.out.println(new String(message.getContent()));
		}
	}

	public static void main(String [] args) throws Exception {
		// Read environment for config
		SampleConfiguration config = new SampleConfiguration();
		
		PxgridControl control = new PxgridControl(config);
		
		// AccountActivate
		while (control.accountActivate() != AccountState.ENABLED) {
			Thread.sleep(60000);
		}
		Console.log("pxGrid controller version=" + control.getControllerVersion());

		// Session ServiceLookup
		Service[] services = control.lookupService("com.cisco.ise.trustsec");
		if (services.length == 0) {
			Console.log("Session service unavailabe");
			return;
		}
		
		Service sessionService = services[0];
		String wsPubsubServiceName = sessionService.getProperties().get("wsPubsubService");
		String topic = sessionService.getProperties().get("policyDownloadTopic");
		Console.log("wsPubsubServiceName=" + wsPubsubServiceName + " topic=" + topic);
		// Pubsub ServiceLookup
		services = control.lookupService(wsPubsubServiceName);
		if (services.length == 0) {
			Console.log("Pubsub service unavailabe");
			return;
		}

		// Select first one for sample purpose. Should cycle through until connects.
		Service wsPubsubService = services[0];
		String wsURL = wsPubsubService.getProperties().get("wsUrl");
		Console.log("url=" + wsURL);

		// pxGrid AccessSecret
		String secret = control.getAccessSecret(wsPubsubService.getNodeName());

		// WebSocket config
		ClientManager client = ClientManager.createClient();
		SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(config.getSSLContext());
		client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
		client.getProperties().put(ClientProperties.CREDENTIALS,
				new Credentials(config.getUserName(), secret.getBytes()));

		// WebSocket connect
		StompPubsubClientEndpoint endpoint = new StompPubsubClientEndpoint();
		javax.websocket.Session session = client.connectToServer(endpoint, new URI(wsURL));

		// Subscribe
		StompSubscription subscription = new StompSubscription(topic, new SessionHandler());
		endpoint.subscribe(subscription);
		
		Console.log("press <enter> to disconnect...");
		System.in.read();
		
		session.close();
	}
}