package introsde.finalproject.rest.processcentricservices.model;

public class Goal {

	private int gid;
	private String type;
	private String value;
	private String startDateGoal;
	private String endDateGoal;
	private Boolean achieved;
	private String condition;
	
	public Goal(){}
	
	public Goal(int gid, String type, String value, String startDateGoal, String endDateGoal, Boolean achieved, String condition){
		this.gid = gid;
		this.type = type;
		this.value = value;
		this.startDateGoal = startDateGoal;
		this.endDateGoal = endDateGoal;
		this.achieved = achieved;
		this.condition = condition;
	}

	public int getGid() {
		return gid;
	}

	public void setGid(int gid) {
		this.gid = gid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getStartDateGoal() {
		return startDateGoal;
	}

	public void setStartDateGoal(String startDateGoal) {
		this.startDateGoal = startDateGoal;
	}

	public String getEndDateGoal() {
		return endDateGoal;
	}

	public void setEndDateGoal(String endDateGoal) {
		this.endDateGoal = endDateGoal;
	}

	public Boolean isAchieved() {
		return achieved;
	}

	public void setAchieved(Boolean achieved) {
		this.achieved = achieved;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String toString() {
		return "Goal ( " + gid + ", " + type + ", " + value + ", " 
						 + startDateGoal + ", " + endDateGoal + ", "
						 + achieved + ", " + condition + " )";
	}
}

