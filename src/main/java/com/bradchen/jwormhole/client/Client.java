package com.bradchen.jwormhole.client;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * jWormhole client.
 */
public class Client {

	private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

	// in class path
	private static final String DEFAULT_SETTINGS_FILE = "settings.default.properties";

	// relative to $HOME
	private static final String OVERRIDE_SETTINGS_FILE = ".jwormhole/client.properties";

	// relative to $HOME
	private static final String PRIVATE_KEY_FILE = ".ssh/id_rsa";
	private static final String KNOWN_HOSTS_FILE = ".ssh/known_hosts";
	private static final Charset UTF8_CHARSET = Charset.forName("utf-8");
	private static final String LOCALHOST = "127.0.0.1";
	private static final int MAX_NUM_RETRIES = 3;
	private static final int RETRY_WAIT_TIME = 3;

	private final Settings settings;
	private final JSch jsch;
	private final UserInfo userInfo;
	private final List<ConnectionLostHandler> connectionClosedHandlers;
	private ScheduledExecutorService scheduler;
	private Session session;
	private Host host;
	private int localPort;
	private int localControllerPort;
	private int numRetries;

	public Client(String server, UserInfo userInfo) throws IOException {
		this.settings = new Settings(readDefaultSettings(), readOverrideSettings(), server);
		this.jsch = new JSch();
		this.userInfo = userInfo;
		this.connectionClosedHandlers = new ArrayList<>();
	}

	public String getProxiedDomainName() {
		if (host == null) {
			return null;
		}
		return host.getDomainName();
	}

	public void addConnectionClosedHandler(ConnectionLostHandler handler) {
		connectionClosedHandlers.add(handler);
	}

	public void removeConnectionClosedHandler(ConnectionLostHandler handler) {
		connectionClosedHandlers.remove(handler);
	}

	public void connect() throws IOException {
		if ((session != null) && session.isConnected()) {
			return;
		}

		try {
			scheduler = Executors.newScheduledThreadPool(1);
			String knownHostsFilePath = getKnownHostsFilePath();
			if (knownHostsFilePath != null) {
				jsch.setKnownHosts(knownHostsFilePath);
			}

			String privateKeyFilePath = getPrivateKeyFilePath();
			if (privateKeyFilePath != null) {
				jsch.addIdentity(privateKeyFilePath);
			}

			session = jsch.getSession(settings.getServerUsername(), settings.getServerSshHost(),
				settings.getServerSshPort());
			session.setUserInfo(userInfo);
			enableSessionCompression();
			session.connect();

			// enable controller socket connection
			localControllerPort = session.setPortForwardingL(0, LOCALHOST,
				settings.getServerControllerPort());
		} catch (JSchException exception) {
			throw new IOException(exception);
		}
	}

	private void enableSessionCompression() {
		session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
		session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
		session.setConfig("compression_level", "9");
	}

	public String proxyLocalPort(int localPort, String name) throws IOException {
		String result;
		if (name == null) {
			result = executeCommand("createHost");
		} else {
			result = executeCommand("createHost " + name);
		}
		if (StringUtils.isBlank(result)) {
			return null;
		}
		String[] tokens = result.trim().split(",");
		if (tokens.length != 3) {
			return null;
		}
		host = new Host(tokens[0], tokens[1], Integer.parseInt(tokens[2]));
		this.localPort = localPort;
		establishLocalPortForwarding(localPort);
		scheduleKeepaliveWorker();
		return host.getDomainName();
	}

	private void scheduleKeepaliveWorker() {
		scheduler.scheduleAtFixedRate(() -> {
			if ((host == null) || (localPort <= 0)) {
				return;
			}

			keepHostAlive();
		}, settings.getKeepaliveInterval(), settings.getKeepaliveInterval(), TimeUnit.SECONDS);
	}

	private void keepHostAlive() {
		try {
			session.sendKeepAliveMsg();
			String response = executeCommand("keepHostAlive " + host.getDomainName());
			if (StringUtils.isBlank(response)) {
				return;
			}
		} catch (Exception ignored) {
		}

		// connection unavailable; attempt to recreate connection
		numRetries = 0;
		scheduler.schedule(new ReconnectWorker(), 0, TimeUnit.SECONDS);
	}

	private void establishLocalPortForwarding(int localPort) throws IOException {
		try {
			session.setPortForwardingR(host.getPort(), LOCALHOST, localPort);
		} catch (JSchException exception) {
			throw new IOException(exception);
		}
	}

	public void shutdown() {
		if (session != null) {
			removeHost();
			session.disconnect();
		}
		if (scheduler != null) {
			scheduler.shutdownNow();
		}
	}

	private void removeHost() {
		if (host == null) {
			return;
		}

		try {
			executeCommand("removeHost " + host.getDomainName());
		} catch (IOException ignored) {
		}
	}

	private String executeCommand(String command) throws IOException {
		try (Socket socket = new Socket(LOCALHOST, localControllerPort)) {
			PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
			InputStream in = socket.getInputStream();
			writer.println(command);
			StringBuilder sb = new StringBuilder();
			byte[] bytes = new byte[1024];
			while (socket.isConnected()) {
				int bytesRead = in.read(bytes);
				if (bytesRead == -1) {
					break;
				}
				if (bytesRead > 0) {
					sb.append(new String(bytes, 0, bytesRead, UTF8_CHARSET));
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException ignored) {
				}
			}
			return sb.toString();
		}
	}

	private static String getPrivateKeyFilePath() throws IOException {
		return getFilePath(PRIVATE_KEY_FILE);
	}

	private static String getKnownHostsFilePath() {
		return getFilePath(KNOWN_HOSTS_FILE);
	}

	private static String getFilePath(String relativePath) {
		String path = System.getenv("HOME") + "/" + relativePath;
		File file = new File(path);
		return file.exists() ? path : null;
	}

	private static Properties readDefaultSettings() throws IOException {
		ClassLoader tcl = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = null;
		try {
			inputStream = tcl.getResourceAsStream(DEFAULT_SETTINGS_FILE);
			return readPropertiesFile(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private static Properties readOverrideSettings() throws IOException {
		File file = new File(System.getenv("HOME") + "/" + OVERRIDE_SETTINGS_FILE);
		if (!file.exists()) {
			return null;
		}

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(file);
			return readPropertiesFile(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}

	private static Properties readPropertiesFile(InputStream inputStream) throws IOException {
		Properties properties = new Properties();
		properties.load(inputStream);
		return properties;
	}

	private class ReconnectWorker implements Runnable {

		@Override
		public void run() {
			try {
				shutdown();
				connect();
				proxyLocalPort(localPort, host.getName());
			} catch (IOException exception) {
				numRetries++;
				if (numRetries < MAX_NUM_RETRIES) {
					LOGGER.warn("Failed to reestablish connection with jWormhole server. Will " +
						"retry in " + RETRY_WAIT_TIME + " seconds...", exception);
					scheduler.schedule(new ReconnectWorker(), RETRY_WAIT_TIME, TimeUnit.SECONDS);
				} else {
					LOGGER.warn("Unable to connect to jWormhole server.", exception);
					shutdown();
					connectionClosedHandlers.parallelStream().forEach(handler ->
							handler.connectionClosed(localPort, host.getDomainName()));
				}
			}
		}

	}

}
