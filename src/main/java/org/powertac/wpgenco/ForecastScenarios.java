/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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
 * This class represents forecast scenarios for wind speed forecast errors,
 * wind speed forecast, and wind farm power output.
 * 
 * @author Shashank Pande (spande00@gmail.com)
 * 
 */
@Domain
@ConfigurableInstance
public class ForecastScenarios
{

  private static Logger log = Logger.getLogger(ForecastScenarios.class);

  public static class Scenario
  {
    private double probability = 0.0;
    private List<Double> values = new ArrayList<Double>();

    public Scenario (final double prob, final List<Double> vals)
    {
      this.probability = prob;
      this.values = vals;
    }

    public double getProbability ()
    {
      return this.probability;
    }

    public List<Double> getValues ()
    {
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
  private final WindfarmGenco windfarmGenco;
  private final List<Scenario> windSpeedForecastErrorScenarios =
    new ArrayList<Scenario>();
  private final List<Scenario> windSpeedForecastScenarios =
    new ArrayList<Scenario>();
  private final List<Scenario> windFarmPowerOutputScenarios =
    new ArrayList<Scenario>();

  public ForecastScenarios (final WindfarmGenco ref)
  {
    this.windfarmGenco = ref;
    readWindForecastErrorScenarios();
  }

  /**
   * Read wind forecast error scenarios.
   * populate list windSpeedForecastErrorScenarios
   */
  private void readWindForecastErrorScenarios ()
  {
    // connect to the database
    final Connection conn = getDbConnection();
    if (conn == null) {
      log.error("failed to read the wind forecast error scenarios");
      return;
    }
    final String query =
      "select SCENARIO_NUMBER, PROBABILITY, ERROR_VALUE from "
              + "WIND_SPEED_FORECAST_ERROR_SCENARIOS WHERE LOCATION = "
              + this.location + " ORDER BY SCENARIO_NUMBER, HOUR";
    Statement stmt = null;
    final Map<Integer, Scenario> scenarioMap = new HashMap<Integer, Scenario>();
    ResultSet rs = null;
    try {
      stmt = conn.createStatement();
      rs = stmt.executeQuery(query);
      while (rs.next()) {
        final int scenario_num = rs.getInt("SCENARIO_NUMBER");
        final double probability = rs.getDouble("PROBABILITY");
        final double errorVal = rs.getDouble("ERROR_VALUE");
        Scenario errorScenario = scenarioMap.get(scenario_num);
        if (errorScenario == null) {
          final List<Double> vals = new ArrayList<Double>();
          vals.add(errorVal);
          errorScenario = new Scenario(probability, vals);
          scenarioMap.put(scenario_num, errorScenario);
        }
        else {
          final List<Double> vals = errorScenario.getValues();
          vals.add(errorVal);
        }
      } // for each row
      if (scenarioMap.size() > 0) {
        populateWindSpeedErrorScenarios(scenarioMap);
      }
      else {
        log.error("no windspeed error scenarios found");
      }
    }
    catch (final SQLException ex) {
      log.error("Error reading wind speed error scenarios from database", ex);
    }
  } // readWindForecastErrorScenarios()

  private void
    populateWindSpeedErrorScenarios (final Map<Integer, Scenario> scenarioMap)
  {
    if (scenarioMap.size() > 0) {
      this.windSpeedForecastErrorScenarios.clear();
    }
    else {
      return;
    }

    final List<Scenario> scenarioList =
      new ArrayList<Scenario>(scenarioMap.values());
    for (final Scenario errorScenario: scenarioList) {
      windSpeedForecastErrorScenarios.add(errorScenario);
    }
  }

  private Connection getDbConnection ()
  {
    Connection conn = null;
    final String dbURL = getDbURL();
    try {
      conn = DriverManager.getConnection(dbURL, dbUserName, dbUserPassword);
    }
    catch (final SQLException e) {
      log.error("getCobbection: unable to connect to database", e);
    }

    return conn;
  }

  private String getDbURL ()
  {
    String url = null;
    url = "jdbc:" + this.dbms + "://" + this.dbHostName + ":" + this.port + "/";
    return url;
  }

  /**
   * calculate wind speed forecast scenarios
   */
  public void calcWindSpeedForecastScenarios ()
  {
    windSpeedForecastScenarios.clear();
    final List<Double> windSpeedForecastValues =
      windfarmGenco.getWindForecast().getWindSpeeds();
    final List<Double> windSpeeds = new ArrayList<Double>();
    for (final Scenario errorScenario: windSpeedForecastErrorScenarios) {
      final double probability = errorScenario.getProbability();
      final List<Double> errorValues = errorScenario.getValues();
      for (int i = 0; i < errorValues.size(); i++) {
        final double errval = errorValues.get(i);
        final double windforecast = windSpeedForecastValues.get(i);
        final double windSpeed = windforecast + errval;
        windSpeeds.add(windSpeed);
      }
      windSpeedForecastScenarios.add(new Scenario(probability, windSpeeds));
    } // for each error scenario
  } // calcWindSpeedForecastScenarios()

  /**
   * Calculate Wind Power output scenarios.
   */
  public void calcPowerOutputScenarios ()
  {
    windFarmPowerOutputScenarios.clear();
    final List<Double> powerOutputs = new ArrayList<Double>();
    final List<Double> airPressures =
      windfarmGenco.getWindForecast().getAirPressure();
    final List<Double> temperatures =
      windfarmGenco.getWindForecast().getTemperature();
    for (final Scenario windSpForecastScenario: windSpeedForecastScenarios) {
      final double probability = windSpForecastScenario.getProbability();
      powerOutputs.clear();
      final List<Double> windSpeeds = windSpForecastScenario.getValues();
      for (int i = 0; i < windSpeeds.size(); i++) {
        final double airpressure = airPressures.get(i);
        final double windspeed = windSpeeds.get(i);
        final double temperature = temperatures.get(i);
        final double airdensity =
          WindfarmGenco.getDryAirDensity(airpressure, temperature);
        final double powerout =
          windfarmGenco.getEstimatedPowerOutput(windspeed, airdensity);
        powerOutputs.add(powerout);
      }
      windFarmPowerOutputScenarios.add(new Scenario(probability, powerOutputs));
    } // for each wind speed scenario
  }

  public List<Scenario> getWindPowerOutputScenarios ()
  {
    return Collections.unmodifiableList(windFarmPowerOutputScenarios);
  }

}
