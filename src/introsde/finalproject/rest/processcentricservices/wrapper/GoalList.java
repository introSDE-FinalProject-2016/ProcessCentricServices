package introsde.finalproject.rest.processcentricservices.wrapper;

import introsde.finalproject.rest.processcentricservices.model.Goal;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "goal-profile")
public class GoalList {

	@XmlElement(name = "goal")
	@JsonProperty("goal")
	public List<Goal> goalList = new ArrayList<Goal>();

	public List<Goal> getGoalList() {
		return goalList;
	}

	public void setGoalList(List<Goal> goalList) {
		this.goalList = goalList;
	}

}
