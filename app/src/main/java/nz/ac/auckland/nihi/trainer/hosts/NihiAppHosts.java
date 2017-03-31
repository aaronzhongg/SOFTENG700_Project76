package nz.ac.auckland.nihi.trainer.hosts;

public enum NihiAppHosts {

	METIS_CTRU_AUCKLAND_AC_NZ("metis.ctru.auckland.ac.nz", 1469, "NIHI Production Host (Linux)"),

	// METISWIN_CTRU_AUCKLAND_AC_NZ("metiswin.ctru.auckland.ac.nz", 1469, "NIHI Backup Host (Windows)"),

	ODIN_CS_AUCKLAND_AC_NZ("odin.cs.auckland.ac.nz", 8081, "Odin Test Host"),

	ODIN_CLOUDAPP_NET("uoa-odin.cloudapp.net", 1469, "Andrew's Azure VM");

	private final String hostName;

	private final int port;

	private final String name;

	private NihiAppHosts(String hostName, int port, String name) {
		this.hostName = hostName;
		this.port = port;
		this.name = name;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}
}
