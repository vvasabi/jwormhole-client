package com.bradchen.jwormhole.client;

import java.io.Serializable;

public final class Host implements Serializable {

	private static final long serialVersionUID = 1282196476329161208L;

	private final String domainName;
	private final int port;
	private final long createTime;

	public Host(String domainName, int port) {
		this.domainName = domainName;
		this.port = port;
		this.createTime = System.currentTimeMillis();
	}

	public String getDomainName() {
		return domainName;
	}

	public int getPort() {
		return port;
	}

	public long getCreateTime() {
		return createTime;
	}

}
