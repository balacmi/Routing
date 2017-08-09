/**
 * The purpose of this package is to provide google based routing of OD pairs with different modes.
 * 
 * The input files should be a csv or txt file, with a header and ; separated values.
 * 
 * The output file contains the following information:
 * trip id, travel time, travel distance, true/false if the trip was routed with the requested mode
 * 
 * For pt routing the output file contains the following:
 * trip id, travel time in seconds,  true/false if the trip was routed with the requested mode
 * access time in seconds,  waiting time in seconds, transfer time in seconds, number of transfers
 * in vehicle travel time in seconds and egress time in seconds
 */
/**
 * @author balacm
 *
 */
package routing;