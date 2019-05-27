package io.beyondprops.test;

import com.beyondprops.BeyondProps;
import com.beyondprops.BeyondPropsClient;

public class TestBeyondPropsJavaClient {

	static BeyondPropsClient bpClient = null;
	static BeyondProps bp = new BeyondProps();

	public static void setup() {
		try {
			//create a new application
			BeyondPropsClient.Collections.Applications.add(bp.new Application("CRM", "New App", "New App for testing"));
			//make the application current
			bpClient.switchApplication("CRM");
			
			//create properties
			BeyondPropsClient.Collections.Properties.add(bp.new Property("surveys.enabled", "Flag to turn on customer surveys", "survey customersurvey", "ALL", BeyondProps.BOOLEAN));
			BeyondPropsClient.Collections.Properties.add(bp.new Property("current.promotype", "Property to specify current promo that's in effect", BeyondProps.STRING));

			//create config/lookup hierarchy
			BeyondPropsClient.Collections.LookupHierarchy.add(bp.new Lookup("mode", new String[] {"dev", "prod", "stage", "qa", "uat"}, 1));
			BeyondPropsClient.Collections.LookupHierarchy.add(bp.new Lookup("client", new String[] {"desktop", "mobile", "ivr", "chat"}, 2));

			//create project branches
			BeyondPropsClient.Collections.ProjectBranches.add(new BeyondProps().new ProjectBranch("master", "master branch for the new app", "master develop devel"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setProp() {
		try {
			BeyondProps.Config config = bp.new Config()
					.setApplication("CRM")
					.setProjectBranch("master")
					.setLookup("mode", "prod")
					.setLookup("client", "*");

			bpClient.switchConfig(config);

			bpClient.setProperty("surveys.enabled", "true")
			.setProperty("current.promotype", "UBEREATSRULE01");

			bpClient.updateAllProperties();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void setProp2() {
		try {
			BeyondProps.Config config = bp.new Config()
					.setApplication("NewApp")
					.setProjectBranch("master")
					.setLookup("mode", "*");

			bpClient.switchConfig(config);

			bpClient.setProperty("surveys.enabled", "false")
					.setProperty("current.promotype", "ZERODISCOUNT");

			bpClient.updateAllProperties();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void getProp2() {
		try {
			BeyondProps.Config config = bp.new Config()
					.setApplication("NewApp")
					.setProjectBranch("master")
					.setLookup("mode", "dev")
					.setLookup("client", "mobile");
			bpClient.switchConfig(config);

			System.out.println(bpClient.getPropertiesAsList(new String[] {"surveys.enabled", "current.promotype"}));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void getProp() {
		try {
			BeyondProps.Config config = bp.new Config()
					.setApplication("CRM")
					.setProjectBranch("master")
					.setLookup("mode", "prod")
					.setLookup("client", "desktop");
			bpClient.switchConfig(config);

			System.out.println(bpClient.getPropertiesAsList(new String[] {"surveys.enabled", "current.promotype"}));
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		try {

			long start = System.currentTimeMillis();
			
			bpClient = new BeyondPropsClient()
					.setMode("dev")
					.setApiKey("090a9801-601d-49de-9840-72dcad97aa91")
					.connect();

			long end = System.currentTimeMillis();
			
			System.out.println("Auth Token:" + bpClient.getAuthToken() + ", time taken:" + (end - start) + " milli seconds");
			
			setup();

			setProp();
			
			//setProp2();

			start = System.currentTimeMillis();
			getProp();
			end = System.currentTimeMillis();

			System.out.println("time taken:" + (end - start) + " milli seconds");
			
			//getProp2();

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
}
