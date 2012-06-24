package org.powertac.wpgenco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;

/**
 * This class represents forecast scenarios
 * 
 * @author Shashank Pande (spande00@gmail.com)
 *
 */
@Domain
@ConfigurableInstance
public class ForecastScenarios {
	
	private static Logger log = Logger.getLogger(ForecastScenarios.class);
	
	public static class Scenario {
		private double probability = 0.0;
		private List<Double> values = new ArrayList<Double>();
		
		public Scenario(double prob, List<Double> vals) {
			this.probability = prob;
			this.values = vals;
		}
		public double getProbability() {
			return this.probability;
		}
		public List<Double> getValues() {
			return this.values;
		}
	}
	
	/*
	 * Condifured attributes
	 */
	@ConfigurableValue(valueType = "String", description = "location of forecast")
	String location;
	
	@ConfigurableValue(valueType = "String", description = "database vendor")
	String dbms;
	
	@ConfigurableValue(valueType = "String", description = "database host name")
	String dbHostName;
	
	@ConfigurableValue(valueType = "Integer", description = "port number")
	Integer port;
	
	@ConfigurableValue(valueType = "String", description = "database name")
	String dbName;
	
	@ConfigurableValue(valueType = "String", description = "database user name")
	String dbUserName;
	
	@ConfigurableValue(valueType = "String", description = "database user password")
	String dbUserPassword;
	
	
	// member variables
	private WindfarmGenco  windfarmGenco;
	private List<Scenario> windSpeedForecastErrorScenarios = new ArrayList<Scenario>();
	private List<Scenario> windSpeedForecastScenarios = new ArrayList<Scenario>();
	private List<Scenario> windFarmPowerOutputScenarios = new ArrayList<Scenario>();
	
	public ForecastScenarios(WindfarmGenco ref) {
		this.windfarmGenco = ref;
		readWindForecastErrorScenarios();
	}
	
	/**
	 * Read wind forecast error scenarios.
	 * populate list windSpeedForecastErrorScenarios
	 */
	private void readWindForecastErrorScenarios() {
		//connect to the database
		Connection conn = getDbConnection();
		if (conn == null) {
			log.error("failed to read the wind forecast error scenarios");
			return;
		}
		String query = "select SCENARIO_NUMBER, PROBABILITY, ERROR_VALUE from " + 
				       "WIND_SPEED_FORECAST_ERROR_SCENARIOS WHERE LOCATION = " + this.location +
				       " ORDER BY SCENARIO_NUMBER, HOUR";
		Statement stmt = null;
		Map<Integer, Scenario> scenarioMap = new HashMap<Integer, Scenario>();
		ResultSet rs = null;
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				int scenario_num = rs.getInt("SCENARIO_NUMBER");
				double probability = rs.getDouble("PROBABILITY");
				double errorVal = rs.getDouble("ERROR_VALUE");
				Scenario errorScenario = scenarioMap.get(scenario_num);
				if (errorScenario == null) {
					List<Double> vals = new ArrayList<Double>();
					vals.add(errorVal);
					errorScenario = new Scenario(probability, vals);
					scenarioMap.put(scenario_num, errorScenario);
				} else {
					List<Double> vals = errorScenario.getValues();
					vals.add(errorVal);
				}
			} //for each row
			if (scenarioMap.size() > 0) {
				populateWindSpeedErrorScenarios(scenarioMap);
			} else {
				log.error("no windspeed error scenarios found");
			}
		} catch (SQLException ex) {
			log.error("Error reading wind speed error scenarios from database", ex);
		}
	} //readWindForecastErrorScenarios()
	
	private void populateWindSpeedErrorScenarios(Map<Integer, Scenario> scenarioMap) {
		if (scenarioMap.size() > 0) {
			this.windSpeedForecastErrorScenarios.clear();
		} else {
			return;
		}
		
		List<Scenario> scenarioList = new ArrayList<Scenario>(scenarioMap.values());
		for (Scenario errorScenario : scenarioList) {
			windSpeedForecastErrorScenarios.add(errorScenario);
		}
	}
	
	private Connection getDbConnection() {
		Connection conn = null;
		String dbURL = getDbURL();
		try {
			conn = DriverManager.getConnection(dbURL, dbUserName, dbUserPassword);
		} catch (SQLException e) {
			log.error("getCobbection: unable to connect to database", e);
		}
		
		return conn;
	}
	
	private String getDbURL() {
		String url = null;
		url = "jdbc:"+this.dbms+"://"+this.dbHostName+":"+this.port+"/";
		return url;
	}
	
	/**
	 * calculate wind speed forecast scenarios
	 */
	public void calcWindSpeedForecastScenarios() {
		windSpeedForecastScenarios.clear();		
		List<Double> windSpeedForecastValues = windfarmGenco.getWindForecast().getWindSpeeds();
		List<Double> windSpeeds = new ArrayList<Double>();
		for (Scenario errorScenario : windSpeedForecastErrorScenarios) {
			double probability = errorScenario.getProbability();
			List<Double> errorValues = errorScenario.getValues();
			for (int i = 0; i < errorValues.size(); i++) {
				double errval = errorValues.get(i);
				double windforecast = windSpeedForecastValues.get(i);
				double windSpeed = windforecast + errval;
				windSpeeds.add(windSpeed);
			}
			windSpeedForecastScenarios.add(new Scenario(probability,windSpeeds));
		} //for each error scenario
	} //calcWindSpeedForecastScenarios()
	
	/**
	 * Calculate Wind Power output scenarios.
	 */
	public void calcPowerOutputScenarios() {
		windFarmPowerOutputScenarios.clear();
		List<Double> powerOutputs = new ArrayList<Double>();
		List<Double> airPressures = windfarmGenco.getWindForecast().getAirPressure();
		List<Double> temperatures = windfarmGenco.getWindForecast().getTemperature();
		for (Scenario windSpForecastScenario : windSpeedForecastScenarios) {
			double probability = windSpForecastScenario.getProbability();
			powerOutputs.clear();
			List<Double> windSpeeds = windSpForecastScenario.getValues();
			for (int i = 0; i < windSpeeds.size(); i++) {
				double airpressure = airPressures.get(i);
				double windspeed = windSpeeds.get(i);
				double temperature = temperatures.get(i);
				double airdensity = WindfarmGenco.getDryAirDensity(airpressure, temperature);
				double powerout = windfarmGenco.getEstimatedPowerOutput(windspeed, airdensity);
				powerOutputs.add(powerout);
			}
			windFarmPowerOutputScenarios.add(new Scenario(probability, powerOutputs));
		} //for each wind speed scenario
	}
	
	public List<Scenario> getWindPowerOutputScenarios() {
		return Collections.unmodifiableList(windFarmPowerOutputScenarios);
	}

}
