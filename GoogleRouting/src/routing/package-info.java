/**
 * The purpose of this package is to provide google based routing of OD pairs with different modes.
 * 
 * The input files should be a csv or txt file, with a header and ; separated values.
 * 
 * The output file contains the following information:
 * trip id, travel time, travel distance, true/false if the trip was routed with the requested mode, transfer time, 
 * number of transfers, in-vehicle time
 * 
 * transfer time,  number of transfers, in-vehicle time are only used for transit, for other modes it is set to 0
 */
/**
 * @author balacm
 *
 */
package routing;