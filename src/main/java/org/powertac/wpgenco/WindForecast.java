package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.repo.WeatherForecastRepo;
import org.springframework.beans.factory.annotation.Autowired;

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
