package it.dibris.unige.TExpSWIPrologConnector.JPL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jpl7.Atom;
import org.jpl7.JPL;
import org.jpl7.PrologException;
import org.jpl7.Query;
import org.jpl7.Term;

import it.dibris.unige.TExpSWIPrologConnector.exceptions.EnvironmentVariableNotDefinedException;
import it.dibris.unige.TExpSWIPrologConnector.exceptions.JPLInitializationException;
import it.dibris.unige.TExpSWIPrologConnector.exceptions.JavaLibraryPathException;
import it.dibris.unige.TExpSWIPrologConnector.exceptions.PrologPredicateFailedException;

/**
 * This class handles all the communication between Java and SWI-Prolog through the use of the JPL library.
 * 
 * @author angeloferrando
 *
 */
public class JPLInitializer {
	
	/**
	 *  Path to the SWI-Prolog library folder (it is read from the environment variable SWI_LIB) 
	 */
	private static String swiplEnvVar;
	
	/**
	 * @return the swiplEnvVar
	 */
	public static String getSwiplEnvVar() {
		return swiplEnvVar;
	}

	/**
	 * @param swiplEnvVar the swiplEnvVar to set
	 */
	public static void setSwiplEnvVar(String swiplEnvVar) {
		JPLInitializer.swiplEnvVar = swiplEnvVar;
	}

	/**
	 * initialize the JPL environment
	 * 
	 * @throws PrologException if an error occurred during the communication with SWI-Prolog
	 * @throws FileNotFoundException if library.pl or decamon.pl files are not found
	 */
	public static void init() throws FileNotFoundException{
		/* Retrieve the SWI_LIB environment variable */
		swiplEnvVar = System.getenv("SWI_LIB");
		
		/* If it does not exist an exception is thrown */
		if(swiplEnvVar == null){
			throw new EnvironmentVariableNotDefinedException("SWI_LIB environment variable not defined");
		}		
		
		/* We need to add the SWI-Prolog Home to the path in order to use the JPL library */
		try{
			addLibraryPath(swiplEnvVar);
		} catch(Exception e){
			throw new JavaLibraryPathException("An error occured during the user path retrieval information process", e);
		}
		
		JPL.setTraditional();
		JPL.init();
		
		String pathToLibrary = "./src/main/resources/prolog-code/library.pl";
		String pathToDecAMon = "./src/main/resources/prolog-code/decamon.pl";
		
		/*
		 * - Check for nested dependency (jpl inside TExpSWIPrologConnector.jar)
		 * - Check get resource from the jar
		 * - Check consult of that resource (maybe creating tmp file for the consult)
		 * */
		
		URL libRes = JPLInitializer.class.getResource("/prolog-code/library.pl");
		URL decRes = JPLInitializer.class.getResource("/prolog-code/decamon.pl");
		
	
		//File lib = new File(JPLInitializer.class.getResource("/prolog-code/library.pl").getPath());
		//File dec = new File(JPLInitializer.class.getResource("/prolog-code/decamon.pl").getPath());
		if(libRes != null && decRes != null){
			pathToLibrary = libRes.getPath();
			pathToDecAMon = decRes.getPath();
			try {
				pathToLibrary = ExportResource("/prolog-code/", "library.pl");
			} catch (Exception e) {
				throw new FileNotFoundException("library.pl resource not found");
			}
			try {
				pathToDecAMon = ExportResource("/prolog-code/", "decamon.pl");
			} catch (Exception e) {
				throw new FileNotFoundException("decamon.pl resource not found");
			}
		} else{
			if(!new File(pathToLibrary).exists()){ 
			    throw new FileNotFoundException("library.pl not found");
			}
			
			if(!new File(pathToDecAMon).exists()){ 
			    throw new FileNotFoundException("decamon.pl not found");
			}
		}
		
		try{
			JPLInitializer.createAndCheck("consult", new Atom(pathToLibrary));
			JPLInitializer.createAndCheck("consult", new Atom(pathToDecAMon));
		} catch(PrologPredicateFailedException | PrologException e){
			throw new JPLInitializationException(e);
		}
	}
	
	/**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param resourceName ie.: "/SmartLibrary.dll"
     * @return The path to the exported resource
     * @throws Exception
     */
    static private String ExportResource(String resourcePath, String resourceName) throws Exception {
        BufferedReader br = null;
        //OutputStream resStreamOut = null;
        FileWriter fw = null;
        String jarFolder;
        try {
            InputStream stream = JPLInitializer.class.getResourceAsStream(resourcePath + resourceName);//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            
            if(stream == null){
        		throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
        	}
            
            jarFolder = new File(JPLInitializer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath().replace('\\', '/');
            br = new BufferedReader(new InputStreamReader(stream));
            fw = new FileWriter(jarFolder + "/" + resourceName);
            String line = br.readLine();
            while(line != null){
            	fw.write(line + "\n");
            	line = br.readLine();
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw ex;
        } finally {
            br.close();
            if(fw != null){ fw.close(); }
        }

        return jarFolder + "/" + resourceName;
    }

	/**
	 * convert a compound term to the corresponding list of terms
	 * @param term is the term that we want to convert
	 * @return the corresponding list of terms
	 */
	public static List<Term> fromCompoundToList(Term term){
		List<Term> l = new ArrayList<>();
		while(term.arity() > 1){
    		l.add(term.arg(1));
    		term = term.arg(2);
    	}
		return l;
	}
	
	/**
	 * Method used to execute a predicate represented as a String
	 * @param predicate is the string representing the predicate term to execute
	 * @return the object corresponding to the opened query (it can be used to retrieve all the information needed)
	 * 
	 * @throws PrologPredicateFailedException if the predicate fails
	 * @throws PrologException if an error occurred during the execution of the query
	 */
	public static Query createAndCheck(String predicate){
		Query query = new Query(predicate);
		if(!query.hasSolution()){
			throw new PrologPredicateFailedException(predicate + " predicate failed");
		}
		return query;
	}
	
	/**
	 * Method used to execute a predicate functor(term) 
	 * @param functor is the functor of the term
	 * @param arg is the term corresponding to the argument of the term
	 * @return the object corresponding to the opened query (it can be used to retrieve all the information needed)
	 * 
	 * @throws PrologPredicateFailedException if the predicate fails
	 * @throws PrologException if an error occurred during the execution of the query
	 */
	public static Query createAndCheck(String functor, Term arg){
		Query query = new Query(functor, arg);
		if(!query.hasSolution()){
			throw new PrologPredicateFailedException(functor + " " + arg + " predicate failed");
		}
		return query;
	}
	
	/**
	 * Method used to execute a predicate functor(term1, ..., termN) where terms = { term1, ..., termN } 
	 * @param functor is the functor of the term
	 * @param args are the terms corresponding to the arguments of the term
	 * @return the object corresponding to the opened query (it can be used to retrieve all the information needed)
	 * 
	 * @throws PrologPredicateFailedException if the predicate fails
	 * @throws PrologException if an error occurred during the execution of the query 
	 */
	public static Query createAndCheck(String functor, Term[] args){
		Query query = new Query(functor, args);
		if(!query.hasSolution()){
			throw new PrologPredicateFailedException(functor + args + " predicate failed");
		}
		return query;
	}
	
	/**
	 * Add the specified path to the java library path
	 *
	 * @param pathToAdd is the path to add
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	*/
	public static void addLibraryPath(String pathToAdd) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException{
	    final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	    usrPathsField.setAccessible(true);

	    //get array of paths
	    final String[] paths = (String[])usrPathsField.get(null);

	    //check if the path to add is already present
	    for(String path : paths) {
	        if(path.equals(pathToAdd)) {
	            return;
	        }
	    }

	    //add the new path
	    final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
	    newPaths[newPaths.length-1] = pathToAdd;
	    usrPathsField.set(null, newPaths);
	}
}
