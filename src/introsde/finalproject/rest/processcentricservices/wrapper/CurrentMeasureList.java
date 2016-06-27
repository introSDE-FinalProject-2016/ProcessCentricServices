package introsde.finalproject.rest.processcentricservices.wrapper;

import introsde.finalproject.rest.processcentricservices.model.Measure;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name="currentHealth-profile")
public class CurrentMeasureList {

	@XmlElement(name="measure")
	@JsonProperty("measure")
	public List<Measure> currentMeasureList = new ArrayList<Measure>();
	
	public List<Measure> getCurrentMeasureList() {
		return currentMeasureList;
	}

	public void setCurrentMeasureList(List<Measure> currentMeasureList) {
		this.currentMeasureList = currentMeasureList;
	}
	
}

