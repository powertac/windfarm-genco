package org.powertac.wpgenco;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the windfarm efficiency curve
 * @author Shashank Pande
 *
 */
public class WindFarmEfficiencyCurve {
	
	public static class WindSpeedband {
		private double fromWindSpeed = 0;
		private double toWindSpeed = 0;
		public WindSpeedband(double fromSpeed, double toSpeed) {
			this.fromWindSpeed = fromSpeed;
			this.toWindSpeed = toSpeed;
		}
		public double getFromWindSpeed() {
			return this.fromWindSpeed;
		}
		public double getToWindSpeed() {
			return this.toWindSpeed;
		}
		public boolean isWithinSpeedband(double windSpeed) {
			if ((windSpeed >= fromWindSpeed) && (windSpeed < toWindSpeed)) {
				return true;
			} else {
				return false;
			}
		}
	} //static class WindSpeedband
	
	/** This map should be populated from database values */
	private List<WindSpeedband> windSpeedbands = new ArrayList<WindSpeedband>();
	private List<Double>        slope          = new ArrayList<Double>();
	private List<Double>        yIntercept     = new ArrayList<Double>();
	
	public WindFarmEfficiencyCurve() {
		// write code here to populate the mapWindSpeedToEfficiency
		double[] from = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
		double[] to   = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
		double[] m    = {0.112704918, 0.048960386, 0.022516468, 0.01184951, 0.012746067, 
				         0.007222986, -0.029581606, -0.068315931, -0.068956675, -0.055775751};
		double[] b    = {-0.215582134, 0.103140528, 0.261804034, 0.33647274, 0.329300284,
				          0.379008009, 0.747053936, 1.173131512, 1.180820432, 1.009468425};
		for (int i = 0; i < from.length; i++) {
			WindSpeedband wspb = new WindSpeedband(from[i], to[i]);
			windSpeedbands.add(wspb);
			slope.add(m[i]);
			yIntercept.add(b[i]);
		} //for i..
	} //WindFarmEfficiencyCurve()
	
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
	}

} //class WindFarmEfficiencyCurve
