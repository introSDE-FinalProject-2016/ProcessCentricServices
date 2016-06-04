package introsde.finalproject.rest.processcentricservices.resources;

import introsde.finalproject.rest.processcentricservices.util.UrlInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
	private String adapterServiceURL;
	private String processCentricServiceURL;

	private static String mediaType = MediaType.APPLICATION_JSON;

	/**
	 * initialize the connection with the Business Logic Service (SS)
	 */
	public PersonResource(UriInfo uriInfo, Request request) {
		this.uriInfo = uriInfo;
		this.request = request;

		this.urlInfo = new UrlInfo();
		this.businessLogicServiceURL = urlInfo.getBusinesslogicURL();
		this.storageServiceURL = urlInfo.getStorageURL();
		this.adapterServiceURL = urlInfo.getAdapterURL();
		this.processCentricServiceURL = urlInfo.getProcesscentricURL();
	}

	private String errorMessage(Exception e) {
		return "{ \n \"error\" : \"Error in Process Centric Services, due to the exception: "
				+ e + "\"}";
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
	 */
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

	/**
	 * PUT /person/{idPerson}/checkGoal/{measureName} II Integration Logic
	 *
	 * checkGoal(idPerson, inputGoalJSON, measureName) calls the following
	 * methods: *readPersonDetails(idPerson) --> BLS
	 * *updateGoal(idPerson,inputGoalJSON,measureName) --> SS
	 * *getPerson(idPerson) --> SS
	 * 
	 * @return
	 */
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

	/**
	 * POST /person/{idPerson}/insertNewMeasure/{measureName} III Integration
	 * Logic
	 * 
	 * insertNewMeasure(idPerson, inputMeasureJSON, measureName) calls the
	 * following methods: *readPersonDetails(idPerson) --> BLS
	 * *createMeasure(idPerson, inputMeasureJSON) --> SS *getPerson(idPerson)
	 * --> SS
	 * 
	 * @return
	 */
	@POST
	@Path("{pid}/insertNewMeasure/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response insertNewMeasure(@PathParam("pid") int idPerson,
			String inputMeasureJSON,
			@PathParam("measureName") String measureName) throws Exception {

		System.out
				.println("insertNewMeasure: Third integration logic which calls 3 services sequentially "
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

		JSONObject measureTarget = null;

		JSONObject currentMeasureObj = (JSONObject) obj.get("currentHealth");
		JSONArray measureArr = currentMeasureObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		if (measureTarget == null) {
			// POST PERSON/{IDPERSON}/MEASURE --> SS
			path = "/person/" + idPerson + "/measure";
			service = client.target(storageServiceURL);

			response = service
					.path(path)
					.request()
					.accept(mediaType)
					.post(Entity.entity(inputMeasureJSON, mediaType),
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

		currentMeasureObj = (JSONObject) obj.get("currentHealth");
		measureArr = currentMeasureObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		System.out.println("Measure: " + measureTarget.get("name"));
		System.out.println("Value: " + measureTarget.get("value"));

		xmlBuild = "<measure>";
		xmlBuild += "<id>" + measureTarget.get("mid") + "</id>";
		xmlBuild += "<type>" + measureTarget.get("name") + "</type>";
		xmlBuild += "<value>" + measureTarget.get("value") + "</value>";
		xmlBuild += "</measure>";

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);

		System.out.println(jsonPrettyPrintString);

		return Response.ok(jsonPrettyPrintString).build();
	}

	/**
	 * POST /person/{idPerson}/insertNewGoal/{measureName} IV Integration Logic:
	 * 
	 * insertNewGoal(idPerson, inputGoalJSON, measureName) calls
	 * *readPersonDetails(idPerson) --> BLS *createGoal(idPerson, inputGoalJSON)
	 * --> SS *getPerson(idPerson) --> SS
	 * 
	 * @return
	 */
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

	/**
	 * POST /person/{idPerson}/verifyGoal/{measureName} V Integration Logic
	 * 
	 * verifyGoal(idPerson, inputGoalJSON, measureName) method calls the
	 * following methods: *readPersonDetails(idPerson) --> BLS
	 * *createGoal(idPerson, inputGoalJSON) --> SS *getPerson(idPerson) --> SS
	 * *readMotivationGoal(idPerson, measureName) --> BLS
	 * 
	 * @return
	 */
	@POST
	@Path("{pid}/verifyGoal/{measureName}")
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@Produces(MediaType.APPLICATION_JSON)
	public Response verifyGoal(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName, String inputGoalJSON)
			throws Exception {

		System.out
				.println("verifyGoal: Firth integration logic which calls 4 services sequentially "
						+ "from Storage and Business Logic Services in Process Centric Services...");

		// I. GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);

		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		if (response.getStatus() != 200) {
			System.out.println("Status1: " + response.getStatus());
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(externalErrorMessageBLS(response.toString()))
					.build();
		}

		System.out.println("Status1: " + response.getStatus());

		String result = response.readEntity(String.class);

		JSONObject obj = new JSONObject(result);

		JSONObject goalTarget = null;
		JSONObject goalObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		// check if goal target exists
		if (goalTarget == null) {
			System.out.println(measureName
					+ " does not exist. I created a new goal");

			// POST PERSON/{IDPERSON}/GOAL --> BLS
			path = "/person/" + idPerson + "/goal";

			clientConfig = new ClientConfig();
			client = ClientBuilder.newClient(clientConfig);
			service = client.target(businessLogicServiceURL);

			response = service
					.path(path)
					.request()
					.accept(mediaType)
					.post(Entity.entity(inputGoalJSON, mediaType),
							Response.class);

			if (response.getStatus() != 200) {
				System.out.println("Status2: " + response.getStatus());
				System.out
						.println("Business Logic Service Error catch response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessageBLS(response.toString()))
						.build();
			}

			System.out.println("Status2: " + response.getStatus());
		}
		
		String xmlBuild = getPersonDetails(idPerson, measureName);
		return Response.ok(xmlBuild).build();
		// return Response.ok(result).build();
	}

	/**
	 * This method call readPersonDetails from BLS
	 * 
	 * @return JSONObject
	 */
	private String getPersonDetails(int idPerson, String measureName) {
		
		// GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;

		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(businessLogicServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		if (response.getStatus() != 200) {
			System.out.println("Status: " + response.getStatus());
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
		}

		System.out.println("Status: " + response.getStatus());

		String result = response.readEntity(String.class);

		JSONObject obj = new JSONObject(result);

		JSONObject measureTarget = null;
		JSONObject currentMeasureObj = (JSONObject) obj.get("currentHealth");
		JSONArray measureArr = currentMeasureObj.getJSONArray("measure");
		for (int i = 0; i < measureArr.length(); i++) {
			if (measureArr.getJSONObject(i).getString("name")
					.equals(measureName)) {
				measureTarget = measureArr.getJSONObject(i);
			}
		}

		JSONObject goalTarget = null;
		JSONObject goalObj = (JSONObject) obj.get("goals");
		JSONArray goalArr = goalObj.getJSONArray("goal");
		for (int i = 0; i < goalArr.length(); i++) {
			if (goalArr.getJSONObject(i).getString("type").equals(measureName)) {
				goalTarget = goalArr.getJSONObject(i);
			}
		}

		String xmlBuild = " ";

		xmlBuild = "<measure>";
		xmlBuild += "<name>" + measureTarget.get("name") + "</name>";
		xmlBuild += "<value>" + measureTarget.get("value") + "</value>";
		xmlBuild += "</measure>";

		xmlBuild += "<goal>";
		xmlBuild += "<name>" + goalTarget.get("type") + "</name>";
		xmlBuild += "<value>" + goalTarget.get("value") + "</value>";
		xmlBuild += "<achieved>" + goalTarget.get("achieved") + "</achieved>";
		xmlBuild += "</goal>";

		JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
		String jsonPrettyPrintString = xmlJSONObj.toString(4);
		System.out.println(jsonPrettyPrintString);
		return jsonPrettyPrintString;
	}

	/***
	 * GET /person/{idPerson}/comparisonInfo/{measureName} VI Integration Logic
	 * 
	 * ComparisonInfo(idPerson, measureName) method calls the following methods:
	 * *readPersonDetails(idPerson) --> BLS *comparisonValueOfMeasure(idPerson,
	 * measureName) --> BLS *readMeasureTypes() --> PCS *getQuote() --> AS
	 * 
	 * @param idPerson
	 * @param measureName
	 * @return
	 * @throws Exception
	 */
	@GET
	@Path("{pid}/comparisonInfo/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response comparisonInfo(@PathParam("pid") int idPerson,
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
			xmlBuild = "<measure>" + measureName + " does not exist "
					+ "</measure>";
			xmlBuild += "<goal>" + measureName + " exist " + "</goal>";

		} else if ((measureTarget != null) && (goalTarget == null)) {
			xmlBuild = "<measure>" + measureName + " exist " + "</measure>";
			xmlBuild += "<goal>" + measureName + " does not exist " + "</goal>";

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

			// III. GET /MEASURETYPES
			String measureType = getMeasureType(measureName);

			// IV. GET PERSON/{IDPERSON}/MOTIVATION-GOAL/{MEASURENAME} --> BLS
			// IV. GET /MOTIVATION-QUOTE --> AS
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

	/**
	 * This method check if goal was achieved
	 * 
	 * @param check
	 *            Boolean
	 * @return String a motivation phrase
	 */
	private String getPhrase(Boolean check, int idPerson, String measureName) {
		if (check == true) {
			return "Very good, you achieved a new goal!!! :)";
		} else {
			return getMotivationGoal(idPerson, measureName);
		}
	}

	/**
	 * This method call readMotivationGoal from BLS and returns a motivation
	 * goal phrase
	 * 
	 * @return String
	 */
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

	/**
	 * This method call getQuote from AS and returns a quote
	 * 
	 * @return String
	 */
	private String getMotivationQuote() {
		String path = "/motivation-quote";
		ClientConfig clientConfig = new ClientConfig();

		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service = client.target(adapterServiceURL);
		Response response = service.path(path).request().accept(mediaType)
				.get(Response.class);

		String result = response.readEntity(String.class);
		JSONObject obj = new JSONObject(result);
		return obj.getJSONObject("result").getString("quote");
	}

	/**
	 * This method call measureTypes from PCS and returns measureType
	 * 
	 * @return String
	 */
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

}