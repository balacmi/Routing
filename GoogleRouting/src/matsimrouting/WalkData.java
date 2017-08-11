package matsimrouting;

public class WalkData {

	String hId;	
	String tripId;
	String travelTime;
	public WalkData(String hId, String tripId, String travelTime) {
		this.hId = hId;
		this.tripId = tripId;
		this.travelTime = travelTime;
	}
	
	@Override
	public String toString() {
		return hId + ";" + tripId +";" + travelTime;
	}
}
