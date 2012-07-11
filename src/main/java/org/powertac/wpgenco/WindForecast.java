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

import org.apache.log4j.Logger;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.repo.WeatherForecastRepo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Retrieves weather forecast data from the weather forecast repo.
 * Provides API to retrieve the forecast data.
 * @author shashpan
 *
 */
public class WindForecast
{
  private static Logger log = Logger.getLogger(WindForecast.class);
  private static final double defaultAirPressure = 1.225;

  @Autowired
  private WeatherForecastRepo weatherForecastRepo;

  private List<Double> windSpeeds = new ArrayList<Double>();
  private List<Double> airPressure = new ArrayList<Double>();
  private List<Double> temperature = new ArrayList<Double>();

  public WindForecast ()
  {

  }

  public List<Double> getWindSpeeds ()
  {
    return Collections.unmodifiableList(windSpeeds);
  }

  public List<Double> getAirPressure ()
  {
    return Collections.unmodifiableList(airPressure);
  }

  public List<Double> getTemperature ()
  {
    return Collections.unmodifiableList(temperature);
  }

  public void refreshWeatherForecast ()
  {
    windSpeeds.clear();
    airPressure.clear();
    temperature.clear();
    if (weatherForecastRepo == null) {
      log.error("WeatherForecastRepo is not initialized");
      return;
    }

    WeatherForecast weatherForecast =
      weatherForecastRepo.currentWeatherForecast();
    List<WeatherForecastPrediction> windPredictions =
      weatherForecast.getPredictions();
    for (WeatherForecastPrediction wp: windPredictions) {
      windSpeeds.add(wp.getWindSpeed());
      airPressure.add(defaultAirPressure);
      temperature.add(wp.getTemperature());
    } // for each prediction
  } // refreshWeatherForecast()

} // class WindForecast
