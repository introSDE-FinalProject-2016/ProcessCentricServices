package introsde.finalproject.rest.processcentricservices.model;

public class Measure {

	private int mid;
	private String name;
	private String value;
	private String created;

	// Constructor measure class
	public Measure(){}
	
	public Measure(int mid, String name, String value, String created) {
		this.mid = mid;
		this.name = name;
		this.value = value;
		this.created = created;
	}
	
	public int getMid() {
		return mid;
	}

	public void setMid(int mid) {
		this.mid = mid;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}
	
	public String toString() {
		return "Measure ( " + mid + ", " + name + ", "
				+ value + ", " + created + " )";
	}
	
}

