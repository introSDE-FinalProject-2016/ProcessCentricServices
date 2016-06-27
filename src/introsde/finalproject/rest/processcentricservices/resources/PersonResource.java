package introsde.finalproject.rest.processcentricservices.resources;

import introsde.finalproject.rest.processcentricservices.model.Measure;
import introsde.finalproject.rest.processcentricservices.util.UrlInfo;
import introsde.finalproject.rest.processcentricservices.wrapper.CurrentMeasureList;
import introsde.finalproject.rest.processcentricservices.wrapper.HistoryMeasureList;
import introsde.finalproject.rest.processcentricservices.wrapper.NewMeasureResponseWrapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

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
	private String processCentricServiceURL;
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
		this.processCentricServiceURL = urlInfo.getProcesscentricURL();
	}

	private String errorMessage(Exception e) {
		return "{ \n \"error\" : \"Error in PCS, due to the exception: " + e
				+ "\"}";
	}

	private String externalErrorMessageSS(String e) {
		return "{ \n \"error\" : \"Error in External Storage Services, due to the exception: "
				+ e + "\"}";
	}

	private String externalErrorMessageBLS(String e) {
		return "{ \n \"error\" : \"Error in External BUsiness Logic Services, due to the exception: "
				+ e + "\"}";
	}

	// ******************* PERSON ***********************

	/**
	 * GET /person/{idPerson}/measure?name={name}?value={value} 
	 * 
	 * I Integration Logic: insertNewMeasure(idPerson, measureName, value)
	 * 			SS --> setMeasure(idPerson, measureName, value) (save a new measure into database) 
	 * 			BLS --> checkGoal(idPerson, Measure) (check if the new measure satisfies a goal)
	 * 			SS --> getMotivationQuote()
	 * 			BLS --> getCurrentHealth() (send list of measures to the client)
	 * 
	 * @return an object of type NewMeasureResponseWrapper
	 */
	@GET
	@Path("{pid}/measure")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response insertNewMeasure(@PathParam("pid") int idPerson,
			@QueryParam("name") String name,
			@QueryParam("value") int value) throws Exception {

		try {
			System.out
					.println("insertNewMeasure: Starting for idPerson " + idPerson + "...");
			
			Measure m = setMeasure(idPerson, name, value);
			System.out.println("Measure: " + m.toString());
			
			Boolean check = checkGoal(idPerson, m);
			System.out.println("Check: " + check);
			
			String phrase = getPhrase(check);
			System.out.println("Phrase: " + phrase);
			
			CurrentMeasureList currentHealth = getCurrentHealth(idPerson);
			for(Measure measure: currentHealth.getCurrentMeasureList()){
				System.out.println(measure.toString());
			}
			NewMeasureResponseWrapper nmrw = createWrapper(phrase, currentHealth);
    		return Response.ok(nmrw).build();
		
		} catch (Exception e) {
			System.out
					.println("PCS Error catch creating post reminder response.getStatus() != 200  ");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	
	/**
     * Create an object of type NewMeasureResponseWrapper
     * This wrapper is used to put together the currentHealth and the
     * motivation phrase to send to the client
     * @param phrase String
     * @param currentHealth CurrentMeasureList
     * @return NewMeasureResponseWrapper
     */
	private NewMeasureResponseWrapper createWrapper(String phrase, CurrentMeasureList currentHealth) {
		NewMeasureResponseWrapper n = new NewMeasureResponseWrapper();
		n.setMeasure(currentHealth);
		n.setPhrase(phrase);
		return n;
	}
	
	/**
	 * This method calls readCurrentHealthDetails and returns list of current measures
	 * Calls one time the BLS
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
	 * @param check Boolean
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
	 * This method calls getQuote and returns some quote
	 * * Calls one time the SS
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
	 * This method checks if the goal is achieved for the measure passed as parameter
	 * Calls one time the BLS
	 * @param idPerson Person
	 * @param m Measure
	 * @return Boolean true if a goal is achieved, false otherwise
	 */
	private Boolean checkGoal(int idPerson, Measure m) {
		String path = "/person/" + idPerson + "/measure/" + m.getMid() + "/check";

		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		
		System.out.println(response);
		return response.readEntity(Boolean.class);
	}
	
	/**
	 * This method creates a new measure object given a string,
	 * corresponding to the name of a type measure, and the value of the new measure.
	 * The new measure is sent to the Storage Service.
	 * Calls one time the SS
	 * @param idPerson
	 * @param name
	 * @param value
	 * @return
	 */
	private Measure setMeasure(int idPerson, String name, int value){
		//create new measure
		Measure newMeasure = new Measure();
		newMeasure.setName(name);
		newMeasure.setValue(value);

		//post new measure to StorageService
		String path = "/person/" + idPerson + "/measure";
		
		WebTarget service = client.target(storageServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.post(Entity.entity(newMeasure, mediaType), Response.class);
		System.out.println(response);
		
		//retrieve the id of the new saved measure
		Integer idMeasure = response.readEntity(Integer.class);
		
		//retrieve the new measure from SS
		Measure targetMeasure = getMeasureById(idPerson, idMeasure);
		System.out.println(targetMeasure.toString());
		
		return targetMeasure;
	}
	
	/**
	 * This method calls getHistoryHealth and returns a measure with {idMeasure}
	 * Calls one time the SS
	 * @param idPerson Person
	 * @param m Measure
	 * @return Measure a measure
	 */
	/**
	 * GET /person/{idPersonId}/measure/{idMeasure}
	 * Return the measure with {measureId}
	 * @return Measure a measure
	 */
	@GET
	@Path("{pid}/measure/{mid}")
	@Produces( MediaType.APPLICATION_JSON )
	public Measure getMeasureById(@PathParam("pid") int idPerson, @PathParam("mid") int idMeasure) {
		try{
			System.out.println("getMeasureById: Reading Measures for idPerson "+ idPerson +"...");
			
			String path = "/person/" + idPerson + "/historyHealth";

			WebTarget service = client.target(storageServiceURL);
			Response response = service.path(path).request().accept(mediaType)
					.get(Response.class);
			System.out.println(response);
			
			String result = response.readEntity(String.class);
			JSONObject obj = new JSONObject(result);
			
			List<Measure> measureList = new ArrayList<Measure>();
			JSONArray measureArr = (JSONArray)obj.getJSONArray("measure");
			
			for (int j = 0; j < measureArr.length(); j++) {
				Measure m = new Measure(measureArr.getJSONObject(j).getInt("mid"), 
										measureArr.getJSONObject(j).getString("name"), 
										measureArr.getJSONObject(j).getInt("value"), 
										measureArr.getJSONObject(j).getString("created"));
				measureList.add(j, m);
			}
			
			for(int i=0; i<measureList.size(); i++){
				Measure m = measureList.get(i);
				if(m.getMid() == idMeasure){
					System.out.println("Measure:" + m.toString());
					return m;
				}
			}
		}catch(Exception e){
			System.out.println(errorMessage(e));
		}
		return null;
	}
	
	
	

	
	
	/*
	
	*//**
	 * This method call readMotivationGoal from BLS and returns a motivation
	 * goal phrase
	 * 
	 * @return String
	 *//*
	
	private String getMotivationGoal(int idPerson, String measureName) {
		String path = "/person/" + idPerson + "/motivation-goal/" + measureName;
		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("motivationGoal").getJSONObject("goal")
				.getString("motivation");
	}
	
	*//**
	 * PUT /person/{idPerson}/checkMeasure/{measureName} I Integration Logic
	 * 
	 * checkMeasure(idPerson, inputMeasureJSON, measureName) calls the following
	 * methods: *readPersonDetails(idPerson) --> BLS *updateMeasure(idPerson,
	 * inputMeasureJSON, measureName) --> SS *comparisonValueOfMeasure(idPerson,
	 * inputMeasureJSON, measureName) --> BLS *readMotivationHealth(idPerson,
	 * measureName) --> BLS *readMotivationGoal(idPerson, measureName) --> BLS
	 * *getPicture() --> SS
	 * 
	 * @return
	 *//*
	@PUT
	@Path("{pid}/checkMeasure/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkMeasure(@PathParam("pid") int idPerson,
			String inputMeasureJSON,
			@PathParam("measureName") String measureName) throws Exception {

		System.out
				.println("checkMeasure: First integration logic which calls 5 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);
		JSONObject measureTarget = null;

		JSONObject obj1 = new JSONObject(result);
		JSONObject currentObj = (JSONObject) obj1.get("currentHealth");
		JSONArray measureArr = currentObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		if (measureTarget == null) {
			xmlBuild = "<measure>" + measureName + " don't exist "
					+ "</measure>";

		} else {
			System.out.println("measureID: " + measureTarget.get("mid"));
			System.out.println("measureType: " + measureTarget.get("name"));

			// PUT PERSON/{IDPERSON}/MEASURE/{IDMEASURE} --> SS
			path = "/person/" + idPerson + "/measure/"
					+ measureTarget.get("mid");

			service = client.target(storageServiceURL);
			response = service.path(path).request(mediaType)
					.put(Entity.json(inputMeasureJSON));

			if (response.getStatus() != 200) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			// GET PERSON/{IDPERSON}/COMPARISON-VALUE/{MEASURENAME} --> BLS
			path = "/person/" + idPerson + "/comparison-value/"
					+ measureTarget.get("name");

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet request = new HttpGet(businessLogicServiceURL + path);
			HttpResponse resp = httpClient.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(resp
					.getEntity().getContent()));

			StringBuffer rs = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				rs.append(line);
			}

			JSONObject obj2 = new JSONObject(rs.toString());

			if (resp.getStatusLine().getStatusCode() != 200) {
				System.out
						.println("Business Logic Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageBLS(response.toString()))
						.build();
			}

			JSONObject comparisonInfo = (JSONObject) obj2
					.getJSONObject("comparison-information");
			JSONObject obj3 = null;
			JSONObject res = null;

			System.out.println("measureValueUpdated: "
					+ comparisonInfo.get("measureValue"));
			System.out.println("result: " + comparisonInfo.get("result"));
			System.out.println("measure: " + comparisonInfo.get("measure"));
			System.out.println("goalValue: " + comparisonInfo.get("goalValue"));
			System.out.println("measureValue: "
					+ comparisonInfo.get("measureValue"));

			if (comparisonInfo.get("result").equals("ok")) {
				// GET PERSON/{IDPERSON}/MOTIVATION-GOAL/{MEASURENAME} --> BLS
				path = "/person/" + idPerson + "/motivation-goal/"
						+ measureTarget.get("name");
				System.out.println("path-4: " + path);

				httpClient = new DefaultHttpClient();
				request = new HttpGet(businessLogicServiceURL + path);
				resp = httpClient.execute(request);

				rd = new BufferedReader(new InputStreamReader(resp.getEntity()
						.getContent()));
				rs = new StringBuffer();
				line = "";
				while ((line = rd.readLine()) != null) {
					rs.append(line);
				}

				obj3 = new JSONObject(rs.toString());
				res = obj3.getJSONObject("motivationGoal")
						.getJSONObject("goal");
				System.out.println("quote: " + res.getString("motivation"));

				if (resp.getStatusLine().getStatusCode() != 200) {
					System.out
							.println("Business Logic Service Error catch response.getStatus() != 200");
					return Response
							.status(Response.Status.INTERNAL_SERVER_ERROR)
							.entity(externalErrorMessageBLS(response.toString()))
							.build();
				}
			} else {
				// GET PERSON/{IDPERSON}/MOTIVATION-HEALTH/{MEASURENAME} --> BLS
				path = "/person/" + idPerson + "/motivation-health/"
						+ measureTarget.get("name");

				httpClient = new DefaultHttpClient();
				request = new HttpGet(businessLogicServiceURL + path);
				resp = httpClient.execute(request);

				rd = new BufferedReader(new InputStreamReader(resp.getEntity()
						.getContent()));
				rs = new StringBuffer();
				line = "";
				while ((line = rd.readLine()) != null) {
					rs.append(line);
				}

				obj3 = new JSONObject(rs.toString());
				res = obj3.getJSONObject("motivationHealth").getJSONObject(
						"measure");
				System.out.println("quote: " + res.getString("motivation"));

				if (resp.getStatusLine().getStatusCode() != 200) {
					System.out
							.println("Business Logic Service Error catch response.getStatus() != 200");
					return Response
							.status(Response.Status.INTERNAL_SERVER_ERROR)
							.entity(externalErrorMessageBLS(response.toString()))
							.build();
				}
			}

			// GET ADAPTER/PICTURE
			path = "/adapter/picture";

			httpClient = new DefaultHttpClient();
			request = new HttpGet(storageServiceURL + path);
			resp = httpClient.execute(request);

			rd = new BufferedReader(new InputStreamReader(resp.getEntity()
					.getContent()));

			rs = new StringBuffer();
			line = "";
			while ((line = rd.readLine()) != null) {
				rs.append(line);
			}

			JSONObject obj4 = new JSONObject(rs.toString());
			System.out.println("picture_url: "
					+ obj4.getJSONObject("picture").getString("thumbUrl"));

			if (resp.getStatusLine().getStatusCode() != 200) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			xmlBuild = "<updatedCurrentHealth>";
			xmlBuild += "<measureID>" + measureTarget.get("mid")
					+ "</measureID>";
			xmlBuild += "<measureType>" + measureTarget.get("name")
					+ "</measureType>";
			xmlBuild += "<measureValueUpdated>"
					+ comparisonInfo.get("measureValue")
					+ "</measureValueUpdated>";
			xmlBuild += "</updatedCurrentHealth>";

			xmlBuild += "<comparisonInformation>";
			xmlBuild += "<result>" + comparisonInfo.get("result") + "</result>";
			xmlBuild += "<measure>" + comparisonInfo.get("measure")
					+ "</measure>";
			xmlBuild += "<goalValue>" + comparisonInfo.get("goalValue")
					+ "</goalValue>";
			xmlBuild += "<measureValue>" + comparisonInfo.get("measureValue")
					+ "</measureValue>";
			xmlBuild += "</comparisonInformation>";

			xmlBuild += "<resultInformation>";
			xmlBuild += "<picture_url>"
					+ obj4.getJSONObject("picture").get("thumbUrl")
					+ "</picture_url>";
			xmlBuild += "<quote>" + res.get("motivation") + "</quote>";
			xmlBuild += "</resultInformation>";

		}

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	*//**
	 * PUT /person/{idPerson}/checkGoal/{measureName} II Integration Logic
	 *
	 * checkGoal(idPerson, inputGoalJSON, measureName) calls the following
	 * methods: *readPersonDetails(idPerson) --> BLS
	 * *updateGoal(idPerson,inputGoalJSON,measureName) --> SS
	 * *getPerson(idPerson) --> SS
	 * 
	 * @return
	 *//*
	@PUT
	@Path("{pid}/checkGoal/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkGoal(@PathParam("pid") int idPerson,
			String inputGoalJSON, @PathParam("measureName") String measureName)
			throws Exception {

		System.out
				.println("checkGoal: Second integration logic which calls 3 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);

		JSONObject goalTarget = null;

		JSONObject goalsObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalsObj.getJSONArray("goal");

		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		if (goalTarget == null) {
			xmlBuild = "<goal>" + measureName + " don't exist " + "</goal>";

		} else {
			System.out.println("goalID: " + goalTarget.get("gid"));
			System.out.println("goalType: " + goalTarget.get("type"));
			System.out.println("goalValue: " + goalTarget.get("value"));

			// PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
			path = "/person/" + idPerson + "/goal/" + goalTarget.get("gid");

			service = client.target(storageServiceURL);
			response = service.path(path).request(mediaType)
					.put(Entity.json(inputGoalJSON));

			if (response.getStatus() != 200) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			// GET PERSON/{IDPERSON} --> SS
			path = "/person/" + idPerson;

			service = client.target(storageServiceURL);
			response = service.path(path).request().accept(mediaType)
					.get(Response.class);

			if (response.getStatus() != 200) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			result = response.readEntity(String.class);
			obj = new JSONObject(result);

			JSONObject updatedGoalTarget = null;
			goalsObj = (JSONObject) obj.get("goals");
			goalArr = goalsObj.getJSONArray("goal");

			for (int i = 0; i < goalArr.length(); i++) {
				if (goalArr.getJSONObject(i).getString("type")
						.equals(measureName)) {
					updatedGoalTarget = goalArr.getJSONObject(i);
				}
			}

			System.out.println("goalValueUpdated: "
					+ updatedGoalTarget.get("value"));

			xmlBuild = "<goalUpdated>";
			xmlBuild += "<id>" + goalTarget.get("gid") + "</id>";
			xmlBuild += "<measure>" + goalTarget.get("type") + "</measure>";
			xmlBuild += "<valueOld>" + goalTarget.get("value") + "</valueOld>";
			xmlBuild += "<valueUpdated>" + updatedGoalTarget.get("value")
					+ "</valueUpdated>";
			xmlBuild += "</goalUpdated>";

		}

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	*//**
	 * POST /person/{idPerson}/insertNewGoal/{measureName} IV Integration Logic:
	 * 
	 * insertNewGoal(idPerson, inputGoalJSON, measureName) calls
	 * *readPersonDetails(idPerson) --> BLS *createGoal(idPerson, inputGoalJSON)
	 * --> SS *getPerson(idPerson) --> SS
	 * 
	 * @return
	 *//*
	@POST
	@Path("{pid}/insertNewGoal/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response insertNewGoal(@PathParam("pid") int idPerson,
			String inputGoalJSON, @PathParam("measureName") String measureName)
			throws Exception {

		System.out
				.println("insertNewGoal: Fourth integration logic which calls 3 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);

		JSONObject goalTarget = null;

		JSONObject obj = new JSONObject(result);

		JSONObject goalsObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalsObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		if (goalTarget == null) {
			// POST PERSON/{IDPERSON}/GOAL --> SS
			path = "/person/" + idPerson + "/goal";
			service = client.target(storageServiceURL);

			response = service
					.path(path)
					.request()
					.accept(mediaType)
					.post(Entity.entity(inputGoalJSON, mediaType),
							Response.class);
			if (response.getStatus() != 201) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 201");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}
		}

		// GET PERSON/{IDPERSON} --> SS
		path = "/person/" + idPerson;

		client = ClientBuilder.newClient(clientConfig);
		service = client.target(storageServiceURL);

		response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Storage Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageSS(response.toString()))
					.build();
		}

		result = response.readEntity(String.class);

		obj = new JSONObject(result);

		goalsObj = (JSONObject) obj.get("goals");
		goalArr = goalsObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		System.out.println("Goal: " + goalTarget.get("type"));
		System.out.println("Value: " + goalTarget.get("value"));
		System.out.println("Achieved: " + goalTarget.get("achieved"));

		xmlBuild = "<goal>";
		xmlBuild += "<id>" + goalTarget.get("gid") + "</id>";
		xmlBuild += "<measure>" + goalTarget.get("type") + "</measure>";
		xmlBuild += "<value>" + goalTarget.get("value") + "</value>";
		xmlBuild += "<achieved>" + goalTarget.get("achieved") + "</achieved>";
		xmlBuild += "</goal>";

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();

	}

	*//**
	 * GET /person/{idPerson}/verifyGoal/{measureName} V Integration Logic
	 * 
	 * verifyGoal(idPerson, measureName) method calls the following methods:
	 * *readPersonDetails(idPerson) --> BLS *updateGoal(idPerson, idGoal) --> SS
	 * *getPerson(idPerson) --> SS *getMotivationGoal(idPerson, measureName) -->
	 * BLS *getMeasureTypes() --> SS *getPicture() --> SS
	 * 
	 * @return
	 *//*
	@GET
	@Path("{pid}/verifyGoal/{measureName}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces(MediaType.APPLICATION_JSON)
	public Response verifyGoal(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) throws Exception {

		System.out
				.println("verifyGoal: Firth integration logic which calls 6 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// I. GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = " ";

		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		if (response.getStatus() != 200) {
			System.out.println("Status: " + response.getStatus());
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);

		JSONObject obj = new JSONObject(result);

		JSONObject goalTarget = null;
		JSONObject measureTarget = null;

		JSONObject measureObj = (JSONObject) obj.get("currentHealth");
		JSONArray measureArr = measureObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		JSONObject goalObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		if (goalTarget == null) {
			xmlBuild = "<goal>" + measureName
					+ " does not exist. You have to push createGoal button"
					+ "</goal>";

		} else {

			String measureTargetName = measureName;
			double measureTargetValue = 70;

			if (measureTarget != null) {
				measureTargetName = measureTarget.getString("name");
				measureTargetValue = measureTarget.getDouble("value");
			}

			System.out.println("Measure:\n");
			System.out.println("Name: " + measureTargetName);
			System.out.println("Value: " + measureTargetValue);

			System.out.println("Goal:\n");
			System.out.println("ID: " + goalTarget.get("gid"));
			System.out.println("Name: " + goalTarget.get("type"));
			System.out.println("Value: " + goalTarget.get("value"));
			System.out.println("Achieved: " + goalTarget.get("achieved"));

			double goalValueDouble = 0.;
			int goalValueInt = 0;

			String updateInputGoalJSONok = "{" + "\"achieved\":\"" + true
					+ "\"}";
			String updateInputGoalJSONko = "{" + "\"achieved\":\"" + false
					+ "\"}";

			// convert measureName heart rate into heart-rate
			String sGoal = goalTarget.getString("type");
			String goalType = sGoal.replaceAll(" ", "-");

			if (goalType.equals("steps") || goalType.equals("hear-rate")) {
				goalValueInt = goalTarget.getInt("value");
			} else {
				goalValueDouble = goalTarget.getDouble("value");
			}

			String updatePath = null;
			WebTarget updateService = null;
			Response updateResponse = null;

			// check if goal is achieved
			switch (goalType) {
			case "steps":

				// walking more than 5000 steps a day
				if (goalValueInt >= 5000) {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out
							.println("path_put_steps_achieved: " + updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONok, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				} else {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out.println("path_put_steps_notAchieved: "
							+ updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONko, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				}
				break;

			case "water":

				// drinking at least 3 litres of water a day
				if (goalValueDouble >= 3.0) {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out
							.println("path_put_water_achieved: " + updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONok, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				} else {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out.println("path_put_water_notAchieved: "
							+ updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONko, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				}
				break;

			case "sleep":

				// sleeping at least 8 hours a day
				if (goalValueDouble >= 8.0) {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out
							.println("path_put_sleep_achieved: " + updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONok, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				} else {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out.println("path_put_sleep_notAchieved: "
							+ updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONko, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				}
				break;

			case "weight":

				// losing 10% of weight in a month
				double newGoalValue = valoreScontato(measureTargetValue, 10);
				System.out.println("Value losing 10%: " + newGoalValue);

				if (goalValueDouble <= newGoalValue) {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out.println("path_put_weight_achieved: "
							+ updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONok, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				} else {

					// III. PUT PERSON/{IDPERSON}/GOAL/{IDGOAL} --> SS
					updatePath = "/person/" + idPerson + "/goal/"
							+ goalTarget.getInt("gid");
					System.out.println("path_put_weight_notAchieved: "
							+ updatePath);

					updateService = client.target(storageServiceURL);

					updateResponse = updateService
							.path(updatePath)
							.request()
							.accept(mediaType)
							.put(Entity
									.entity(updateInputGoalJSONko, mediaType),
									Response.class);
					if (updateResponse.getStatus() != 200) {
						System.out
								.println("Storage Service Error catch response.getStatus() != 200");
						return Response
								.status(Response.Status.INTERNAL_SERVER_ERROR)
								.entity(externalErrorMessageSS(response
										.toString())).build();
					}
				}
				break;
			}

			// III. GET PERSON/{IDPERSON} --> SS
			String pathGet = "/person/" + idPerson;

			WebTarget serviceGet = client.target(storageServiceURL);
			Response responseGet = serviceGet.path(pathGet).request()
					.accept(mediaType).get(Response.class);

			if (responseGet.getStatus() != 200) {
				System.out.println("Status: " + responseGet.getStatus());
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			String resultGet = responseGet.readEntity(String.class);

			JSONObject objGet = new JSONObject(resultGet);

			JSONObject goalTargetGet = null;

			JSONObject goalObjGet = (JSONObject) objGet.get("goals");
			JSONArray goalArrGet = goalObjGet.getJSONArray("goal");
			for (int i = 0; i < goalArrGet.length(); i++) {
				if (goalArrGet.getJSONObject(i).getString("type")
						.equals(measureName)) {
					goalTargetGet = goalArrGet.getJSONObject(i);
				}
			}

			// IV. GET /MEASURETYPES --> SS
			String measureType = getMeasureType(measureName);

			// IV. GET PERSON/{IDPERSON}/MOTIVATION-GOAL/{MEASURENAME} --> BLS
			String phase = getPhrase(goalTargetGet.getBoolean("achieved"),
					idPerson, measureName);

			// VI. GET ADAPTER/PICTURE --> SS
			path = "/adapter/picture";

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse resp = httpClient.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(resp
					.getEntity().getContent()));

			StringBuffer rs = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				rs.append(line);
			}

			if (resp.getStatusLine().getStatusCode() != 200) {
				System.out
						.println("Storage Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageSS(response.toString()))
						.build();
			}

			JSONObject getPicture = new JSONObject(rs.toString());
			JSONObject pictureObj = getPicture.getJSONObject("picture");
			String pictureUrl = pictureObj.getString("thumbUrl");
			String pictureName = pictureObj.getString("random_tag");

			System.out.println("Picture:");
			System.out.println("Name: " + pictureName);
			System.out.println("URL: " + pictureUrl);

			xmlBuild = "<verifyGoal>";

			xmlBuild += "<person>" + objGet.get("lastname") + ", "
					+ obj.get("firstname") + "</person>";

			xmlBuild += "<goal>";
			xmlBuild += "<name>" + goalTargetGet.get("type") + "</name>";
			xmlBuild += "<value>" + goalTargetGet.get("value") + "</value>";
			xmlBuild += "<type>" + measureType + "</type>";
			xmlBuild += "<achieved>" + goalTargetGet.get("achieved")
					+ "</achieved>";
			xmlBuild += "<motivation>" + phase + "</motivation>";
			xmlBuild += "<picture>" + pictureUrl + "</picture>";
			xmlBuild += "</goal>";

			xmlBuild += "</verifyGoal>";

		}

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	*//**
	 * This method return the value of current weight measure - 10%
	 * 
	 * @param valore
	 * @param sconto
	 * @return
	 *//*
	public static double valoreScontato(double valore, double sconto) {
		double out = valore * sconto / 100;
		out = valore - out;
		return out;
	}

	*//**
	 * GET /person/{idPerson}/comparisonInfo/{measureName} VI Integration Logic
	 * 
	 * ComparisonInfo(idPerson, measureName) method calls the following methods:
	 * *readPersonDetails(idPerson) --> BLS *comparisonValueOfMeasure(idPerson,
	 * measureName) --> BLS *readMeasureTypes() --> SS *getQuote() --> SS
	 * 
	 * @param idPerson
	 * @param measureName
	 * @return
	 * @throws Exception
	 *//*
	@GET
	@Path("{pid}/comparisonValue/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response comparisonValue(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) throws Exception {

		System.out
				.println("comparisonInfo: Sixth integration logic which calls 4 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// I. GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);

		JSONObject goalTarget = null;
		JSONObject measureTarget = null;

		JSONObject obj = new JSONObject(result);

		// check if list current measure contains the measure target
		JSONObject currentObj = (JSONObject) obj.get("currentHealth");
		JSONArray measureArr = currentObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		// check if list goal contains the goal target
		JSONObject goalsObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalsObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		if ((measureTarget == null) && (goalTarget != null)) {
			xmlBuild = "<measure>" + measureName
					+ " does not exist. You have to push createMeasure button "
					+ "</measure>";
			xmlBuild += "<goal>" + measureName + " exist " + "</goal>";

		} else if ((measureTarget != null) && (goalTarget == null)) {
			xmlBuild = "<measure>" + measureName + " exist " + "</measure>";
			xmlBuild += "<goal>" + measureName
					+ " does not exist. You have to push createGoal button "
					+ "</goal>";

		} else if ((measureTarget == null) && (goalTarget == null)) {
			xmlBuild = "<measure>" + measureName
					+ " does not exist. You have to push createMeasure button "
					+ "</measure>";
			xmlBuild += "<goal>" + measureName
					+ " does not exist. You have to push createGoal button "
					+ "</goal>";

		} else {

			// II. GET PERSON/{IDPERSON}/COMPARISON-VALUE/{MEASURENAME} --> BLS
			path = "/person/" + idPerson + "/comparison-value/"
					+ measureTarget.get("name");

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet request = new HttpGet(businessLogicServiceURL + path);
			HttpResponse resp = httpClient.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(resp
					.getEntity().getContent()));

			StringBuffer rs = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				rs.append(line);
			}

			JSONObject obj2 = new JSONObject(rs.toString());

			if (resp.getStatusLine().getStatusCode() != 200) {
				System.out
						.println("Business Logic Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageBLS(response.toString()))
						.build();
			}

			JSONObject comparisonInfo = (JSONObject) obj2
					.getJSONObject("comparison-information");

			// III. GET /MEASURETYPES --> SS
			String measureType = getMeasureType(measureName);

			// IV. GET /MOTIVATION-QUOTE --> SS
			String quote = getMotivationQuote();

			xmlBuild = "<info>";

			xmlBuild += "<measure>";
			xmlBuild += "<name>" + measureTarget.get("name") + "</name>";
			xmlBuild += "<value>" + measureTarget.get("value") + "</value>";
			xmlBuild += "<type>" + measureType + "</type>";
			xmlBuild += "</measure>";

			xmlBuild += "<goal>";
			xmlBuild += "<name>" + goalTarget.get("type") + "</name>";
			xmlBuild += "<value>" + goalTarget.get("value") + "</value>";
			xmlBuild += "<achieved>" + goalTarget.get("achieved")
					+ "</achieved>";
			xmlBuild += "</goal>";

			xmlBuild += "<comparison>";
			xmlBuild += "<result>" + comparisonInfo.get("result") + "</result>";
			xmlBuild += "<quote>" + quote + "</quote>";
			xmlBuild += "</comparison>";

			xmlBuild += "</info>";
		}

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	

	*//**
	 * This method call measureTypes from SS and returns measureType
	 * 
	 * @return String
	 *//*
	private String getMeasureType(String measureName) {
		String path = "/measureTypes";
		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(processCentricServiceURL);
		Response response = service.path(path).request(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("measureTypes").getString(measureName);
	}

	*//**
	 * GET /person/{idPerson}/checkNewMeasure/{mid} VII Integration Logic
	 * 
	 * CheckNewMeasure(idPerson, idMeasure) method calls the following methods:
	 * *readPersonDetails(idPerson) --> BLS *readMeasureTypes() --> PCS
	 * 
	 * @param idPerson
	 * @param idMeasure
	 * @return
	 * @throws Exception
	 *//*
	@GET
	@Path("{pid}/checkNewMeasure/{mid}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkNewMeasure(@PathParam("pid") int idPerson,
			@PathParam("mid") int idMeasure) throws Exception {

		System.out
				.println("checkNewMeasure: Seventh integration logic which calls 2 services "
						+ "from Business Logic Services in Process Centric Services...");

		// I. GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);

		JSONObject measureTarget = null;

		JSONObject obj = new JSONObject(result);

		// check if list current measure contains the measure target
		JSONObject currentObj = (JSONObject) obj.get("currentHealth");
		JSONArray measureArr = currentObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).get("mid").equals(idMeasure)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		// II. GET /MEASURETYPES --> PCS
		String measureType = getMeasureType(measureTarget.getString("name"));

		xmlBuild = "<newMeasure>";
		xmlBuild += "<name>" + measureTarget.get("name") + "</name>";
		xmlBuild += "<value>" + measureTarget.get("value") + "</value>";
		xmlBuild += "<type>" + measureType + "</type>";
		xmlBuild += "<created>" + measureTarget.get("created") + "</created>";
		xmlBuild += "</newMeasure>";

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	*//**
	 * GET /person/{idPerson}/checkNewGoal/{gid} VIII Integration Logic
	 * 
	 * CheckNewGoal(idPerson, idGoal) method calls the following methods:
	 * *readPersonDetails(idPerson) --> BLS *readMeasureTypes() --> PCS
	 * 
	 * @param idPerson
	 * @param idMeasure
	 * @return
	 * @throws Exception
	 *//*
	@GET
	@Path("{pid}/checkNewGoal/{gid}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkNewGoal(@PathParam("pid") int idPerson,
			@PathParam("gid") int idGoal) throws Exception {

		System.out
				.println("checkNewGoal: Seventh integration logic which calls 2 services "
						+ "from Business Logic Services in Process Centric Services...");

		// I. GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		String xmlBuild = "";

		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);

		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);
		if (response.getStatus() != 200) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		String result = response.readEntity(String.class);

		JSONObject goalTarget = null;

		JSONObject obj = new JSONObject(result);

		// check if list current goals contains the goal target
		JSONObject goalsObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalsObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).get("gid").equals(idGoal)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		// II. GET /MEASURETYPES --> PCS
		String measureType = getMeasureType(goalTarget.getString("type"));

		xmlBuild = "<newGoal>";
		xmlBuild += "<name>" + goalTarget.get("type") + "</name>";
		xmlBuild += "<value>" + goalTarget.get("value") + "</value>";
		xmlBuild += "<type>" + measureType + "</type>";
		xmlBuild += "<startDateGoal>" + goalTarget.get("startDateGoal")
				+ "</startDateGoal>";
		xmlBuild += "<endDateGoal>" + goalTarget.get("endDateGoal")
				+ "</endDateGoal>";
		xmlBuild += "<achieved>" + goalTarget.get("achieved") + "</achieved>";
		xmlBuild += "</newGoal>";

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}
	*/
}