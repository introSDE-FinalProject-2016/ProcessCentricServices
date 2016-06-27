package introsde.finalproject.rest.processcentricservices.resources;

import introsde.finalproject.rest.processcentricservices.model.Goal;
import introsde.finalproject.rest.processcentricservices.model.Measure;
import introsde.finalproject.rest.processcentricservices.util.UrlInfo;
import introsde.finalproject.rest.processcentricservices.wrapper.CurrentMeasureList;
import introsde.finalproject.rest.processcentricservices.wrapper.GoalList;
import introsde.finalproject.rest.processcentricservices.wrapper.NewGoalResponseWrapper;
import introsde.finalproject.rest.processcentricservices.wrapper.NewMeasureResponseWrapper;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;

@Stateless
@LocalBean
public class PersonResource {

	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	private UrlInfo urlInfo;
	private String businessLogicServiceURL;
	private String storageServiceURL;
	private ClientConfig clientConfig;
	private Client client;

	private static String mediaType = MediaType.APPLICATION_JSON;

	/**
	 * initialize the connection with the Business Logic Service (SS)
	 */
	public PersonResource(UriInfo uriInfo, Request request) {
		clientConfig = new ClientConfig();
		client = ClientBuilder.newClient(clientConfig);

		this.uriInfo = uriInfo;
		this.request = request;

		this.urlInfo = new UrlInfo();
		this.businessLogicServiceURL = urlInfo.getBusinesslogicURL();
		this.storageServiceURL = urlInfo.getStorageURL();
	}

	private String errorMessage(Exception e) {
		return "{ \n \"error\" : \"Error in PCS, due to the exception: " + e
				+ "\"}";
	}

	// ******************* PERSON ***********************

	/**
	 * GET /person/{idPerson}/measure?name={name}?value={value}
	 * 
	 * I Integration Logic: insertNewMeasure(idPerson, measureName, value) SS
	 * --> setMeasure(idPerson, measureName, value) (save a new measure into
	 * database) BLS --> checkGoal(idPerson, Measure) (check if the new measure
	 * satisfies a goal) SS --> getMotivationQuote() BLS --> getCurrentHealth()
	 * (send list of measures to the client)
	 * 
	 * @return an object of type NewMeasureResponseWrapper
	 */
	@GET
	@Path("{pid}/measure")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response insertNewMeasure(@PathParam("pid") int idPerson,
			@QueryParam("name") String name, @QueryParam("value") int value)
			throws Exception {

		try {
			System.out.println("insertNewMeasure: Starting for idPerson "
					+ idPerson + "...");

			Measure m = setMeasure(idPerson, name, value);
			System.out.println("Measure: " + m.toString());

			Boolean check = checkGoal(idPerson, m);
			System.out.println("Check: " + check);

			String phrase = getPhrase(check);
			System.out.println("Phrase: " + phrase);

			CurrentMeasureList currentHealth = getCurrentHealth(idPerson);
			for (Measure measure : currentHealth.getCurrentMeasureList()) {
				System.out.println(measure.toString());
			}
			NewMeasureResponseWrapper nmrw = createMeasureWrapper(phrase,
					currentHealth);
			return Response.ok(nmrw).build();

		} catch (Exception e) {
			System.out
					.println("PCS Error catch creating post reminder response.getStatus() != 200  ");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	/**
	 * Create an object of type NewMeasureResponseWrapper This wrapper is used
	 * to put together the currentHealth and the motivation phrase to send to
	 * the client
	 * 
	 * @param phrase
	 *            String
	 * @param currentHealth
	 *            CurrentMeasureList
	 * @return NewMeasureResponseWrapper
	 */
	private NewMeasureResponseWrapper createMeasureWrapper(String phrase,
			CurrentMeasureList currentHealth) {
		NewMeasureResponseWrapper n = new NewMeasureResponseWrapper();
		n.setMeasure(currentHealth);
		n.setPhrase(phrase);
		return n;
	}

	/**
	 * This method calls readCurrentHealthDetails and returns list of current
	 * measures Calls one time the BLS
	 * 
	 * @return CurrentMeasureList
	 */
	private CurrentMeasureList getCurrentHealth(int idPerson) {
		String path = "/person/" + idPerson + "/current-health";

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		System.out.println(response);
		return response.readEntity(CurrentMeasureList.class);
	}

	/**
	 * This method checks if goal was achieved
	 * 
	 * @param check
	 *            Boolean
	 * @return String a motivation phrase
	 */
	private String getPhrase(Boolean check) {
		if (check == true) {
			return "Very good, you achieved a new goal!!! :)";
		} else {
			return getMotivationQuote();
		}
	}

	/**
	 * This method calls getQuote and returns some quote * Calls one time the SS
	 * 
	 * @return String
	 */
	private String getMotivationQuote() {
		String path = "/adapter/quote";

		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("result").getString("quote");
	}

	/**
	 * This method checks if the goal is achieved for the measure passed as
	 * parameter Calls one time the BLS
	 * 
	 * @param idPerson
	 *            Person
	 * @param m
	 *            Measure
	 * @return Boolean true if a goal is achieved, false otherwise
	 */
	private Boolean checkGoal(int idPerson, Measure m) {
		String path = "/person/" + idPerson + "/measure/" + m.getMid()
				+ "/check";

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		System.out.println(response);
		return response.readEntity(Boolean.class);
	}

	/**
	 * This method creates a new measure object given a string, corresponding to
	 * the name of a type measure, and the value of the new measure. The new
	 * measure is sent to the Storage Service. Calls one time the SS
	 * 
	 * @param idPerson
	 * @param name
	 * @param value
	 * @return
	 */
	private Measure setMeasure(int idPerson, String name, int value) {
		// create new measure
		Measure newMeasure = new Measure();
		newMeasure.setName(name);
		newMeasure.setValue(String.valueOf(value));

		// post new measure to StorageService
		String path = "/person/" + idPerson + "/measure";

		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.post(Entity.entity(newMeasure, mediaType), Response.class);
		System.out.println(response);

		// retrieve the id of the new saved measure
		Integer idMeasure = response.readEntity(Integer.class);

		// retrieve the new measure from SS
		Measure targetMeasure = getMeasureById(idPerson, idMeasure);
		System.out.println(targetMeasure.toString());

		return targetMeasure;
	}

	/**
	 * GET /person/{idPersonId}/measure/{idMeasure}
	 * 
	 * This method calls getHistoryHealth and returns a measure with {idMeasure}
	 * Calls one time the SS
	 * 
	 * @param idPerson
	 *            Person
	 * @param m
	 *            Measure
	 * @return Measure a measure
	 */
	@GET
	@Path("{pid}/measure/{mid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Measure getMeasureById(@PathParam("pid") int idPerson,
			@PathParam("mid") int idMeasure) {
		try {
			System.out.println("getMeasureById: Reading Measures for idPerson "
					+ idPerson + "...");

			String path = "/person/" + idPerson + "/historyHealth";

			WebTarget service = client.target(storageServiceURL);
			Response response = service.path(path).request().accept(mediaType)
					.get(Response.class);
			System.out.println(response);

			String result = response.readEntity(String.class);
			JSONObject obj = new JSONObject(result);

			List<Measure> measureList = new ArrayList<Measure>();
			JSONArray measureArr = (JSONArray) obj.getJSONArray("measure");

			for (int j = 0; j < measureArr.length(); j++) {
				Measure m = new Measure(measureArr.getJSONObject(j).getInt(
						"mid"), measureArr.getJSONObject(j).getString("name"),
						measureArr.getJSONObject(j).getString("value"),
						measureArr.getJSONObject(j).getString("created"));
				measureList.add(j, m);
			}

			for (int i = 0; i < measureList.size(); i++) {
				Measure m = measureList.get(i);
				if (m.getMid() == idMeasure) {
					System.out.println("Measure:" + m.toString());
					return m;
				}
			}
		} catch (Exception e) {
			System.out.println(errorMessage(e));
		}
		return null;
	}

	// ******************* GOAL ***********************

	/**
	 * GET /person/{idPerson}/goals
	 * 
	 * II Integration Logic: countGoalsAchieved(idPerson) BLS -->
	 * getListGoals(idPerson) (get list of goals) SS -->
	 * getPictureForGoalAchieved() SS --> getPictureForGoalNotAchieved() BLS -->
	 * getGoalsListAchieved(idPerson) (send list of achieved goals to the
	 * client)
	 * 
	 * @return an object of type NewGoalResponseWrapper
	 */
	@GET
	@Path("{pid}/goals")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response countGoalsAchieved(@PathParam("pid") int idPerson)
			throws Exception {

		try {
			System.out.println("countGoalsAchieved: Starting for idPerson "
					+ idPerson + "...");

			GoalList goalList = getListGoals(idPerson);
			for (Goal g : goalList.getGoalList()) {
				System.out.println(g.toString());
			}

			int count = countGoalAchieved(goalList);
			System.out.println("Count: " + count);

			String urlPicture = "";
			if (count == 0) {
				urlPicture = getPictureForGoalNotAchieved();
				System.out.println("URL: " + urlPicture);

			} else {
				urlPicture = getPictureForGoalAchieved();
				System.out.println("URL: " + urlPicture);
			}

			GoalList goalsAchieved = getGoalsListAchieved(idPerson);
			for (Goal g : goalsAchieved.getGoalList()) {
				System.out.println(g.toString());
			}

			NewGoalResponseWrapper nmrw = createGoalWrapper(urlPicture,
					goalsAchieved);
			return Response.ok(nmrw).build();

		} catch (Exception e) {
			System.out
					.println("PCS Error catch creating post reminder response.getStatus() != 200  ");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	/**
	 * Create an object of type NewMeasureResponseWrapper This wrapper is used
	 * to put together the currentHealth and the motivation phrase to send to
	 * the client
	 * 
	 * @param phrase
	 *            String
	 * @param currentHealth
	 *            CurrentMeasureList
	 * @return NewMeasureResponseWrapper
	 */
	private NewGoalResponseWrapper createGoalWrapper(String url,
			GoalList goalList) {
		NewGoalResponseWrapper n = new NewGoalResponseWrapper();
		n.setGoal(goalList);
		n.setURL(url);
		return n;
	}

	/**
	 * This methods calls readGoalListDetails and returns list of goals Calls
	 * one time the BLS
	 * 
	 * @param idPerson
	 * @return
	 */
	private GoalList getListGoals(int idPerson) {
		String path = "/person/" + idPerson + "/goal";

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		System.out.println(response);
		return response.readEntity(GoalList.class);
	}

	/**
	 * This method counts how many goals was achieved
	 * 
	 * @param goalList
	 * @return
	 */
	private int countGoalAchieved(GoalList goalList) {
		int count = 0;
		for (Goal g : goalList.getGoalList()) {
			if (g.isAchieved() == Boolean.TRUE) {
				count++;
			}
		}
		return count;
	}

	/**
	 * This methods calls getPictureGood and returns a specified picture Calls
	 * one time the SS
	 * 
	 * @return
	 */
	private String getPictureForGoalAchieved() {
		String path = "/adapter/pictureGood";

		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("picture").getString("thumbUrl");
	}

	/**
	 * This methods calls getPictureBad and returns a specified picture Calls
	 * one time the SS
	 * 
	 * @return
	 */
	private String getPictureForGoalNotAchieved() {
		String path = "/adapter/pictureBad";

		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("picture").getString("thumbUrl");
	}

	/**
	 * This methods calls readGoalListDetails and returns list of goals Calls
	 * one time the BLS
	 * 
	 * @param idPerson
	 * @return
	 */
	private GoalList getGoalsListAchieved(int idPerson) {
		String path = "/person/" + idPerson + "/goal";

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		GoalList result = response.readEntity(GoalList.class);
		List<Goal> newGoalList = new ArrayList<Goal>();

		for (Goal g : result.getGoalList()) {
			if (g.isAchieved() == Boolean.TRUE) {
				newGoalList.add(g);
			}
		}
		result.setGoalList(newGoalList);
		return result;
	}

}