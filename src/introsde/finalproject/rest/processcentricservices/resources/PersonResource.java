package introsde.finalproject.rest.processcentricservices.resources;

import introsde.finalproject.rest.processcentricservices.util.UrlInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
	}

	private String errorMessage(Exception e) {
		return "{ \n \"error\" : \"Error in Business Logic Services, due to the exception: "
				+ e + "\"}";
	}

	private String externalErrorMessage(String e) {
		return "{ \n \"error\" : \"Error in External services, due to the exception: "
				+ e + "\"}";
	}

	// ******************* PERSON ***********************

	/**
	 * GET /business-service/person/{idPerson} This method calls a getPerson
	 * method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}/checkCurrentHealth/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkCurrentHealth(@PathParam("pid") int idPerson, String inputMeasureJSON, @PathParam("measureName") String measureName){
	
		System.out
		.println("checkCurrentHealth: .........Reading list of details about all people from Storage Services Module in Business Logic Services...");

		//GET PERSON/{IDPERSON} --> BLS
		String path = "/person/" + idPerson;
		String xmlBuild = "";
		
		ClientConfig clientConfig = new ClientConfig();
		Client client = ClientBuilder.newClient(clientConfig);
		WebTarget service =  client.target(businessLogicServiceURL);
		
		Response response = service.path(path).request().accept(mediaType).get(Response.class);
		if(response.getStatus() != 200){
			return Response.status(400).build();
		}
		
		String result = response.readEntity(String.class);
		JSONObject measureTarget;
		
		JSONObject obj1 = new JSONObject(result);
		JSONObject currentObj = (JSONObject) obj1.get("currentHealth");
		JSONArray measureArr = currentObj.getJSONArray("measure");
		for(int i=0;i<measureArr.length();i++){
			if(measureArr.getJSONObject(i).getString("name").equals(measureName)){
				measureTarget = measureArr.getJSONObject(i);
			}
		}
		
		if(measureTarget == null){
			xmlBuild += "<measure>" + measureName + "don't exist " + "</measure>";
			
		}else{
			//PUT PERSON/{IDPERSON}/MEASURE/{IDMEASURE} --> SS
			path = "/person/" + idPerson + "/measure/" + measureTarget.getInt("mid");
			service =  client.target(storageServiceURL);
			response = service.path(path).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(inputMeasureJSON));
			
			if(response.getStatus() != 200){
				return Response.status(400).build();
			}
			
			//GET PERSON/{IDPERSON}/COMPARISON-VALUE/{MEASURENAME} --> BLS
			path = "/person/" + idPerson + "/comparison-value/" + measureTarget.getString("name");
	    	
			DefaultHttpClient httpClient = new DefaultHttpClient();
	    	HttpGet request = new HttpGet(businessLogicServiceURL + path);
	    	HttpResponse resp = httpClient.execute(request);
	    	
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));

	    	StringBuffer rs = new StringBuffer();
	    	String line = "";
	    	while ((line = rd.readLine()) != null) {
	    	    rs.append(line);
	    	}

	    	JSONObject obj2 = new JSONObject(result.toString());
	    	
	    	if(resp.getStatusLine().getStatusCode() != 200){
	    		return Response.status(204).build();
	    	}
			
	    	JSONObject comparisonInfo = (JSONObject) obj2.get("comparison-information");
	    	
	    	
	    	//GET ADAPTER/PICTURE
	    	path = "/adapter/picture";
	    	httpClient = new DefaultHttpClient();
	    	request = new HttpGet(path);
	    	resp = httpClient.execute(request);
	    	
	    	rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
	    	
	    	rs = new StringBuffer();
	    	//String line = "";
	    	while ((line = rd.readLine()) != null) {
	    	    rs.append(line);
	    	}

	    	JSONObject obj3 = new JSONObject(rs.toString());
	    	
	    	if(resp.getStatusLine().getStatusCode() != 200){
	    		return Response.status(204).build();
	    	}

	    	
	    	//GET ADAPTER/QUOTE
	    	path = "/adapter/quote";
	    	httpClient = new DefaultHttpClient();
	    	request = new HttpGet(path);
	    	resp = httpClient.execute(request);
	    	
	    	rd = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));
	    	
	    	rs = new StringBuffer();
	    	//String line = "";
	    	while ((line = rd.readLine()) != null) {
	    	    rs.append(line);
	    	}

	    	JSONObject obj4 = new JSONObject(rs.toString());
	    	
	    	if(resp.getStatusLine().getStatusCode() != 200){
	    		return Response.status(204).build();
	    	}
	    	
		}
		
		
		
		
		countPeople = arr.length();
		if(countPeople > 2)
			RESULT = "OK";
		first_person_id = arr.getJSONObject(0).get("id").toString();
		last_person_id = arr.getJSONObject(countPeople-1).get("id").toString();
		
	}
	
	
	
	
	/**
	 * GET /business-service/person This method calls a getPersonList method in
	 * Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readPersonListDetails() {
		try {
			System.out
					.println("readPersonList: Reading list of details about all people from Storage Services Module in Business Logic Services...");

			String path = "/person";

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject obj = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {
				return Response.ok(obj.toString()).build();
			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * GET /business-service/person/{idPerson} This method calls a getPerson
	 * method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response readPersonDetails(@PathParam("pid") int idPerson) {
		try {
			System.out
					.println("readPerson: Reading Person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject o = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {
				return Response.ok(o.toString()).build();
			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
				// return Response.status(204).build();
			}

		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	/**
	 * POST /business-service/person This method calls a createPerson method in
	 * Storage Services Module
	 * 
	 * @return
	 */
	@POST
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response insertNewPerson(String inputPersonJSON) {
		try {

			System.out
					.println("insertNewPerson: Inserting a new Person from Storage Services Module in Business Logic Services");

			String path = "/person";

			Client client = ClientBuilder.newClient();
			WebTarget service = client.target(storageServiceURL + path);
			Builder builder = service.request(mediaType);

			Response response = builder.post(Entity.json(inputPersonJSON));

			String result = response.readEntity(String.class);

			if (response.getStatus() != 201) {
				System.out
						.println("Storage Service Error response.getStatus() != 201");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			} else {
				return Response.ok(result).build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 201");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * PUT /business-service/person/{idPerson} This method calls a updatePerson
	 * method in Storage Services Module
	 * 
	 * @return
	 */
	@PUT
	@Path("{pid}")
	@Produces({ MediaType.APPLICATION_JSON })
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response updatePerson(@PathParam("pid") int idPerson,
			String inputPersonJSON) {
		try {
			System.out
					.println("updatePerson: Updating Person from Storage Services Module in Business Logic Services");

			String path = "/person/" + idPerson;

			Client client = ClientBuilder.newClient();
			WebTarget webTarget = client.target(storageServiceURL + path);
			Builder builder = webTarget.request(mediaType);

			Response response = builder.put(Entity.json(inputPersonJSON));

			String result = response.readEntity(String.class);

			if (response.getStatus() != 200) {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			} else {
				return Response.ok(result).build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * DELETE /business-service/person/{idPerson} This method calls a
	 * deletePerson method in Storage Services Module
	 * 
	 * @return
	 */
	@DELETE
	@Path("{pid}")
	@Produces({ MediaType.APPLICATION_JSON })
	public Response deletePerson(@PathParam("pid") int idPerson) {
		try {
			System.out
					.println("deletePerson: Deleting Person from Storage Services Module in Business Logic Services");

			String path = "/person/" + idPerson;

			Client client = ClientBuilder.newClient();
			WebTarget webTarget = client.target(storageServiceURL + path);
			Builder builder = webTarget.request(mediaType);

			Response response = builder.delete();

			String result = response.readEntity(String.class);

			if (response.getStatus() != 204) {
				System.out
						.println("Storage Service Error response.getStatus() != 204");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			} else {
				return Response.ok(result).build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 204");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * GET /business-service/person/{idPerson}/current-health This method calls
	 * a getPerson method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}/current-health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readCurrentHealthDetails(@PathParam("pid") int idPerson) {
		try {
			System.out
					.println("readCurrentHealthDetails: Reading list of all current measures for a person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson;
			String xmlResponse = null;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject obj = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {

				xmlResponse = "<currentHealth-profile>";
				JSONObject currentObj = (JSONObject) obj.get("currentHealth");

				JSONArray measureArr = currentObj.getJSONArray("measure");
				for (int j = 0; j < measureArr.length(); j++) {
					
					String sMeasure = measureArr.getJSONObject(j).getString("name");
					String tMeasure = sMeasure.replaceAll(" ", "-");
					
					// measure array json
					xmlResponse += "<" + tMeasure + ">";
					xmlResponse += "<id>"
							+ measureArr.getJSONObject(j).get("mid") + "</id>";
					/*xmlResponse += "<name>"
							+ measureArr.getJSONObject(j).get("name")
							+ "</name>";*/
					xmlResponse += "<value>"
							+ measureArr.getJSONObject(j).get("value")
							+ "</value>";
					xmlResponse += "<created>"
							+ measureArr.getJSONObject(j).get("created")
							+ "</created>";
					xmlResponse += "</" + tMeasure + ">";
				}
				xmlResponse += "</currentHealth-profile>";

				System.out.println(prettyXMLPrint(xmlResponse));

				JSONObject xmlJSONObj = XML.toJSONObject(xmlResponse);
				String jsonPrettyPrintString = xmlJSONObj.toString(4);
				return Response.ok(jsonPrettyPrintString).build();

			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * GET /business-service/person/{idPerson}/history-health This method calls
	 * a getHistoryHealth method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}/history-health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readHistoryHealthDetails(@PathParam("pid") int idPerson) {
		try {
			System.out
					.println("readHistoryHealthDetails: Reading list of all history measures for a person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson + "/historyHealth";
			String xmlResponse = null;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject obj = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {

				xmlResponse = "<historyHealth-profile>";

				JSONArray measureArr = (JSONArray) obj.getJSONArray("measure");
				for (int j = 0; j < measureArr.length(); j++) {

					String sMeasure = measureArr.getJSONObject(j).getString("name");
					String tMeasure = sMeasure.replaceAll(" ", "-");
					
					// measure array json
					xmlResponse += "<" + tMeasure + ">";
					xmlResponse += "<id>"
							+ measureArr.getJSONObject(j).get("mid") + "</id>";
					/*xmlResponse += "<name>"
							+ measureArr.getJSONObject(j).get("name")
							+ "</name>";*/
					xmlResponse += "<value>"
							+ measureArr.getJSONObject(j).get("value")
							+ "</value>";
					xmlResponse += "<created>"
							+ measureArr.getJSONObject(j).get("created")
							+ "</created>";
					xmlResponse += "</" + tMeasure + ">";
				}

				xmlResponse += "</historyHealth-profile>";

				System.out.println(prettyXMLPrint(xmlResponse));

				JSONObject xmlJSONObj = XML.toJSONObject(xmlResponse);
				String jsonPrettyPrintString = xmlJSONObj.toString(4);
				return Response.ok(jsonPrettyPrintString).build();

			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	// ******************* MEASURE ***********************

	/**
	 * GET /business-service/person/{idPerson}/measure/{measureName} This
	 * method calls a getMeasure method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}/measure/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readMeasureDetails(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) {
		try {
			System.out
					.println("readMeasureHealthDetails: Reading list of all "
							+ measureName
							+ " for a person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson + "/measure/" + measureName;
			String xmlResponse = null;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject obj = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {

				xmlResponse = "<measure-profile>";

				JSONArray measureArr = (JSONArray) obj.getJSONArray("measure");
				for (int j = 0; j < measureArr.length(); j++) {

					// measure array json
					xmlResponse += "<" + measureName + ">";
					xmlResponse += "<mid>"
							+ measureArr.getJSONObject(j).get("mid") + "</mid>";
					xmlResponse += "<name>"
							+ measureArr.getJSONObject(j).get("name")
							+ "</name>";
					xmlResponse += "<value>"
							+ measureArr.getJSONObject(j).get("value")
							+ "</value>";
					xmlResponse += "<created>"
							+ measureArr.getJSONObject(j).get("created")
							+ "</created>";
					xmlResponse += "</" + measureName + ">";
				}

				xmlResponse += "</measure-profile>";

				System.out.println(prettyXMLPrint(xmlResponse));

				JSONObject xmlJSONObj = XML.toJSONObject(xmlResponse);
				String jsonPrettyPrintString = xmlJSONObj.toString(4);
				return Response.ok(jsonPrettyPrintString).build();
			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
			}
		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	// ******************* GOAL ***********************

	/**
	 * GET /business-service/person/{idPerson}/goal This method calls a
	 * getGoalList method in Storage Services Module
	 * 
	 * @return
	 */
	@GET
	@Path("{pid}/goal")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response readGoalListDetails(@PathParam("pid") int idPerson) {
		try {
			System.out
					.println("readGoalListDetails: Reading list of all goals for Person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson + "/goal";

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject o = new JSONObject(result.toString());

			if (response.getStatusLine().getStatusCode() == 200) {
				return Response.ok(o.toString()).build();

			} else {
				System.out
						.println("Storage Service Error response.getStatus() != 200");
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity(externalErrorMessage(response.toString()))
						.build();
				// return Response.status(204).build();
			}

		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	/**
	 * GET /business-service/person/{idPerson}/comparison-value/{measureName} 
	 * This method calls a getPerson method in Storage Services Module
	 * @param idPerson
	 * @param measureName
	 * @return
	 */
	@GET
	@Path("{pid}/comparison-value/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response comparisonValueOfMeasure(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) {
		try {
			System.out
					.println("comparisonValueOfMeasure: Getting the result about the comparison of the value's "
							+ measureName
							+ " between goal and currentHealth for Person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson;

			String xmlBuild = "";
			String comparison = "";

			double currentMeasureValueDouble = -1.;
			double goalValueDouble = -1.;

			int currentMeasureValueInt = -1;
			int goalValueInt = -1;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			// person json obj
			JSONObject obj = new JSONObject(result.toString());

			// GETTING CURRENT MEASURE LIST FOR A SPECIFIED PERSON
			// currentHealth json obj - measure json array
			JSONArray measureArr = (JSONArray) obj.getJSONObject(
					"currentHealth").getJSONArray("measure");
			for (int i = 0; i < measureArr.length(); i++) {
				if ((measureArr.getJSONObject(i).getString("name"))
						.equals(measureName)) {
					String target = measureArr.getJSONObject(i).getString(
							"name");
					System.out.println("measureName : " + target);
					if (target.equals("heart rate") || target.equals("steps")) {
						currentMeasureValueInt = measureArr.getJSONObject(i)
								.getInt("value");
						System.out.println("measure-value: "
								+ currentMeasureValueInt);
					} else {
						currentMeasureValueDouble = measureArr.getJSONObject(i)
								.getDouble("value");
						System.out.println("measure-value: "
								+ currentMeasureValueDouble);
					}
				}
			}

			// GETTING GOAL LIST FOR A SPECIFIED PERSON
			// goals json obj - goal json array
			JSONArray goalArr = (JSONArray) obj.getJSONObject("goals")
					.getJSONArray("goal");
			for (int i = 0; i < goalArr.length(); i++) {
				if ((goalArr.getJSONObject(i).getString("type"))
						.equals(measureName)) {
					String target = goalArr.getJSONObject(i).getString("type");
					System.out.println("goalType : " + target);
					if (target.equals("heart rate") || target.equals("steps")) {
						goalValueInt = goalArr.getJSONObject(i).getInt("value");
						System.out.println("goal-value: " + goalValueInt);
					} else {
						goalValueDouble = goalArr.getJSONObject(i).getDouble(
								"value");
						System.out.println("goal-value: " + goalValueDouble);
					}
				}
			}

			// COMPARISON
			switch (measureName) {
			case "heart rate":
				if (currentMeasureValueInt == -1 || goalValueInt == -1) {
					return Response.status(404).build();
				}
				if (currentMeasureValueInt >= goalValueInt) {
					comparison = "ok";
				} else {
					comparison = "ko";
				}
				break;
			case "steps":
				if (currentMeasureValueInt == -1 || goalValueInt == -1) {
					return Response.status(404).build();
				}
				if (currentMeasureValueInt >= goalValueInt) {
					comparison = "ok";
				} else {
					comparison = "ko";
				}
				break;
			default:
				if (currentMeasureValueDouble == -1. || goalValueDouble == -1.) {
					return Response.status(404).build();
				}
				if (currentMeasureValueDouble >= goalValueDouble) {
					comparison = "ok";
				} else {
					comparison = "ko";
				}
			}
			System.out.println("comparison: " + comparison);

			xmlBuild = "<comparison-information>";
			xmlBuild += "<measure>" + measureName + "</measure>";

			if (measureName.equals("heart rate") || measureName.equals("steps")) {
				xmlBuild += "<currentMeasureValue>" + currentMeasureValueInt
						+ "</currentMeasureValue>";
				xmlBuild += "<goalValue>" + goalValueInt + "</goalValue>";
			} else {
				xmlBuild += "<currentMeasureValue>" + currentMeasureValueDouble
						+ "</currentMeasureValue>";
				xmlBuild += "<goalValue>" + goalValueDouble + "</goalValue>";
			}

			xmlBuild += "<result>" + comparison + "</result>";
			xmlBuild += "</comparison-information>";

			System.out.println(prettyXMLPrint(xmlBuild));

			JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
			String jsonPrettyPrintString = xmlJSONObj.toString(4);

			return Response.ok(jsonPrettyPrintString).build();

		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * GET /business-service/person/{idPerson}/motivational-goal/{measureName} 
	 * This method calls a getPerson method in Storage Services Module
	 * @param idPerson
	 * @param measureName
	 * @return
	 */
	@GET
	@Path("{pid}/motivation-goal/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response readMotivationGoal(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) {
		try {
			System.out
					.println("motivationGoal: Checking goal for "
							+ measureName
							+ ", for a specified Person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson;
			String xmlBuild = "";
			Random generator = new Random();
			int randIndex = 0;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject motivationGoalObj = new JSONObject(
					createJSONMotivationGoal());
			// System.out.println("lenght: "
			// + motivationGoalObj.getJSONArray("motivation-goal")
			// .length());

			JSONObject obj = new JSONObject(result.toString());

			xmlBuild = "<motivationGoal>";
			xmlBuild += "<person>" + obj.getString("lastname") + " "
					+ obj.getString("firstname") + "</person>";

			JSONArray goalArr = (JSONArray) obj.getJSONObject("goals")
					.getJSONArray("goal");

			JSONObject goal = null;

			for (int i = 0; i < goalArr.length(); i++) {
				if (goalArr.getJSONObject(i).getString("type")
						.equals(measureName)) {
					goal = goalArr.getJSONObject(i);
				}
			}

			if (goal == null) {
				xmlBuild += "<goal>" + "don't exist goal for " + measureName + "</goal>";
			} else {
				randIndex = generator.nextInt(motivationGoalObj.getJSONArray(
						"motivation-goal").length());
				System.out.println("index: " + randIndex);

				xmlBuild += "<goal>";
				xmlBuild += "<measure>" + goal.getString("type") + "</measure>";
				xmlBuild += "<value>" + goal.getString("value") + "</value>";
				xmlBuild += "<motivation>"
						+ motivationGoalObj.getJSONArray("motivation-goal")
								.getJSONObject(randIndex)
								.getString("motivation") + "</motivation>";
				xmlBuild += "<author>"
						+ motivationGoalObj.getJSONArray("motivation-goal")
								.getJSONObject(randIndex).getString("author")
						+ "</author>";
				xmlBuild += "</goal>";

			}

			xmlBuild += "</motivationGoal>";

			System.out.println(prettyXMLPrint(xmlBuild));

			JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
			String jsonPrettyPrintString = xmlJSONObj.toString(4);

			return Response.ok(jsonPrettyPrintString).build();

		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}
	}

	/**
	 * GET /business-service/person/{idPerson}/motivation-health/{measureName} 
	 * This method calls a getPerson method in Storage Services Module
	 * @param idPerson
	 * @param measureName
	 * @return
	 */
	@GET
	@Path("{pid}/motivation-health/{measureName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response readMotivationHealth(@PathParam("pid") int idPerson,
			@PathParam("measureName") String measureName) {
		try {
			System.out
					.println("readMotivationHealth: Checking measure for "
							+ measureName
							+ ", for a specified Person with "
							+ idPerson
							+ " from Storage Services Module in Business Logic Services...");

			String path = "/person/" + idPerson;
			String xmlBuild = "";
			Random generator = new Random();
			int randIndex = 0;

			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(storageServiceURL + path);
			HttpResponse response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			JSONObject motivationHealthObj = new JSONObject(
					createJSONMotivationHealth());

			JSONObject obj = new JSONObject(result.toString());

			xmlBuild = "<motivationHealth>";
			xmlBuild += "<person>" + obj.getString("lastname") + " "
					+ obj.getString("firstname") + "</person>";

			JSONArray measureArr = (JSONArray) obj.getJSONObject(
					"currentHealth").getJSONArray("measure");

			JSONObject measure = null;

			for (int i = 0; i < measureArr.length(); i++) {
				if (measureArr.getJSONObject(i).getString("name")
						.equals(measureName)) {
					measure = measureArr.getJSONObject(i);
				}
			}
			if (measure == null) {
				xmlBuild += "<measure>" + measureName + " don't exist"
						+ "</measure>";
			} else {
				randIndex = generator.nextInt(motivationHealthObj.getJSONArray(
						"motivation-health").length());
				System.out.println("index: " + randIndex);

				xmlBuild += "<measure>";
				xmlBuild += "<name>" + measure.getString("name") + "</name>";
				xmlBuild += "<value>" + measure.getString("value") + "</value>";
				xmlBuild += "<motivation>"
						+ motivationHealthObj.getJSONArray("motivation-health")
								.getJSONObject(randIndex)
								.getString("motivation") + "</motivation>";
				xmlBuild += "<author>"
						+ motivationHealthObj.getJSONArray("motivation-health")
								.getJSONObject(randIndex).getString("author")
						+ "</author>";
				xmlBuild += "</measure>";

			}

			xmlBuild += "</motivationHealth>";

			System.out.println(prettyXMLPrint(xmlBuild));

			JSONObject xmlJSONObj = XML.toJSONObject(xmlBuild);
			String jsonPrettyPrintString = xmlJSONObj.toString(4);

			return Response.ok(jsonPrettyPrintString).build();

		} catch (Exception e) {
			System.out
					.println("Business Logic Service Error catch response.getStatus() != 200");
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(errorMessage(e)).build();
		}

	}

	public static String createJSONMotivationHealth() {
		// create string array with motivation health - exercise - activity and
		// his author
		String[] motivationsHealth = {
				"Our health always seems much more valuable after we lose it.",
				"A man’s health can be judged by which he takes two at a time – pills or stairs.",
				"Living a healthy lifestyle will only deprive you of poor health, lethargy, and fat.",
				"Health is a state of complete physical, mental and social well-being, and not merely the absence of disease or infirmity.",
				"Those who think they have not time for bodily exercise will sooner or later have to find time for illness.",
				"Movement is a medicine for creating change in a person’s physical, emotional, and mental states.",
				"If it weren’t for the fact that the TV set and the refrigerator are so far apart, some of us wouldn’t get any exercise at all.",
				"Too many people confine their exercise to jumping to conclusions, running up bills, stretching the truth, bending over backwards, lying down on the job, sidestepping responsibility and pushing their luck.",
				"Fitness – If it came in a bottle, everybody would have a great body.",
				"Walking is the best possible exercise. Habituate yourself to walk very far.",
				"Walking: the most ancient exercise and still the best modern exercise." };

		String[] authors = { "Unknown", "Joan Welsh", "Jill Johnson",
				"World Health Organization", "Edward Stanley", "Carol Welch",
				"Joey Adams", "Anonymous", "Cher", "Thomas Jefferson",
				"Carrie Latet" };

		// create motivation quotes json obj
		JSONObject objInner;
		JSONArray arr = new JSONArray();
		for (int i = 0; i < motivationsHealth.length; i++) {
			objInner = new JSONObject();
			objInner.put("motivation", motivationsHealth[i]);
			objInner.put("author", authors[i]);
			arr.put(objInner);
		}
		JSONObject obj = new JSONObject();
		obj.put("motivation-health", arr);
		return obj.toString();
	}

	public static String createJSONMotivationGoal() {
		// create string array with motivation goal and his author
		String[] motivationsGoal = {
				"A goal without a plan is just a wish.",
				"Nothing can stop the man with the right mental attitude from achieving his goal; nothing on earth can help the man with the wrong mental attitude.",
				"If you want to accomplish anything in life, you can’t just sit back and hope it will happen. You’ve got to make it happen.",
				"You must have long-range goals to keep you from being frustrated by short-range failures.",
				"If what you are doing is not moving you towards your goals, then it’s moving you away from your goals.",
				"People with clear, written goals, accomplish far more in a shorter period of time than people without them could ever imagine.",
				"You cannot expect to achieve new goals or move beyond your present circumstances unless you change.",
				"This one step – choosing a goal and sticking to it – changes everything.",
				"Your goals, minus your doubts, equal your reality." };

		String[] authors = { "Larry Elder", "Thomas Jefferson", "Chuck Norris",
				"Charles C. Noble", "Brian Tracy", "Brian Tracy", "Les Brown",
				"Scott Reed", "Ralph Marston" };

		// create motivation quotes json obj
		JSONObject objInner;
		JSONArray arr = new JSONArray();
		for (int i = 0; i < motivationsGoal.length; i++) {
			objInner = new JSONObject();
			objInner.put("motivation", motivationsGoal[i]);
			objInner.put("author", authors[i]);
			arr.put(objInner);
		}
		JSONObject obj = new JSONObject();
		obj.put("motivation-goal", arr);
		return obj.toString();
	}

	/**
	 * Prints pretty format for XML
	 * 
	 * @param xml
	 * @return
	 * @throws TransformerException
	 */
	public String prettyXMLPrint(String xmlString) throws TransformerException {

		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(
				"{http://xml.apache.org/xslt}indent-amount", "4");

		Source xmlInput = new StreamSource(new StringReader(xmlString));
		StringWriter stringWriter = new StringWriter();
		StreamResult xmlOutput = new StreamResult(stringWriter);

		transformer.transform(xmlInput, xmlOutput);
		return xmlOutput.getWriter().toString();
	}

}