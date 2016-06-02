package introsde.finalproject.rest.processcentricservices.util;

public class UrlInfo {
	
	public UrlInfo() {}
	
	//static final String businessLogicUrl = "http://127.0.1.1:5700/sdelab/businessLogic-service";
	static final String businessLogicUrl = "https://fierce-sea-36005.herokuapp.com/sdelab/businessLogic-service";
	
	//static final String storageUrl = "http://127.0.1.1:5701/sdelab/storage-service";
	static final String storageUrl = "https://warm-hamlet-95336.herokuapp.com/sdelab/storage-service";
	
	//static final String adapterUrl = "http://127.0.1.1:5702/sdelab/adapter-service";
	static final String adapterUrl = "https://stark-island-39603.herokuapp.com/sdelab/adapter-service";
	
	static final String processCentricUrl = "http://127.0.1.1:5703/sdelab/processCentric-service";
	
	/**
	 * This method is used to get the business logic url
	 * @return
	 */
	public String getBusinesslogicURL() {
		return businessLogicUrl;
	}
	
	
	/**
	 * This method is used to get the storage url
	 * @return
	 */
	public String getStorageURL() {
		return storageUrl;
	}
}
