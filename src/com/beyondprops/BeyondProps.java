package com.beyondprops;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

public class BeyondProps {

	public static class PropertyObject {
		PropertyValue propertyVal;
		boolean synced = false;
		
		public PropertyObject(PropertyValue propertyVal) {
			this.propertyVal = propertyVal;
			this.synced = false;
		}
		
		public PropertyObject(PropertyValue propertyVal, boolean synced) {
			this.propertyVal = propertyVal;
			this.synced = synced;
		}
	
		public JSONObject asJSON() {
			JSONObject jo = propertyVal.asJSON();
			jo.put("synced", synced);
			return jo;
		}
	}
	
	public static class PropertyValue {
		private Object value1;
		private ValueTypes type1;
		
		private final Pattern valFromEnvPattern = Pattern.compile("(\\$ENV\\{.*\\})(|.*)?");
		
		public void setValue(Object value) {
			this.value1 = checkEnv(value);
		}
		
		public void setValueType(ValueTypes type) {
			type1 = type;
		}
		
		public void setValueType(int type) {
			 ValueTypes.fromType(type);
		}
		
		public Object value() {
			return value1;
		}
		
		public ValueTypes type() {
			return type1;
		}
		
		public PropertyValue(Object value) {
			this.value1 = value;
			this.type1 = STRING;
		}
		
		public PropertyValue(String value, ValueTypes type) {
			this.value1 = checkEnv(value);
			this.type1 = type;
		}
		
		public PropertyValue(String value, int type) {
			this.value1 = checkEnv(value);
			this.type1 = ValueTypes.fromType(type);
		}
		
		public String checkEnv(Object value) {
			Matcher m = valFromEnvPattern.matcher(value.toString());
			if (m.matches()) {
				//we have an environment variable
				String envVar = m.group(1);
				String fallback = m.group(2);
				String envVal = System.getenv(envVar);
				if (envVal != null) {
					return envVal;
				} else {
					if (fallback != null) {
						return fallback;
					} else
						return envVar + " environment variable is not defined";
				}
			} else {
				return value.toString();
			}
		}
		

		public JSONObject asJSON() {
			JSONObject jo = new JSONObject();
			jo.put("val", value1);
			jo.put("type", type1.getValueType());
			return jo;
			
		}
		
		public String toString() {
			return "Value:" + value();
		}
	}
	
	public class Config {
		String apiKey, application, projectBranch;
		Map<String, String> lookup;
		
		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public Config(JSONObject jsonObject) {
			
			if (jsonObject.has("apiKey"))
				apiKey = jsonObject.getString("apiKey");
			
			if (jsonObject.has("application"))
				application = jsonObject.getString("application");
			
			if (jsonObject.has("projectBranch"))
				projectBranch = jsonObject.getString("projectBranch");
			
			if (jsonObject.has("lookupHierarchy")) {
				JSONObject temp = jsonObject.getJSONObject("lookupHierarchy").getJSONObject("map");
				lookup = new HashMap<String, String>();
				for (String key : temp.keySet()) {
					lookup.put(key, temp.getString(key));
				}
			}
		}
		
		public Config() {
			
		}

		public String getApplication() {
			return application;
		}
		
		public Config setApplication(String application) {
			this.application = application;
			return this;
		}
		
		public String getProjectBranch() {
			return projectBranch;
		}
		
		public Config setProjectBranch(String projectBranch) {
			this.projectBranch = projectBranch;
			return this;
		}
		
		public Map<String, String> getLookup() {
			return lookup;
		}
		
		public String getLookup(String key) {
			if (lookup != null) {
				return lookup.get(key);
			} else {
				return null;
			}
		}
		
		public Config setLookup(Map<String, String> lookup) {
			this.lookup = lookup;
			return this;
		}

		public Config setLookup(String key, String val) {
			if (lookup == null) {
				lookup = new HashMap<String, String>();
			}
			lookup.put(key, val);
			return this;
		}
	}
	
	public abstract class Collection {
		String name, desc, tags, lastUpdatedBy, lastUpdatedOn, createdDate;
		
		public Collection(String name2, String desc2, String tags2) {
			this.name = name2;
			this.desc = desc2;
			this.tags = tags2;
		}

		public Collection(String name2, String desc2, String tags2, String createdDate2, String lastUpdatedBy2,
				String lastUpdatedOn2) {
			this.name = name2;
			this.desc = desc2;
			this.tags = tags2;
			this.createdDate = createdDate2;
			this.lastUpdatedBy = lastUpdatedBy2;
			this.lastUpdatedOn = lastUpdatedOn2;
		}

		public String getLastUpdatedBy() {
			return lastUpdatedBy;
		}

		public void setLastUpdatedBy(String lastUpdatedBy) {
			this.lastUpdatedBy = lastUpdatedBy;
		}

		public String getLastUpdatedOn() {
			return lastUpdatedOn;
		}

		public void setLastUpdatedOn(String lastUpdatedOn) {
			this.lastUpdatedOn = lastUpdatedOn;
		}

		public String getCreatedDate() {
			return createdDate;
		}

		public void setCreatedDate(String createdDate) {
			this.createdDate = createdDate;
		}

		public String getName() {
			return name;
		}

		public Collection setName(String name) {
			this.name = name;
			return this;
		}

		public String getDesc() {
			return desc;
		}

		public Collection setDesc(String desc) {
			this.desc = desc;
			return this;
		}

		public String getTags() {
			return tags;
		}

		public Collection setTags(String tags) {
			this.tags = tags;
			return this;
		}
	}

	public class Application extends Collection {

		public Application(String name, String desc, String tags) {
			super(name, desc, tags);
		}

		public Application(String name, String desc, String tags, String createdDate, String lastUpdatedBy, String lastUpdatedOn) {
			super(name, desc, tags, createdDate, lastUpdatedBy, lastUpdatedOn);
		}

		public String toString() {
			return new StringBuilder().append("{\n").append("Name: ")
												  .append(name)
												  .append(",\n")
												  .append("Description: ")
												  .append(desc)
												  .append(",\n")
												  .append("Tags: ")
												  .append(tags)
												  .append("\n}")
												  .toString();
		}
	}
	
	public class ProjectBranch extends Collection {
		boolean enabled;
		
		public ProjectBranch(String name, String desc, String tags) {
			super (name, desc, tags);
		}
		
		public ProjectBranch(String name, String desc, String tags, String createdDate, String lastUpdatedBy, String lastUpdatedOn, boolean enabled) {
			super(name, desc, tags, createdDate, lastUpdatedBy, lastUpdatedOn);
			this.enabled = enabled;
		}
		
		public boolean isEnabled() {
			return enabled;
		}

		public ProjectBranch setEnabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}
	}

	public class Lookup extends Collection {
		
		int order;
		String[] values;
		
		public Lookup(String name, String[] values, int order) {
			super (name, "", "");
			this.order = order;
			this.values = values;
		}
		
		public Lookup(String name, String desc, String tags, String createdDate, String lastUpdatedBy, String lastUpdatedOn, int order, String[] values) {
			super(name, desc, tags, createdDate, lastUpdatedBy, lastUpdatedOn);
			this.order = order;
			this.values = values;
		}
		
		public int getOrder() {
			return order;
		}
		
		public Lookup setOrder(int order) {
			this.order = order;
			return this;
		}
		
		public String[] getValues() {
			return values;
		}
		
		public Lookup setValues(String[] values) {
			this.values = values;
			return this;
		}
	}

	public enum ValueTypes {
		STRING(1), NUMBER(2), BOOLEAN(3), DATE(4), REGEX(5), MAP(6);
		
		private final int valueType;
		 
		ValueTypes(int valueType) {   // constructor
			this.valueType = valueType;
		}
		 
		int getValueType() {
			return valueType;
		}

		public static ValueTypes fromType(int type) {
			switch(type) {
			case 1:
				return STRING;
			case 2:
				return NUMBER;
			case 3:
				return BOOLEAN;
    		case 4:
    			return DATE;
    		case 5:
    			return REGEX;
    		case 6:
    			return MAP;
    		default:
    			return STRING;
			}
		}
	}
	
	public static ValueTypes BOOLEAN = ValueTypes.BOOLEAN;
	public static ValueTypes STRING= ValueTypes.STRING;
	public static ValueTypes NUMBER = ValueTypes.NUMBER;
	public static ValueTypes DATE = ValueTypes.DATE;
	public static ValueTypes REGEX = ValueTypes.REGEX;
	public static ValueTypes MAP = ValueTypes.MAP;

	public class Property extends Collection {
		
		String availability="ALL";
		ValueTypes type;
		String defaultValue;

		public Property(String name, String desc, String tags, String availability, int type) {
			super(name, desc, tags);
			this.availability = availability;
			this.type = ValueTypes.fromType(type);
		}
		
		public Property(String name, String desc, int type) {
			super(name, desc, "");
			this.type = ValueTypes.fromType(type);
		}
		
		public Property(String name, String desc, ValueTypes type) {
			super(name, desc, "");
			this.type = type;
		}
		
		public Property(String name, String desc, String tags, int type) {
			super(name, desc, tags);
			this.type = ValueTypes.fromType(type);
		}
		
		public Property(String name, String desc, String tags, ValueTypes type) {
			super(name, desc, tags);
			this.type = type;
		}
		
		public Property(String name, String desc, String tags, String availability, ValueTypes type) {
			super(name, desc, tags);
			this.availability = availability;
			this.type = type;
		}
		
		public Property(String name, String desc, String tags, String availability, int type, String createdDate, String lastUpdatedBy, String lastUpdatedOn) {
			super(name, desc, tags, createdDate, lastUpdatedBy, lastUpdatedOn);
			this.availability = availability;
			this.type = ValueTypes.fromType(type);
		}
		
		public Property(String name, String desc, String tags, String availability, int type, String defaultValue) {
			super(name, desc, tags);
			this.availability = availability;
			this.type = ValueTypes.fromType(type);
			this.defaultValue = defaultValue;
		}
		

		public Property(String name, String desc, int type, String defaultValue) {
			super(name, desc, "");
			this.type = ValueTypes.fromType(type);
			this.defaultValue = defaultValue;
		}
		
		public Property(String name, String desc, ValueTypes type, String defaultValue) {
			super(name, desc, "");
			this.type = type;
			this.defaultValue = defaultValue;
		}
		
		public Property(String name, String desc, String tags, int type, String defaultValue) {
			super(name, desc, tags);
			this.type = ValueTypes.fromType(type);
			this.defaultValue = defaultValue;
		}
		
		public Property(String name, String desc, String tags, ValueTypes type, String defaultValue) {
			super(name, desc, tags);
			this.type = type;
			this.defaultValue = defaultValue;
		}
		
		public Property(String name, String desc, String tags, String availability, ValueTypes type, String defaultValue) {
			super(name, desc, tags);
			this.availability = availability;
			this.type = type;
			this.defaultValue = defaultValue;
		}
		
		public Property(String name, String desc, String tags, String availability, int type, String defaultValue, String createdDate, String lastUpdatedBy, String lastUpdatedOn) {
			super(name, desc, tags, createdDate, lastUpdatedBy, lastUpdatedOn);
			this.availability = availability;
			this.type = ValueTypes.fromType(type);
			this.defaultValue = defaultValue;
		}
		
		public String getAvailability() {
			return availability;
		}
		
		public Property setAvailability(String availability) {
			this.availability = availability;
			return this;
		}
		
		public ValueTypes getType() {
			return type;
		}
		
		public Property setType(ValueTypes type) {
			this.type = type;
			return this;
		}
		
		public String getDefaultValue() {
			return defaultValue;
		}

		public Property setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}
	}
	
}
