package it.dibris.unige.TExpSWIPrologConnector;

import java.util.Date;
import java.util.Set;

import org.jpl7.PrologException;
import org.jpl7.Query;

import it.dibris.unige.TExpSWIPrologConnector.exceptions.InitializationException;
import it.dibris.unige.TExpSWIPrologConnector.texp.TraceExpression;

public class Connector {
	
	private static long local_epoch = new Date().getTime();
	
	/**
	 * initialize the RV SWI-Prolog library for a single monitor
	 * 
	 * @param tExp the trace expression that will be used to guide the RV process
	 * @param logFileName the name of the log file will be generated
	 * @param monitorID the id of the monitor 
	 * 
	 * @throws PrologException if an error occurred during the execution of the query
	 */
	public static void initialize(TraceExpression tExp, String logFileName, String monitorID, Set<String> roleSet){
		//Set<String> roleSet = tExp.involvedRoles();
		String roles = "[";
		for(String role : roleSet){
			roles += role + ",";
		}
		roles = roles.substring(0, roles.length() - 1) + "]";
		System.out.println(roles);
		String t1 = "initialize('" + logFileName + "'," + "'" + monitorID + "'," + roles + ", " + tExp.getProtocolName() + ")";
        System.out.println(t1);
        Query q1 = null;
        try{
        	q1 = new Query(t1);
        	if(!q1.hasSolution()){
        		throw new InitializationException("Impossible to initialize the SWI-Prolog library for the monitor: " + monitorID);
        	}
        } catch(Exception e){
        	throw new InitializationException(e);
        } finally{
        	if(q1 != null){
        		q1.close();
        	}
        }
	}
	
	/**
	 * put the event in the queue (it will be checked by the verify method)
	 * 
	 * @param monitorID the monitor
	 * @param event the event
	 * 
	 * @throws PrologException if an error occurred during the execution of the query
	 */
	public static void remember(String monitorID, String event){
		Query q1 = null;
		try{
			String t1 = "remember(" + monitorID + "," + event + ")";
			q1 = new Query(t1);
			q1.hasSolution();
		} finally{
			if(q1 != null){
				q1.close();
			}
		}
	}
	
	/**
	 * verify if the monitor can accept the last event perceived
	 * 
	 * @param monitorID the monitor identifier
	 * @return true if the event is consistent, false otherwise
	 * 
	 * @throws PrologException if an error occurred during the execution of the query
	 */
	public static boolean verify(String monitorID){
		Query q = null;
		try{
			q = new Query("verify(" +
				monitorID + ","
				+ (System.currentTimeMillis() - local_epoch) + ")");
			return q.hasSolution();
		} finally{
			if(q != null){
				q.close();
			}
		}
	}
	
}
