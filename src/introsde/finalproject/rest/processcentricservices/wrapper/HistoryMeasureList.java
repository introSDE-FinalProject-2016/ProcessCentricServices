package introsde.finalproject.rest.processcentricservices.wrapper;

import introsde.finalproject.rest.processcentricservices.model.Measure;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name="historyHealth-profile")
public class HistoryMeasureList {

		@XmlElement(name="measure")
		@JsonProperty("measure")
		public List<Measure> historyMeasureList = new ArrayList<Measure>();

		public List<Measure> getHistoryMeasureList() {
			return historyMeasureList;
		}

		public void setHistoryMeasureList(List<Measure> historyMeasureList) {
			this.historyMeasureList = historyMeasureList;
		}
				
}
