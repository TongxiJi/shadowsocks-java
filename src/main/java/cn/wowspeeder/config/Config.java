package cn.wowspeeder.config;


import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class Config {

	@SerializedName("server")
	private String server;

	@SerializedName("port_password")
	private Map<Integer,String> portPassword;

	@SerializedName("method")
	private String method;

	@SerializedName("obfs")
	private String obfs;

	@SerializedName("obfsparam")
	private String obfsparam;

	@SerializedName("local_address")
	private String localAddress;

	@SerializedName("local_port")
	private Integer localPort;

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public Map<Integer,String> getPortPassword() {
		return portPassword;
	}

	public void setPortPassword(Map<Integer,String> portPassword) {
		this.portPassword = portPassword;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getObfs() {
		return obfs;
	}

	public void setObfs(String obfs) {
		this.obfs = obfs;
	}

	public String getObfsParam() {
		return obfsparam;
	}

	public void setObfsparam(String obfsparam) {
		this.obfsparam = obfsparam;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	public Integer getLocalPort() {
		return localPort;
	}

	public void setLocalPort(Integer localPort) {
		this.localPort = localPort;
	}
}
