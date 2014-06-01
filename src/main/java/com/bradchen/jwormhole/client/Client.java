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
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
	private static final long RENEW_PERIOD = 20;

	private final Settings settings;
	private final JSch jsch;
	private final UserInfo userInfo;
	private final ScheduledExecutorService scheduler;
	private Session session;
	private int localControllerPort;

	public Client(String settingKey, UserInfo userInfo) throws IOException {
		this.settings = new Settings(readDefaultSettings(), readOverrideSettings(), settingKey);
		this.jsch = new JSch();
		this.userInfo = userInfo;
		this.scheduler = Executors.newScheduledThreadPool(1);
	}

	public void connect() throws IOException {
		if (session != null) {
			return;
		}

		try {
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
			throw new RuntimeException(exception);
		}
	}

	private void enableSessionCompression() {
		session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
		session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
		session.setConfig("compression_level", "9");
	}

	public Host createHost(String hostName) throws IOException {
		String result;
		if (hostName == null) {
			result = executeCommand("createHost");
		} else {
			result = executeCommand("createHost " + hostName);
		}
		if (StringUtils.isBlank(result)) {
			return null;
		}
		String[] tokens = result.trim().split(",");
		if (tokens.length != 2) {
			return null;
		}
		return new Host(tokens[0], Integer.parseInt(tokens[1]));
	}

	public void proxyLocalPort(Host host, int localPort) {
		try {
			session.setPortForwardingR(host.getPort(), LOCALHOST, localPort);
			scheduler.scheduleAtFixedRate(() -> {
				try {
					renewHost(host);
				} catch (IOException exception) {
					LOGGER.error("Failed to renew host: " + host.getDomainName(), exception);
					shutdown();
				}
			}, RENEW_PERIOD, RENEW_PERIOD, TimeUnit.SECONDS);
		} catch (JSchException exception) {
			throw new RuntimeException(exception);
		}
	}

	public void renewHost(Host host) throws IOException {
		executeCommand("renewHost " + host.getDomainName());
	}

	public void removeHost(Host host) throws IOException {
		executeCommand("removeHost " + host.getDomainName());
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
				} catch (InterruptedException exception) {
					// ignored
				}
			}
			return sb.toString();
		}
	}

	public void shutdown() {
		if (session != null) {
			session.disconnect();
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

}
