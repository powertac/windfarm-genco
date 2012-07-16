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

  /*
   * Condifured attributes
   */
  @ConfigurableValue(valueType = "String", description = "location of forecast")
  String location;

  // member variables
  private final WindfarmGenco windfarmGenco;
  private final WindForecastErrorScenarios windspeedErrorScenarios = null;
  private final List<Scenario> windSpeedForecastScenarios =
    new ArrayList<Scenario>();
  private final List<Scenario> windFarmPowerOutputScenarios =
    new ArrayList<Scenario>();

  public ForecastScenarios (final WindfarmGenco ref)
  {
    this.windfarmGenco = ref;
  }


  private void
    populateWindSpeedErrorScenarios (final Map<Integer, Scenario> scenarioMap)
  {
    if (scenarioMap.size() > 0) {
      // TODO: this.windSpeedForecastErrorScenarios.clear();
    }
    else {
      return;
    }

    final List<Scenario> scenarioList =
      new ArrayList<Scenario>(scenarioMap.values());
    for (final Scenario errorScenario: scenarioList) {
      // TODO: windSpeedForecastErrorScenarios.add(errorScenario);
    }
  }

  /**
   * calculate wind speed forecast scenarios
   */
  public void calcWindSpeedForecastScenarios ()
  {
//    windSpeedForecastScenarios.clear();
//    final List<Double> windSpeedForecastValues =
//      windfarmGenco.getWindForecast().getWindSpeeds();
//    final List<ScenarioValue> windSpeeds = new ArrayList<ScenarioValue>();
//    for (final Scenario errorScenario: windSpeedForecastErrorScenarios) {
//      final double probability = errorScenario.getProbability();
//      final List<ScenarioValue> errorValues = errorScenario.getValues();
//      for (int i = 0; i < errorValues.size(); i++) {
//        final int    leadHour = errorValues.get(i).getLeadHour();
//        final double errval = errorValues.get(i).getValue();
//        final double windforecast = windSpeedForecastValues.get(i);
//        final double windSpeed = windforecast + errval;
//        windSpeeds.add(new ScenarioValue(leadHour, errval));
//      }
//      //windSpeedForecastScenarios.
//    } // for each error scenario
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
      final List<Double> windSpeeds = null; //TODO: windSpForecastScenario.getValues();
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
      //TODO: windFarmPowerOutputScenarios.add(new Scenario(probability, powerOutputs));
    } // for each wind speed scenario
  }

  public List<Scenario> getWindPowerOutputScenarios ()
  {
    return Collections.unmodifiableList(windFarmPowerOutputScenarios);
  }

}
