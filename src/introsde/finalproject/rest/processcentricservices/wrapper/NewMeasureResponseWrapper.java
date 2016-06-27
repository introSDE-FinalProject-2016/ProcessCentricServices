package introsde.finalproject.rest.processcentricservices.wrapper;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name="Response")
public class NewMeasureResponseWrapper {
	
	@XmlElement(name="currentHealth")
	@JsonProperty("currentHealth")
	public CurrentMeasureList currentHealth = new CurrentMeasureList();
	
	@XmlElement(name="phrase")
	@JsonProperty("phrase")
	public String phrase = new String();
	
	public void setMeasure(CurrentMeasureList currentHealth) {
		this.currentHealth = currentHealth;
	}
	
	public void setPhrase(String phrase){
		this.phrase = phrase;
	}
}
