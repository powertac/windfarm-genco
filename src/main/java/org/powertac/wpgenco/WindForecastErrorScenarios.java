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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.log4j.Logger;

import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * This class represents wind speed forecast error scenarios.
 * 
 * @author spande00 (Shashank Pande)
 * 
 */
@ConfigurableInstance
@XStreamAlias("WindForecastErrorScenarios")
public class WindForecastErrorScenarios
{
  private static Logger log = Logger.getLogger(WindForecastErrorScenarios.class);
  // configured attribute
  // configured parameters
  @ConfigurableValue(valueType = "String", description = "path/name for the wind speed forecast scenarios file name")
  private static String errorScenarioDataFile;
  @XStreamImplicit
  private SortedSet<Scenario> windSpeedForecastErrorScenarios =
    new TreeSet<Scenario>();

  protected WindForecastErrorScenarios ()
  {

  }

  public WindForecastErrorScenarios (Collection<Scenario> scenarios)
  {
    windSpeedForecastErrorScenarios.addAll(scenarios);
  }

  public boolean addScenario (Scenario sco)
  {
    return windSpeedForecastErrorScenarios.add(sco);
  }

  public boolean addScenarios (Collection<Scenario> scenarioCollection)
  {
    return windSpeedForecastErrorScenarios.addAll(scenarioCollection);
  }

  public Set<Scenario> getScenarios ()
  {
    return Collections.unmodifiableSortedSet(windSpeedForecastErrorScenarios);
  }
  
  private static XStream getConfiguredXStream() {
    XStream xstream = new XStream();
    xstream.alias("Scenario", Scenario.class);
    xstream.alias("WindForecastErrorScenarios", WindForecastErrorScenarios.class);
    xstream.alias("Value", Scenario.ScenarioValue.class);
    xstream.addImplicitCollection(WindForecastErrorScenarios.class, "windSpeedForecastErrorScenarios");
    xstream.addImplicitCollection(Scenario.class, "values");
    xstream.useAttributeFor(Scenario.class, "scenarioNumber");
    xstream.aliasField("id", Scenario.class, "scenarioNumber");
    xstream.useAttributeFor(Scenario.class, "probability");
    xstream.useAttributeFor(Scenario.ScenarioValue.class, "hour");
    xstream.useAttributeFor(Scenario.ScenarioValue.class, "value"); 
    xstream.aliasField("error", Scenario.ScenarioValue.class, "value");
    return xstream;
  }
  
  public static WindForecastErrorScenarios getWindForecastErrorScenarios() {
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(errorScenarioDataFile);
    } catch (FileNotFoundException ex) {
      log.error(String.format("File not found %s", errorScenarioDataFile), ex);
    }
    XStream xstream = getConfiguredXStream(); 
    WindForecastErrorScenarios wferrorScenarios = 
            (WindForecastErrorScenarios) xstream.fromXML(inputStream);
    for (Scenario scn : wferrorScenarios.getScenarios()) {
      scn.createValueList();
    }
    return wferrorScenarios;
  }
  
  // TODO: remove this function. It is just for testing.
  public static void main(String[] args) {
    WindForecastErrorScenarios wsperrScenarios = new WindForecastErrorScenarios();
    for (int i = 0; i < 5; i++) { //create 6 scenarios for testing
      double prob = Math.random();
      int scNum = i+1; //scenario number
      Scenario scenario = new Scenario(scNum, prob);
      //add values to scenario
      for (int j = 0; j < 24; j++) {
        int hour = j + 1;
        double error = Math.random() * 100;
        scenario.addValue(new Scenario.ScenarioValue(hour, error));
      }
      wsperrScenarios.addScenario(scenario);
    }
    //create XML
    XStream xstream = getConfiguredXStream();
    String xmlStr = xstream.toXML(wsperrScenarios);
    try {
      FileWriter fw = new FileWriter("C:/microgrid/xstreamExample.xml");
      fw.write(xmlStr);
      fw.write("\n");
      fw.close();
    } catch (IOException ex) {
      System.out.println("IO ERROR");
    }
    
  }
}
