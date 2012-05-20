package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;

/**
 * This class represents the windfarm efficiency curve
 * @author Shashank Pande
 *
 */
@Domain
@ConfigurableInstance
public class WindFarmEfficiencyCurve {
	
	private static class WindSpeedband {
		private double fromWindSpeed = 0;
		private double toWindSpeed = 0;
		public WindSpeedband(double fromSpeed, double toSpeed) {
			this.fromWindSpeed = fromSpeed;
			this.toWindSpeed = toSpeed;
		}
		public boolean isWithinSpeedband(double windSpeed) {
			if ((windSpeed >= fromWindSpeed) && (windSpeed < toWindSpeed)) {
				return true;
			} else {
				return false;
			}
		}
	} //static class WindSpeedband
	
	/** Configured values to be read as List of Strings */
	@ConfigurableValue(valueType = "List", description = "wind speed bands")
	private List<String> cfgWindSpeedbands = null;
	@ConfigurableValue(valueType = "List", description = "value of slope in a linear equation")
	private List<String> cfgSlope          = null;
	@ConfigurableValue(valueType = "List", description = "value of y intercept in a linear equation")
	private List<String> cfgYIntercept     = null;
	
	/** This map should be populated from configured values */
	private List<WindSpeedband> windSpeedbands = new ArrayList<WindSpeedband>();
	private List<Double>        slope          = new ArrayList<Double>();
	private List<Double>        yIntercept     = new ArrayList<Double>();
	
	/**
	 * Constructor
	 */
	public WindFarmEfficiencyCurve() {
		initialize();		
	} //WindFarmEfficiencyCurve()
	
	private void initialize() {
		
		// write code here to populate the mapWindSpeedToEfficiency
		for (int i = 0; i < cfgWindSpeedbands.size(); i++) {
			String from_to = cfgWindSpeedbands.get(i);
			String[] fromtoarray = from_to.split("-");
			WindSpeedband wspb = new WindSpeedband(Double.valueOf(fromtoarray[0]), Double.valueOf(fromtoarray[1]));
			this.windSpeedbands.add(wspb);
			
			this.slope.add(Double.valueOf(cfgSlope.get(i)));
			this.yIntercept.add(Double.valueOf(cfgYIntercept.get(i)));
			
		}		
	}
	
	/**
	 * get efficiency for given wind speed in m/sec
	 * @param windSpeed wind speed in m/sec
	 * @return efficiency
	 */
	public double getEfficiency(double windSpeed) {
		int indexInList = -1;
		for (int i = 0; i < windSpeedbands.size(); i++) {
			if (windSpeedbands.get(i).isWithinSpeedband(windSpeed)) {
				indexInList = i;
				break;
			}
		}
		if (indexInList > -1) {
			double m = slope.get(indexInList);
			double b = yIntercept.get(indexInList);
			return (m * windSpeed + b);
		} else {
			return 0;
		}
	} //get efficiency

} //class WindFarmEfficiencyCurve
