package com.beyondprops;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;

import com.beyondprops.BeyondProps.Application;
import com.beyondprops.BeyondProps.Config;
import com.beyondprops.BeyondProps.Lookup;
import com.beyondprops.BeyondProps.ProjectBranch;
import com.beyondprops.BeyondProps.Property;
import com.beyondprops.BeyondProps.PropertyObject;
import com.beyondprops.BeyondProps.PropertyValue;
import com.beyondprops.BeyondProps.ValueTypes;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class BeyondPropsClient {

	static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"); 

	//Constructors

	public BeyondPropsClient() {
		config = new BeyondProps().new Config();
	}

	public BeyondPropsClient(String apiKey) {
		this.apiKey = apiKey;
		config = new BeyondProps().new Config();
	}


	//Collections inner class

	public static class Collections {

		private static String collEndPoint;
		static {
			if (mode.equals("dev")) {
				collEndPoint = COLLECTION_API_ENDPOINT_DEV;
			} else {
				collEndPoint = COLLECTION_API_ENDPOINT_PROD;
			}
		}

		public static class Applications {

			public static String add (List<Application> apps) throws BeyondPropsException {
				return executeCollectionService(apps, false, false);
			}

			public static String add (Application app) throws BeyondPropsException {
				List<Application> temp = new ArrayList<Application>();
				temp.add(app);
				return executeCollectionService(temp, false, false);
			}

			public static boolean update (List<Application> apps) throws BeyondPropsException {
				executeCollectionService(apps, true, false);
				return true;
			}

			public static boolean update (Application app) throws BeyondPropsException {
				List<Application> temp = new ArrayList<Application>();
				temp.add(app);
				executeCollectionService(temp, true, false);
				return true;
			}

			public static boolean delete (List<Application> apps) throws BeyondPropsException {
				executeCollectionService(apps, false, true);
				return true;
			}

			public static boolean delete (Application app) throws BeyondPropsException {
				List<Application> temp = new ArrayList<Application>();
				temp.add(app);
				executeCollectionService(temp, false, true);
				return true;
			}

			public static Application get(String appName) throws BeyondPropsException {
				return get(new String[] { appName }).get(0);
			}
			
			public static List<Application> get(String[] appNames) throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				List<Application> retValue = new LinkedList<Application>();
				try {

					JSONArray data = new JSONArray();
					for (String appName : appNames) {
						JSONObject dataObj = new JSONObject();
						dataObj.put("name", appName);
						data.put(dataObj);
					}
					JSONObject req = new JSONObject();
					req.put("collection", "application")
					.put("authToken", getAuthToken())
					.put("data", data);

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray dataArr = response.getJSONArray("data");

						Iterator<Object> itr = dataArr.iterator();
						while (itr.hasNext()) {
							JSONObject appObj = ((JSONObject) itr.next()).getJSONObject("map");
							retValue.add(new BeyondProps().new Application(
									appObj.getString("name"),
									appObj.getString("desc"),
									appObj.getString("tags"),
									appObj.getString("createdDate"),
									appObj.has("lastUpdatedBy") ? appObj.getString("lastUpdatedBy") : "",
											appObj.getString("lastUpdatedDate")
									));
						}
						return retValue;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			public static List<Application> list() throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {
					JSONObject req = new JSONObject();
					req.put("collection", "application")
					.put("authToken", getAuthToken());

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray("data");

						Iterator<Object> itr = arr.iterator();
						List<Application> list = new LinkedList<Application>();
						while(itr.hasNext()) {
							JSONObject appObj = (JSONObject) itr.next();

							Application app = new BeyondProps().new Application(
									appObj.getString("name"),
									appObj.getString("desc"),
									appObj.getString("tags"),
									appObj.getString("createdDate"),
									appObj.has("lastUpdatedBy") ? appObj.getString("lastUpdatedBy") : "",
											appObj.getString("lastUpdatedDate")
									);
							list.add(app);
						}
						return list;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			private static String executeCollectionService(List<Application> apps, boolean isUpdate, boolean isDelete) throws BeyondPropsException {

				JSONObject req = new JSONObject();
				JSONArray data = new JSONArray();

				for (Application app : apps) {
					JSONObject dataObj = new JSONObject();
					dataObj.put("name", app.getName())
					.put("desc", app.getDesc())
					.put("tags", app.getTags());
					data.put(dataObj);
				}

				req.put("collection", "application")
				.put("data", data)
				.put("authToken", getAuthToken());

				if (isUpdate) {
					req.put("operation", "update");
				}
				if (isDelete) {
					req.put("operation", "delete");
				}

				boolean newApp = false;

				if (!isUpdate && !isDelete) {
					newApp = true;
				}
				HttpResponse<JsonNode> jsonResponse;
				try {
					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (newApp && response.getInt("statusCode") == 0) {
						return "success";
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException("Error: " + response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}
		}

		public static class ProjectBranches {

			public static boolean add (List<ProjectBranch> branches) throws BeyondPropsException {
				executeCollectionService(branches, false, false);
				return true;
			}

			public static boolean update (List<ProjectBranch> branches) throws BeyondPropsException {
				executeCollectionService(branches, true, false);
				return true;
			}

			public static boolean delete (List<ProjectBranch> branches) throws BeyondPropsException {
				executeCollectionService(branches, false, true);
				return true;
			}

			public static boolean add (ProjectBranch branch) throws BeyondPropsException {
				List<ProjectBranch> temp = new ArrayList<ProjectBranch>();
				temp.add(branch);
				executeCollectionService(temp, false, false);
				return true;
			}

			public static boolean update (ProjectBranch branch) throws BeyondPropsException {
				List<ProjectBranch> temp = new ArrayList<ProjectBranch>();
				temp.add(branch);
				executeCollectionService(temp, true, false);
				return true;
			}

			public static boolean delete (ProjectBranch branch) throws BeyondPropsException {
				List<ProjectBranch> temp = new ArrayList<ProjectBranch>();
				temp.add(branch);
				executeCollectionService(temp, false, true);
				return true;
			}

			public static ProjectBranch get(String branchName) throws BeyondPropsException {
				return get(new String[] { branchName }).get(0);
			}
			public static List<ProjectBranch> get(String[] branchNames) throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				List<ProjectBranch> retValue = new LinkedList<ProjectBranch>();
				try {

					JSONArray data = new JSONArray();
					for (String branchName : branchNames) {
						JSONObject dataObj = new JSONObject();
						dataObj.put("name", branchName);
						data.put(dataObj);
					}
					JSONObject req = new JSONObject();
					req.put("collection", "branch")
					.put("authToken", getAuthToken())
					.put("data", data);

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray dataArr = response.getJSONArray("data");

						Iterator<Object> itr = dataArr.iterator();
						while (itr.hasNext()) {
							JSONObject branchObj = (JSONObject) itr.next();
							retValue.add(new BeyondProps().new ProjectBranch(
									branchObj.getString("name"),
									branchObj.getString("desc"),
									branchObj.getString("tags"),
									branchObj.getString("createdDate"),
									branchObj.has("lastUpdatedBy") ? branchObj.getString("lastUpdatedBy") : "",
											branchObj.getString("lastUpdatedDate"),
											branchObj.getBoolean("enabled")
									));
						}
						return retValue;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			public static List<ProjectBranch> list() throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {
					JSONObject req = new JSONObject();
					req.put("collection", "branch")
					.put("authToken", getAuthToken());

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray("data");

						Iterator<Object> itr = arr.iterator();
						List<ProjectBranch> list = new LinkedList<ProjectBranch>();
						while(itr.hasNext()) {
							JSONObject obj = (JSONObject) itr.next();

							ProjectBranch branch = new BeyondProps().new ProjectBranch(
									obj.getString("name"),
									obj.getString("desc"),
									obj.getString("tags"),
									obj.getString("createdDate"),
									obj.has("lastUpdatedBy") ? obj.getString("lastUpdatedBy") : "",
											obj.getString("lastUpdatedDate"),
											obj.getBoolean("enabled")
									);
							list.add(branch);
						}
						return list;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			private static String executeCollectionService(List<ProjectBranch> branches, boolean isUpdate, boolean isDelete) throws BeyondPropsException {

				JSONObject req = new JSONObject();

				JSONArray data = new JSONArray();

				for (ProjectBranch branch : branches) {
					JSONObject dataObj = new JSONObject();
					dataObj.put("name", branch.getName())
					.put("desc", branch.getDesc())
					.put("tags", branch.getTags())
					.put("enabled", branch.enabled);
					data.put(dataObj);
				}

				req.put("collection", "projectBranch")
				.put("data", data)
				.put("authToken", getAuthToken());

				if (isUpdate) {
					req.put("operation", "update");
				}
				if (isDelete) {
					req.put("operation", "delete");
				}

				HttpResponse<JsonNode> jsonResponse;
				try {
					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {
						return "success";
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}
		}

		public static class LookupHierarchy {

			public static boolean add (List<Lookup> lookups) throws BeyondPropsException {
				executeCollectionService(lookups, false, false);
				return true;
			}

			public static boolean update (List<Lookup> lookups) throws BeyondPropsException {
				executeCollectionService(lookups, true, false);
				return true;
			}

			public static boolean delete (List<Lookup> lookups) throws BeyondPropsException {
				executeCollectionService(lookups, false, true);
				return true;
			}

			public static boolean add (Lookup lookup) throws BeyondPropsException {
				List<Lookup> temp = new ArrayList<Lookup>();
				temp.add(lookup);
				executeCollectionService(temp, false, false);
				return true;
			}

			public static boolean update (Lookup lookup) throws BeyondPropsException {
				List<Lookup> temp = new ArrayList<Lookup>();
				temp.add(lookup);
				executeCollectionService(temp, true, false);
				return true;
			}

			public static boolean delete (Lookup lookup) throws BeyondPropsException {
				List<Lookup> temp = new ArrayList<Lookup>();
				temp.add(lookup);
				executeCollectionService(temp, false, true);
				return true;
			}

			public static Lookup get(String lookupName) throws BeyondPropsException {
				return get(new String[] { lookupName }).get(0);
			}


			public static List<Lookup> get(String[] lookupNames) throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {

					JSONArray data = new JSONArray();
					for (String lookupName : lookupNames) {
						JSONObject dataObj = new JSONObject();
						dataObj.put("name", lookupName);
						data.put(dataObj);
					}

					JSONObject req = new JSONObject();
					req.put("collection", "lookup")
					.put("authToken", getAuthToken())
					.put("data", data);

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray("data");

						Iterator<Object> itr = arr.iterator();
						List<Lookup> list = new LinkedList<Lookup>();
						while(itr.hasNext()) {
							JSONObject obj = (JSONObject) itr.next();

							list.add(new BeyondProps().new Lookup(
									obj.getString("name"),
									obj.getString("desc"),
									obj.getString("tags"),
									obj.getString("createdDate"),
									obj.has("lastUpdatedBy") ? obj.getString("lastUpdatedBy") : "",
											obj.getString("lastUpdatedDate"),
											obj.getInt("order"),
											obj.getString("values").split(",")
									));
						}
						return list;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			public static List<Lookup> list() throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {
					JSONObject req = new JSONObject();
					req.put("collection", "lookup")
					.put("authToken", getAuthToken());

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray("data");

						Iterator<Object> itr = arr.iterator();
						List<Lookup> list = new LinkedList<Lookup>();
						while(itr.hasNext()) {
							JSONObject obj = (JSONObject) itr.next();

							Lookup lookup = new BeyondProps().new Lookup(
									obj.getString("name"),
									obj.getString("desc"),
									obj.getString("tags"),
									obj.getString("createdDate"),
									obj.has("lastUpdatedBy") ? obj.getString("lastUpdatedBy") : "",
											obj.getString("lastUpdatedDate"),
											obj.getInt("order"),
											obj.getString("values").split(",")
									);
							list.add(lookup);
						}
						return list;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			private static String executeCollectionService(List<Lookup> lookups, boolean isUpdate, boolean isDelete) throws BeyondPropsException {

				JSONObject req = new JSONObject();

				JSONArray data = new JSONArray();

				for (Lookup lookup : lookups) {
					JSONObject dataObj = new JSONObject();
					dataObj.put("name", lookup.getName())
					.put("desc", lookup.getDesc())
					.put("tags", lookup.getTags())
					.put("order", lookup.order)
					.put("values", lookup.values);
					data.put(dataObj);
				}

				req.put("collection", "config")
				.put("data", data)
				.put("authToken", getAuthToken());

				if (isUpdate) {
					req.put("operation", "update");
				}
				if (isDelete) {
					req.put("operation", "delete");
				}

				HttpResponse<JsonNode> jsonResponse;
				try {
					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {
						return "success";
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}
		}

		public static class Properties {

			public static boolean add (List<Property> properties) throws BeyondPropsException {
				executeCollectionService(properties, false, false);
				return true;
			}

			public static boolean update (List<Property> properties) throws BeyondPropsException {
				executeCollectionService(properties, true, false);
				return true;
			}

			public static boolean delete (List<Property> properties) throws BeyondPropsException {
				executeCollectionService(properties, false, true);
				return true;
			}

			public static boolean add (Property property) throws BeyondPropsException {
				List<Property> temp = new ArrayList<Property>();
				temp.add(property);
				executeCollectionService(temp, false, false);
				return true;
			}

			public static boolean update (Property property) throws BeyondPropsException {
				List<Property> temp = new ArrayList<Property>();
				temp.add(property);
				executeCollectionService(temp, true, false);
				return true;
			}

			public static boolean delete (Property property) throws BeyondPropsException {
				List<Property> temp = new ArrayList<Property>();
				temp.add(property);
				executeCollectionService(temp, false, true);
				return true;
			}

			public static Property get(String propertyName) throws BeyondPropsException {
				return get(new String[] { propertyName }).get(0);
			}

			public static List<Property> get(String[] propertyNames) throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {

					JSONArray data = new JSONArray();

					for (String propertyName : propertyNames) {
						JSONObject dataObj = new JSONObject();
						dataObj.put("name", propertyName);
						data.put(dataObj);
					}

					JSONObject req = new JSONObject();
					req.put("collection", "property")
					.put("authToken", getAuthToken())
					.put("data", data);

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray ("data");
						Iterator<Object> itr = arr.iterator();
						List<Property> list = new LinkedList<Property>();
						while(itr.hasNext()) {
							JSONObject obj = (JSONObject) itr.next();

							list.add(new BeyondProps().new Property(
									obj.getString("name"),
									obj.getString("desc"),
									obj.getString("tags"),
									obj.getString("availability"),
									obj.getInt("type"),
									obj.has("defaultValue") ? obj.getString("defaultValue") : "",
											obj.getString("createdDate"),
											obj.has("lastUpdatedBy") ? obj.getString("lastUpdatedBy") : "",
													obj.getString("lastUpdatedDate")
									));
						}
						return list;

					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			public static List<Property> list() throws BeyondPropsException {
				HttpResponse<JsonNode> jsonResponse;
				try {
					JSONObject req = new JSONObject();
					req.put("collection", "property")
					.put("authToken", getAuthToken());

					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {

						JSONArray arr = response.getJSONArray("data");

						Iterator<Object> itr = arr.iterator();
						List<Property> list = new LinkedList<Property>();
						while(itr.hasNext()) {
							JSONObject obj = (JSONObject) itr.next();

							Property property = new BeyondProps().new Property(
									obj.getString("name"),
									obj.getString("desc"),
									obj.getString("tags"),
									obj.getString("availability"),
									obj.getInt("type"),
									obj.has("defaultValue") ? obj.getString("defaultValue") : "",
											obj.getString("createdDate"),
											obj.has("lastUpdatedBy") ? obj.getString("lastUpdatedBy") : "",
													obj.getString("lastUpdatedDate")
									);
							list.add(property);
						}
						return list;
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}

			private static String executeCollectionService(List<Property> properties, boolean isUpdate, boolean isDelete) throws BeyondPropsException {

				JSONObject req = new JSONObject();

				JSONArray data = new JSONArray();

				for (Property property : properties) {
					JSONObject dataObj = new JSONObject();
					dataObj.put("name", property.getName())
					.put("desc", property.getDesc())
					.put("tags", property.getTags())
					.put("enabled", property.getAvailability())
					.put("type", property.getType().getValueType());

					if (property.getDefaultValue() != null) {
						dataObj.put("defaultValue", property.getDefaultValue());
					}

					data.put(dataObj);
				}

				req.put("collection", "property")
				.put("data", data)
				.put("authToken", getAuthToken());

				if (isUpdate) {
					req.put("operation", "update");
				}
				if (isDelete) {
					req.put("operation", "delete");
				}

				HttpResponse<JsonNode> jsonResponse;
				try {
					jsonResponse = Unirest.post(collEndPoint)
							.header("accept", "application/json")
							.body(req.toString())
							.asJson();

					JSONObject response = jsonResponse.getBody().getObject();

					if (response.getInt("statusCode") == 0) {
						return "success";
					} else {
						System.out.println(response.getJSONArray("errors"));
						throw new BeyondPropsException(response.getJSONArray("errors").join(","));
					}
				} catch (Exception e) {
					e.printStackTrace();
					throw new BeyondPropsException(e.getMessage());
				}
			}
		}
	}

	public BeyondPropsClient clearLocalPropertiesCache() {
		localPropertiesCache.clear();
		return this;
	}
	
	public BeyondPropsClient setProperty(String key, Object val) {
		if (localPropertiesCache == null) {
			localPropertiesCache = new LinkedHashMap<String, PropertyObject>();
		}
		
		if (val instanceof Date) {
			val = formatter.format((Date)val);
		}
		
		localPropertiesCache.put(key, new PropertyObject(new PropertyValue(val)));
		return this;
	}

	
	public PropertyObject getLocalProperty(String key) {
		if (localPropertiesCache == null) {
			return null;
		}
		return localPropertiesCache.get(key);
	}

	public Map<String, PropertyObject> getLocalProperties() {
		return localPropertiesCache;
	}

	public BeyondPropsClient setProperties(Map<String, PropertyObject> properties) {
		this.localPropertiesCache = properties;
		return this;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public String getMode() {
		return mode;
	}

	public BeyondPropsClient setMode(String mode1) {
		mode = mode1;
		return this;
	}

	public String getApiKey() {
		return apiKey;
	}

	public BeyondPropsClient setApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	public String getApplication() {
		if (config!=null)
			return config.getApplication();
		else
			return null;
	}

	public BeyondPropsClient setApplication(String application) {
		if (config == null) {
			config = new BeyondProps().new Config();
		}

		config.setApplication(application);
		return this;
	}

	public String getProjectBranch() {
		if (config!=null)
			return config.getProjectBranch();
		else
			return null;
	}

	public BeyondPropsClient setProjectBranch(String projectBranch) {
		if (config == null) {
			config = new BeyondProps().new Config();
		}

		config.setProjectBranch(projectBranch);
		return this;
	}

	public Map<String, String> getLookupMap() {
		if (config!=null)
			return config.getLookup();
		else
			return null;
	}

	public BeyondPropsClient setLookup(Map<String, String> lookup) {
		if (config == null) {
			config = new BeyondProps().new Config();
		}

		config.setLookup(lookup);
		return this;
	}

	public String getLookup(String key) {
		if (config!=null)
			return config.getLookup(key);
		else
			return null;
	}

	public BeyondPropsClient setLookup (String key, String val) {
		if (config == null) {
			config = new BeyondProps().new Config();
		}

		config.setLookup(key, val);
		return this;
	}

	public static String getAuthToken() {
		return authToken;
	}

	public static void setAuthToken(String authToken1) {
		authToken = authToken1;
	}

	public Date getAuthTokenExpirationTime() {
		return authTokenExpirationTime;
	}

	public void setAuthTokenExpirationTime(Date authTokenExpirationTime1) {
		authTokenExpirationTime = authTokenExpirationTime1;
	}

	public void switchApplication(String appName) throws BeyondPropsException {
		setApplication(appName);
		connect();
	}

	public void switchProjectBranch(String branchName) throws BeyondPropsException {
		setProjectBranch(branchName);
		connect();
	}

	public void switchConfig(Config config) throws BeyondPropsException {
		setConfig(config);
		connect();
	}

	public BeyondPropsClient connect() throws BeyondPropsException {

		//let's authenticate 
		JSONObject req = new JSONObject();

		String envApiKey = System.getenv("beyondprops.apikey");

		if (authToken != null && authTokenExpirationTime.after(new Date())){
			req.put("authToken", authToken);
		} else {
			if (authenticatedConfig != null && authenticatedConfig.getApiKey() != null) {
				req.put("apiKey", authenticatedConfig.getApiKey());
			} else if (apiKey != null) {
				req.put("apiKey", apiKey);
			} else if (envApiKey !=null) {
				req.put("apiKey", envApiKey);
			}
			else {
				throw new BeyondPropsException("Api Key is missing");
			}
		}

		if (config != null) {
			if (config.getApplication() != null) {
				req.put("appName", config.getApplication());
			}

			if (config.getProjectBranch() != null) {
				req.put("projectBranch", config.getProjectBranch());
			}

			if (config.getLookup() != null) {
				req.put("config", config.getLookup());
			}
		}

		String endPoint;

		if (mode.equals("dev")) {
			endPoint = AUTHENTICATION_API_ENDPOINT_DEV;
		} else {
			endPoint = AUTHENTICATION_API_ENDPOINT_PROD;
		}
		HttpResponse<JsonNode> jsonResponse;
		try {
			jsonResponse = Unirest.post(endPoint)
					.header("accept", "application/json")
					.body(req.toString())
					.asJson();

			JSONObject response = jsonResponse.getBody().getObject();
			JSONObject responseData = null;
			if ( response != null && response.getInt("statusCode") == 0) {
				responseData = response.getJSONArray("data").getJSONObject(0).getJSONObject("map");

				authToken = responseData.getString("authToken");
				String dtString = responseData.getString("expirationDate");
				authTokenExpirationTime = (Date)formatter.parse(dtString);
				JSONObject auth = responseData.getJSONObject("authentication").getJSONObject("map");
				apiKey = auth.getString("apiKey");
				authenticatedConfig = new BeyondProps().new Config(auth);

			} else {
				throw new BeyondPropsException("Couldn't authenticate " + response.getJSONArray("errors").join(","));
			}
		} catch (UnirestException e) {
			e.printStackTrace();
			throw new BeyondPropsException (e.getMessage());
		} catch (ParseException e) {
			e.printStackTrace();
			throw new BeyondPropsException (e.getMessage());
		}

		return this;
	}

	//TBD lookup hierarchy could be set as part of update
	public BeyondPropsClient updateAllProperties() throws BeyondPropsException {

		JSONObject req = new JSONObject();

		req.put("authToken", getUpdatedAuthToken());
		JSONObject data = new JSONObject();
		for (String prop : localPropertiesCache.keySet()) {
			data.put(prop, localPropertiesCache.get(prop).propertyVal);
		}
		req.put("data", data);
		if (authenticatedConfig != null) {
			if (authenticatedConfig.getApplication() == null) {
				throw new BeyondPropsException("Application needs to be set before attempting to update properties");
			}

			if (authenticatedConfig.getProjectBranch() == null) {
				throw new BeyondPropsException("Project Branch needs to be set before attempting to update properties");
			}

			if (authenticatedConfig.getLookup() == null) {
				throw new BeyondPropsException("Lookup hierarchy needs to be set before attempting to update properties");
			}
		} else {
			;
			//throw new BeyondPropsException("Client needs to be connected with Application, Project Branch and Lookup hierarchy before attempting to update properties");
		}

		String endPoint;

		if (mode.equals("dev")) {
			endPoint = SETPROPERTY_API_ENDPOINT_DEV;
		} else {
			endPoint = SETPROPERTY_API_ENDPOINT_PROD;
		}

		try {
			
			String strReq = Base64.encodeBase64String(zip(req.toString()));
			
			HttpResponse<String> strResponse = Unirest.post(endPoint)
					.header("accept", "text/plain")
					.header("compressed", "true")
					.body(strReq)
					.asString();
			//.asJson();
			
			String strResponse1 = null;
			
			if (strResponse.getHeaders().containsKey("compressed") && strResponse.getHeaders().get("compressed").equals("true")) {
				strResponse1 = unzip(Base64.decodeBase64(strResponse.getBody()));
			} else {
				strResponse1 = strResponse.getBody();
			}
			
			JSONObject response = new JSONObject(strResponse1);
			
			if ( response != null && response.getInt("statusCode") == 0) {
				for (String property : localPropertiesCache.keySet()) {
					localPropertiesCache.get(property).synced = true;
				}
			} else {
				throw new BeyondPropsException("Couldn't set properties, " + response.getJSONArray("errors").join(","));
			}
		} catch (UnirestException e) {
			throw new BeyondPropsException("Couldn't set properties, " + e.getMessage());
		}
		return this;
	}

	public Map<String, PropertyValue> getPropertiesAsMap(String[] properties) throws BeyondPropsException {
		Map<String, PropertyValue> retValue = new HashMap<String, PropertyValue>();

		JSONObject data = getProperties(properties);
		for (String key : properties) {
			if (data.has(key)) {
				JSONObject propValType = data.getJSONObject(key);
				if (propValType.has("map")) {
					propValType = propValType.getJSONObject("map");
				}
				retValue.put(key, new PropertyValue(propValType.getString("val"), propValType.getInt("type")));
			}
		}

		if (retValue.size() == 0) {
			throw new BeyondPropsException("Couldn't retrieve properties, please check property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
		return retValue;
	}

	public List<PropertyValue> getPropertiesAsList(String[] properties) throws BeyondPropsException {
		JSONObject data = getProperties(properties);
		List<PropertyValue> lst = new LinkedList<PropertyValue>();
		JSONObject propValType = null;
		for (String key : properties) {
			if (data.has(key)) {
				propValType = data.getJSONObject(key);
				if (propValType.has("map")) {
					propValType = propValType.getJSONObject("map");
				}
				lst.add(new PropertyValue(propValType.getString("val"), propValType.getInt("type")));
			}
		}

		if (lst.size() == 0) {
			throw new BeyondPropsException("Couldn't retrieve properties, please check property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
		return lst;
	}

	public PropertyValue getProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property).getJSONObject("map");
			return new PropertyValue(propValType.getString("val"), propValType.getInt("type"));
		} else {
			throw new BeyondPropsException("Couldn't retrieve properties, please check property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}

	public String getStringProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property);
			if (propValType.has("map")) {
				propValType = propValType.getJSONObject("map");
			}
			if (propValType.getInt("type") == BeyondProps.STRING.getValueType()) {
				return propValType.getString("val");
			} else {
				throw new BeyondPropsException("Couldn't retrieve property, property type may not be a String");
			}
		} else {
			throw new BeyondPropsException("Couldn't retrieve property, please check if property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}

	public Double getNumberProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property);
			if (propValType.has("map")) {
				propValType = propValType.getJSONObject("map");
			}
			if (propValType.getInt("type") == BeyondProps.NUMBER.getValueType()) {
				return propValType.getDouble("val");
			} else {
				throw new BeyondPropsException("Couldn't retrieve property, property type may not be a Number");
			}
		} else {
			throw new BeyondPropsException("Couldn't retrieve property, please check if property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}

	public Boolean getBooleanProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property);
			if (propValType.has("map")) {
				propValType = propValType.getJSONObject("map");
			}
			if (propValType.getInt("type") == BeyondProps.BOOLEAN.getValueType()) {
				String val = propValType.getString("val");
				if (val.equalsIgnoreCase("TRUE") || val.equalsIgnoreCase("YES") || val.equals("1"))
					return true;
				else
					return false;
			} else {
				throw new BeyondPropsException("Couldn't retrieve property, property type may not be a Boolean");
			}
		} else {
			throw new BeyondPropsException("Couldn't retrieve property, please check if property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}

	public Date getDateProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property);
			if (propValType.has("map")) {
				propValType = propValType.getJSONObject("map");
			}
			if (propValType.getInt("type") == BeyondProps.DATE.getValueType()) {
				String val = propValType.getString("val");
				try {
					return formatter.parse(val);
				} catch(ParseException pe) {
					throw new BeyondPropsException ("Couldn't parse Date, should be in the format:" + formatter.toString() + ", value:" + val);
				}
			} else {
				throw new BeyondPropsException("Couldn't retrieve property, property type may not be a Date");
			}
		} else {
			throw new BeyondPropsException("Couldn't retrieve property, please check if property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}

	public Map<String, String> getMapProperty(String property) throws BeyondPropsException {
		JSONObject data = getProperties(new String[] {property});
		if (data.has(property)) {
			JSONObject propValType = data.getJSONObject(property);
			if (propValType.has("map")) {
				propValType = propValType.getJSONObject("map");
			}
			if (propValType.getInt("type") == BeyondProps.MAP.getValueType()) {
				String val = propValType.getString("val");
				try {
					Map<String, String> out = new HashMap<String, String>();
					String[] vals = val.split(";");
					for (int i = 0; i < vals.length; i++) {
						String[] keyVal = vals[i].split("=");
						out.put(keyVal[0], keyVal[1]);
					}
					return out;
				} catch(Exception pe) {
					throw new BeyondPropsException ("Couldn't parse Map, should be in the format:key1=val1;key2=val2" + " value:" + val);
				}
			} else {
				throw new BeyondPropsException("Couldn't retrieve property, property type may not be a Map");
			}
		} else {
			throw new BeyondPropsException("Couldn't retrieve property, please check if property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());
		}
	}
	
	public PropertyValue[] getPropertiesAsArray(String[] properties) throws BeyondPropsException {
		PropertyValue[] retValue;

		JSONObject data = getProperties(properties);
		List<PropertyValue> lst = new LinkedList<PropertyValue>();
		JSONObject propValType = null;
		for (String key : properties) {
			if (data.has(key)) {
				propValType = data.getJSONObject(key);
				if (propValType.has("map")) {
					propValType = propValType.getJSONObject("map");
				}
				lst.add(new PropertyValue(propValType.getString("val"), propValType.getInt("type")));
			}
		}

		if (lst.size() == 0)
			throw new BeyondPropsException("Couldn't retrieve properties, please check property values are set for the application: " + authenticatedConfig.getApplication() + ", branch:" + authenticatedConfig.getProjectBranch() + ", lookup hierarchy:" + authenticatedConfig.getLookup());

		retValue = new PropertyValue[lst.size()];
		retValue = lst.toArray(retValue);
		return retValue;
	}

	//Private Variables --- Start-----

	private static String AUTHENTICATION_API_ENDPOINT_DEV = "https://services.beyondpropsdev.net/authenticate";
	private static String AUTHENTICATION_API_ENDPOINT_PROD = "https://services.beyondprops.net/authenticate";
	private static String COLLECTION_API_ENDPOINT_DEV = "https://services.beyondpropsdev.net/collections";
	private static String COLLECTION_API_ENDPOINT_PROD = "https://services.beyondprops.net/collections";
	private static String SETPROPERTY_API_ENDPOINT_DEV = "https://services.beyondpropsdev.net/setprop";
	private static String SETPROPERTY_API_ENDPOINT_PROD = "https://services.beyondprops.net/setprop";
	private static String GETPROPERTY_API_ENDPOINT_DEV = "https://services.beyondpropsdev.net/getprop";
	private static String GETPROPERTY_API_ENDPOINT_PROD = "https://services.beyondprops.net/getprop";
	static String mode = "prod";
	private String apiKey;
	private Config config;
	private static Config authenticatedConfig;
	private static String authToken;
	private static Date authTokenExpirationTime;
	private Map<String, PropertyObject> localPropertiesCache;
	//Private Variables --- End -----

	//Private Methods --- Start-----

	private JSONObject getProperties (String[] properties) throws BeyondPropsException {

		JSONObject data = new JSONObject();
		
		List<String> propertiesList = Arrays.asList(properties);

		List<PropertyValue> propsObjs = new ArrayList<PropertyValue>();
		
		PropertyValue o = null;
		for (String prop : propertiesList) {
			o = getLocalProperty(prop).propertyVal;
			if (o != null) {
				data.put(prop, o.asJSON());
				propertiesList.remove(prop);
			}
		}
		
		if (propertiesList.size() > 0) {
			//let's authenticate 
			JSONObject req = new JSONObject();
	
			req.put("authToken", getUpdatedAuthToken());
	
			req.put("properties", propertiesList);
	
			if (authenticatedConfig != null) {
				if (authenticatedConfig.getApplication() == null) {
					throw new BeyondPropsException("Application needs to be set before attempting to retrieve properties");
				}
	
				if (authenticatedConfig.getProjectBranch() == null) {
					throw new BeyondPropsException("Project Branch needs to be set before attempting to retrieve properties");
				}
	
				if (authenticatedConfig.getLookup() == null) {
					throw new BeyondPropsException("Lookup hierarchy needs to be set before attempting to retrieve properties");
				}
			} else {
				//throw new BeyondPropsException("Client needs to be connected with Application, Project Branch and Lookup hierarchy before attempting to retrieve properties");
			}
	
			String endPoint;
	
			if (mode.equals("dev")) {
				endPoint = GETPROPERTY_API_ENDPOINT_DEV;
			} else {
				endPoint = GETPROPERTY_API_ENDPOINT_PROD;
			}
	
			try {
				
				String strReq = Base64.encodeBase64String(zip(req.toString()));
				
				HttpResponse<String> strResponse = Unirest.post(endPoint)
						.header("accept", "text/plain")
						.header("compressed", "true")
						.body(strReq)
						.asString();
				//.asJson();
				
				String strResponse1 = null;
				
				if (strResponse.getHeaders().containsKey("compressed")) {
					strResponse1 = unzip(Base64.decodeBase64(strResponse.getBody()));
				} else {
					strResponse1 = strResponse.getBody();
				}
				
				JSONObject response = new JSONObject(strResponse1);
				
				if ( response != null && response.getInt("statusCode") == 0) {
					data = add(data, response.getJSONArray("data").getJSONObject(0).getJSONObject("map"));
					
					for (String key : data.keySet()) {
						JSONObject obj = data.getJSONObject(key).getJSONObject("map");
						String val = obj.getString("val");
						ValueTypes type = ValueTypes.fromType(obj.getInt("type"));
						localPropertiesCache.put(key, new PropertyObject(new PropertyValue(val, type), true));
					}
					
					return data;
				} else {
					throw new BeyondPropsException("Couldn't retrieve properties, " + response.getJSONArray("errors").join(","));
				}
			} catch (UnirestException e) {
				throw new BeyondPropsException("Couldn't retrieve properties " + e.getMessage());
			}
		} else {
			return data;
		}
	}

	private String getUpdatedAuthToken() throws BeyondPropsException {

		if (authToken == null || authTokenExpirationTime.before(new Date())) {
			//we need to re-authenticate
			connect();
		}

		return authToken;
	}

	private byte[] zip(final String str) {
		if ((str == null) || (str.length() == 0)) {
			throw new IllegalArgumentException("Cannot zip null or empty string");
		}

		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
			try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
				gzipOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
			}
			return byteArrayOutputStream.toByteArray();
		} catch(IOException e) {
			throw new RuntimeException("Failed to zip content", e);
		}
	}

	private String unzip(String compressed) {
		return unzip(compressed.getBytes());
	}
	
	private String unzip(final byte[] compressed) {
		if ((compressed == null) || (compressed.length == 0)) {
			throw new IllegalArgumentException("Cannot unzip null or empty bytes");
		}
		if (!isZipped(compressed)) {
			return new String(compressed);
		}

		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed)) {
			try (GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {
				try (InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)) {
					try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
						StringBuilder output = new StringBuilder();
						String line;
						while((line = bufferedReader.readLine()) != null){
							output.append(line);
						}
						return output.toString();
					}
				}
			}
		} catch(IOException e) {
			throw new RuntimeException("Failed to unzip content", e);
		}
	}


	private boolean isZipped(final byte[] compressed) {
		return (compressed[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (compressed[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
	}
	//Private Methods --- End-----

	/*
	 * public static void main(String args[]) { BeyondPropsClient c = new
	 * BeyondPropsClient(); String s = new String(c.zip("{'a' : 'b'}"));
	 * System.out.println(s); String s2 = c.unzip(s); System.out.println(s2); }
	 */

	private JSONObject add(JSONObject obj1, JSONObject obj2) {
		if (obj1 == null || obj2 == null)
			return obj1;
		
		for (String key : obj2.keySet()) {
			obj1.put(key, obj2.get(key));
		}
		
		return obj1;
	}
}
