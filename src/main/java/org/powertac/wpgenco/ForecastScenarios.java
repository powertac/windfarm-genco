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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

//import org.apache.log4j.Logger;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;
import org.powertac.wpgenco.Scenario.ScenarioValue;

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

  //private static Logger log = Logger.getLogger(ForecastScenarios.class);

  /*
   * Condifured attributes
   */
  @ConfigurableValue(valueType = "String", description = "location of forecast")
  String location;

  // member variables
  private final WindfarmGenco windfarmGenco;
  private WindForecastErrorScenarios windspeedErrorScenarios = null;
  private final List<Scenario> windSpeedForecastScenarios =
    new ArrayList<Scenario>();
  private final List<Scenario> windFarmPowerOutputScenarios =
    new ArrayList<Scenario>();

  public ForecastScenarios (final WindfarmGenco ref)
  {
    this.windfarmGenco = ref;
    windspeedErrorScenarios = WindForecastErrorScenarios.getWindForecastErrorScenarios();
  }


  /**
   * calculate wind speed forecast scenarios
   */
  public void calcWindSpeedForecastScenarios ()
  {
    windSpeedForecastScenarios.clear();
    final List<Double> windSpeedForecastValues =
      windfarmGenco.getWindForecast().getWindSpeeds();
    for (final Scenario errorScenario : windspeedErrorScenarios.getScenarios()) {
      int scenarioNumber = errorScenario.getScenarioNumber();
      final double probability = errorScenario.getProbability();
      Scenario windSpeedScenario = new Scenario(scenarioNumber, probability);
      final Set<ScenarioValue> errorValues = errorScenario.getValues();
      for (ScenarioValue ev : errorValues) {
        final int    leadHour = ev.getHour();
        final double errval = ev.getValue();
        final double windforecast = windSpeedForecastValues.get(leadHour - 1);
        final double windSpeed = windforecast + errval;
        windSpeedScenario.addValue(new ScenarioValue(leadHour, windSpeed));       
      }
      windSpeedScenario.createValueList();
      windSpeedForecastScenarios.add(windSpeedScenario);
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
      final int scenarioNumber = windSpForecastScenario.getScenarioNumber();
      powerOutputs.clear();
      final Set<ScenarioValue> windSpeeds = windSpForecastScenario.getValues();
      Scenario powerOutputScenario = new Scenario(scenarioNumber, probability);
      for (ScenarioValue wsp : windSpeeds) {
        int hour = wsp.getHour();
        double windSpeed = wsp.getValue();
        double airpressure = airPressures.get(hour - 1);
        double temperature = temperatures.get(hour - 1);
        double airdensity = WindfarmGenco.getDryAirDensity(airpressure, temperature);
        double powerout = windfarmGenco.getEstimatedPowerOutput(windSpeed, airdensity);
        powerOutputScenario.addValue(new ScenarioValue(hour, powerout));
      } //for each wind speed scenario value
      powerOutputScenario.createValueList();
      windFarmPowerOutputScenarios.add(powerOutputScenario);
    } // for each wind speed scenario
  }

  public List<Scenario> getWindPowerOutputScenarios ()
  {
    return Collections.unmodifiableList(windFarmPowerOutputScenarios);
  }

}
