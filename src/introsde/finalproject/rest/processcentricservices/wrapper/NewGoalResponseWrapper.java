package introsde.finalproject.rest.processcentricservices.wrapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name="Response")
public class NewGoalResponseWrapper {
	
	@XmlElement(name="goalsAchieved")
	@JsonProperty("goals")
	public GoalList goalList = new GoalList();
	
	@XmlElement(name="url")
	@JsonProperty("url")
	public String url = new String();
	
	public void setGoal(GoalList goalList) {
		this.goalList = goalList;
	}
	
	public void setURL(String url){
		this.url = url;
	}
}
