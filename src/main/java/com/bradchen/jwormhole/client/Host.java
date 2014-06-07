package com.bradchen.jwormhole.client;

import java.io.Serializable;

/**
 * Stores the parameters of the currently proxied local host.
 */
final class Host implements Serializable {

	private static final long serialVersionUID = 1282196476329161208L;

	private final String domainName;
	private final String name;
	private final int port;
	private final long createTime;

	public Host(String domainName, String name, int port) {
		this.domainName = domainName;
		this.name = name;
		this.port = port;
		this.createTime = System.currentTimeMillis();
	}

	public String getDomainName() {
		return domainName;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}

	public long getCreateTime() {
		return createTime;
	}

}
