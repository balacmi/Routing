package matsimrouting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;

import com.google.inject.Provider;

public class MultithreadPTRouting {

	public static void main(String[] args) throws IOException {
		
		MultithreadPTRouting mt = new MultithreadPTRouting();
		
        ExecutorService executor = Executors.newFixedThreadPool(Integer.parseInt(args[4]));
		
		BufferedReader readLink = IOUtils.getBufferedReader(args[0]);
		readLink.readLine();
		String s = readLink.readLine();
		
		Config config = ConfigUtils.createConfig();	

		config.network().setInputFile(args[1]);

	    config.transit().setTransitScheduleFile(args[2]);
	    config.transit().setVehiclesFile(args[3]);
		
	    config.transit().setUseTransit(true);
		config.transitRouter().setSearchRadius(1000.0);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(600.0);
		final Scenario scenario = ScenarioUtils.loadScenario(config);
				
		TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(scenario.getTransitSchedule(), 600.0D);
	    ((PlanCalcScoreConfigGroup)config.getModule("planCalcScore")).setUtilityOfLineSwitch(-0.0D);
		final double travelingWalk = -6.0D;
		((PlanCalcScoreConfigGroup)config.getModule("planCalcScore")).getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);	    
	    PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
	    routeConfigGroup.getModeRoutingParams().get("walk").setBeelineDistanceFactor(1.3);
	    routeConfigGroup.getModeRoutingParams().get("walk").setTeleportedModeSpeed(1.25);

		    
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(),
				config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());

		Provider<TransitRouter> transitRouterFactory = new TransitRouterImplFactory(scenario.getTransitSchedule(), transitRouterConfig, routerNetwork);
		
		Set<RouteData> rd = new HashSet<>();
		Set<FrequencyData> fd = new HashSet<>();
		Set<WalkData> wd = new HashSet<>();

		long x = System.currentTimeMillis();
		while (s != null) {
			
			PTRouter worker = mt.new PTRouter(s, transitRouterFactory, scenario, rd, fd, wd, args[8]);
			executor.execute(worker);			
            s = readLink.readLine();
		}
		
		executor.shutdown();
		while(!executor.isTerminated()) {
			
		}
	        // Wait until all threads are finish
		final BufferedWriter outLinkR = IOUtils.getBufferedWriter(args[5]);
		for (RouteData routeData : rd) {
			
			outLinkR.write(routeData.toString());
			outLinkR.newLine();
		}
		outLinkR.flush();
		outLinkR.close();
		
		final BufferedWriter outLinkF = IOUtils.getBufferedWriter(args[6]);
		for (FrequencyData frequencyData : fd) {
			
			outLinkF.write(frequencyData.toString());
			outLinkF.newLine();
		}
		outLinkF.flush();
		outLinkF.close();
		
		final BufferedWriter outLinkW = IOUtils.getBufferedWriter(args[7]);
		for (WalkData walkData : wd) {
			
			outLinkW.write(walkData.toString());
			outLinkW.newLine();
		}
		outLinkW.flush();
		outLinkW.close();
        System.out.println("Finished all threads " + (System.currentTimeMillis() - x));
	}
	
	
	public class PTRouter extends Thread {			
		String stringToProcess;
		Provider<TransitRouter> transitRouterFactory;
		Scenario scenario;
		Set<RouteData> rd;
		Set<FrequencyData> fd;
		Set<WalkData> wd;
		String collumns;
		PTRouter(String s, Provider<TransitRouter> transitRouterFactory, Scenario scenario,
				Set<RouteData> rd, Set<FrequencyData> fd, Set<WalkData> wd, String collumns) {
			this.stringToProcess = s;			
			this.transitRouterFactory = transitRouterFactory;
			this.scenario = scenario;
			this.rd = rd;
			this.fd = fd;
			this.wd = wd;
			this.collumns = collumns;
		}
		
		public synchronized void addData(RouteData r) {
			rd.add(r);
		}
		
		public synchronized void addData(FrequencyData r) {
			fd.add(r);
		}
		
		public synchronized void addData(WalkData r) {
			wd.add(r);
		}
		
		public void run() {
			int timeStep = 60;
		    PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();

			TransitRouterWrapper routingModule = new TransitRouterWrapper(
	        		transitRouterFactory.get(),
	                scenario.getTransitSchedule(),
	                scenario.getNetwork(), // use a walk router in case no PT path is found
	                DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, scenario.getPopulation().getFactory(), 
					        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) ));
			
			
			
			NetworkLinkUtils lUtils = new NetworkLinkUtils(scenario.getNetwork());

			String[] arr = this.stringToProcess.split(";");
			String[] collArr = this.collumns.split(",");
			String tripId = arr[Integer.parseInt(collArr[0])]; 
			Person person = scenario.getPopulation().getFactory().
					createPerson(Id.createPersonId(tripId));

			Coord coordStart = new Coord(Double.parseDouble( arr[Integer.parseInt(collArr[1])]), 
					Double.parseDouble( arr[Integer.parseInt(collArr[2])]));
						
			Link lStart = lUtils.getClosestLink(coordStart);
						
			Coord coordEnd = new Coord(Double.parseDouble(arr[Integer.parseInt(collArr[3])]), 
					Double.parseDouble( arr[Integer.parseInt(collArr[4])]));
			
			Link lEnd = lUtils.getClosestLink(coordEnd);		
			
			double m = Integer.parseInt(arr[Integer.parseInt(collArr[5])]);
			final ActivityFacilities facilities = scenario.getActivityFacilities() ;
			final ActivityFacilitiesFactory ff = facilities.getFactory();

			ActivityFacility startFacility = ff.createActivityFacility(Id.create(0, ActivityFacility.class), coordStart, lStart.getId()) ;
			ActivityFacility endFacility = ff.createActivityFacility(Id.create(1, ActivityFacility.class), coordEnd, lEnd.getId());
			
			ArrayList<List<? extends PlanElement>> allRoutes = new ArrayList<List<? extends PlanElement>>();
			
			List<? extends PlanElement> route =  routingModule.calcRoute(startFacility, endFacility, m * 60, person);
			((Leg)route.get(0)).setDepartureTime(m * 60);
			
			System.out.println("routed for the initial time");
			
			double departureTime = m *60 + ((Leg)route.get(0)).getTravelTime();
			
			double travelTime = this.getTraveltime(route);			
			int numberOfTransfers = this.getNumberOfTransfers(route);
			
			double firstTime = 0.0;
			double lastTime = 0.0;
				
			int count = 0;
			
			ArrayList<List<? extends PlanElement>> allRoutesFirst = new ArrayList<List<? extends PlanElement>>();
			if(route.size() == 1) {
				WalkData walkData = new WalkData(tripId, 
						tripId, Double.toString(((Leg)route.get(0)).getTravelTime()) );
				this.addData(walkData);
			}
			
			if (route.size() != 1) {
				allRoutes.add(route);
				for (double time = departureTime + 7200; time >= departureTime - 7200; time -= timeStep) {
					
						List<? extends PlanElement> routeNew =  routingModule.calcRoute(startFacility, endFacility, time, person);
						
						double travelTimeNew = getTraveltime (routeNew);
							
						((Leg)routeNew.get(0)).setDepartureTime(time);
							
						if(!isDominatedWithTran(routeNew, allRoutes)) {
							if (routeNew.size() != 1) {
									
								allRoutesFirst.add(routeNew);	
								if (travelTimeNew < 1.30 * travelTime && numberOfTransfers + 2 > this.getNumberOfTransfers(routeNew) && 
										!isDominatedWithoutTran(routeNew, allRoutes)	
										) {
								
									allRoutes.add(routeNew);
									
								}
									
							}
						}
				}	
				
				double lastArrival = ((Leg)route.get(0)).getDepartureTime();
	
				int countTransfers = -1;
				
				double transferTime = 0.0;
				
				boolean writtenAccessTime = false;
				
				double egressTime = 0.0;
				
				double distance = 0.0;
				RouteData routeData = new RouteData();
				routeData.sethId(tripId);
				routeData.setTripId(tripId);
	
				for(PlanElement pe1: route) {
					
					if (pe1 instanceof Leg && ((Leg) pe1).getMode().equals("pt")) {
						countTransfers++;
						ExperimentalTransitRoute tr1 = ((ExperimentalTransitRoute)(((Leg)pe1).getRoute()));
						double temp = Double.MAX_VALUE;
						double earliestDepartureTime = Double.MAX_VALUE;
						double tempLastArrival = lastArrival > 24 * 3600.0 ? lastArrival - 24 * 3600.0 : lastArrival;
						for (Departure d: scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getDepartures().values()) {
							if (d.getDepartureTime() < earliestDepartureTime) {
								earliestDepartureTime = d.getDepartureTime();
							}
							double fromStopArrivalOffset = scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getStop(scenario.getTransitSchedule().getFacilities().get(tr1.getAccessStopId())).getDepartureOffset();
														
							if (d.getDepartureTime() + fromStopArrivalOffset >= tempLastArrival && d.getDepartureTime() + fromStopArrivalOffset < temp) {
								
								temp = d.getDepartureTime() + fromStopArrivalOffset;
								
							}
						}
						
						
							
						distance += ((Leg) pe1).getRoute().getDistance();
						
						if (temp == Double.MAX_VALUE) {
							double fromStopArrivalOffset = scenario.getTransitSchedule().getTransitLines().get(tr1.getLineId()).getRoutes().get(tr1.getRouteId()).getStop(scenario.getTransitSchedule().getFacilities().get(tr1.getAccessStopId())).getDepartureOffset();

							temp = earliestDepartureTime + fromStopArrivalOffset + 24 * 3600.0;
						}				
						
						double transfertTimePart = temp - lastArrival;
						if (lastArrival > 24 * 3600.0)
							transfertTimePart += 24 *3600.0;
						if (countTransfers == 0)
							routeData.setFirstWaiting(Double.toString(transfertTimePart));
						
						else
							
							transferTime += transfertTimePart;
							
						lastArrival +=  ((Leg) pe1).getTravelTime();
					}
					else if (pe1 instanceof Leg) {
						lastArrival += ((Leg) pe1).getTravelTime();
						
						if (!writtenAccessTime) {
							
							if (route.size() == 1) {
								routeData.setDepartureTime(Double.toString(((Leg) pe1).getDepartureTime()));
								routeData.setAccessTime(Double.toString(0.0));
							}						
							else{			
								routeData.setDepartureTime(Double.toString(((Leg) pe1).getDepartureTime()));
								routeData.setAccessTime(Double.toString(((Leg) pe1).getTravelTime()));
							}
							writtenAccessTime = true;
						}
						
						egressTime = ((Leg) pe1).getTravelTime();
						
					}
					
				}
	
				routeData.setTransferTime(Double.toString(transferTime));
				routeData.setNumberOfTransfers(Integer.toString(countTransfers));
			
				if (route.size() == 1)
					routeData.setEgressTime(Double.toString(0.0));
	
				else
					routeData.setEgressTime(Double.toString(egressTime));
				
				routeData.setTravlelTime(Double.toString(lastArrival - ((Leg)route.get(0)).getDepartureTime()));
				routeData.setDistance(Double.toString(distance));
				this.addData(routeData);			
				
				for (List<? extends PlanElement> routeIter : allRoutes) {
				
					lastArrival = ((Leg)routeIter.get(0)).getDepartureTime();
					
						if (routeIter.size() != 1) {
							if (lastTime == 0.0)
								lastTime = lastArrival;
							else if (lastTime < lastArrival)
								lastTime = lastArrival;
													
							if (firstTime == 0.0)
								firstTime = lastArrival;
							else if (firstTime > lastArrival)
								firstTime = lastArrival;
													
							count++;
						}				
				}
	
				if (count == 1) {
					FrequencyData frequencyData = new FrequencyData(tripId, tripId, "0");
					this.addData(frequencyData);
				}
				else {
					FrequencyData frequencyData = new FrequencyData(tripId, tripId,
							Double.toString((lastTime - firstTime)/(count - 1)));
					this.addData(frequencyData);

				}		
			}		
		}	
		
		public double getTraveltime(List<? extends PlanElement> route) {
			
			double travelTime = 0.0;
			
			for (PlanElement pe : route) {
				
				
				if (pe instanceof Leg) {
					
					travelTime += ((Leg) pe).getTravelTime();
					
				}
				
			}		
			
			return travelTime;
		}
			
		public int getNumberOfTransfers(List<? extends PlanElement> route) {
			int count = 0;
			
			for (PlanElement pe : route)
				if (pe instanceof Leg && ((Leg) pe).getMode().equals("pt")) {
					count++;
				}
			
			
			return -1 + count;
		}
		//simple check without checking if it is exactly the same route
		public boolean isDominatedWithTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
			
			for (List<? extends PlanElement> r : allRoutes) {
				
				if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
						((Leg)route.get(0)).getDepartureTime() + getTraveltime(route) && getNumberOfTransfers(r) == getNumberOfTransfers(route)) {
					
					
					return true;
					
				}
				
			}
			
			
			return false;
		}
		public boolean isDominatedWithoutTran(List<? extends PlanElement> route, ArrayList<List<? extends PlanElement>> allRoutes) {
			
			for (List<? extends PlanElement> r : allRoutes) {
				
				if (((Leg)r.get(0)).getDepartureTime() + getTraveltime(r) == 
						((Leg)route.get(0)).getDepartureTime() + getTraveltime(route)) {
					
					
					return true;
					
				}
				
			}
			
			
			return false;
		}
		
	}

}
