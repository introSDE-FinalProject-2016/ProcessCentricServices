package introsde.finalproject.rest.processcentricservices.resources;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

@Stateless
@LocalBean
@Path("/processCentric-service")
public class CollectionResources {

	
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

	@Path("/person")
	public PersonResource routePerson() {
		return new PersonResource(uriInfo, request);
	}
	
	@Path("/measureTypes")
	public MeasureDefinitionResource routeMeasureDefinition() {
		return new MeasureDefinitionResource(uriInfo, request);
	}
	
}
