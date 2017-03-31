package com.odin.android.bioharness.prefs;

public class BioharnessDescription {

	private static final String START = "BioharnessDescription[";
	private static final String END = "]";

	private String name, address;

	public static BioharnessDescription fromString(String value) {
		String[] parts = value.split(",");

		if (parts.length != 2) {
			throw new IllegalArgumentException();
		}

		if (!parts[0].startsWith(START)) {
			throw new IllegalArgumentException();
		}

		if (!parts[1].endsWith(END)) {
			throw new IllegalArgumentException();
		}

		String name = parts[0].substring(START.length());
		String address = parts[1].substring(0, parts[1].length() - END.length());
		return new BioharnessDescription(name, address);
	}

	public BioharnessDescription() {
	}

	public BioharnessDescription(String name, String address) {
		this.name = name;
		this.address = address;
	}

	public boolean isValid() {
		return name != null && address != null && !"".equals(name) && !"".equals(address);
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return START + name + "," + address + END;
	}

}
