package io.opencaesar.docbook.adapter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;

//Inheritance super class
abstract class DBTransformer{
	private File input; 
	private File style; 
	private File result;
	protected final Logger LOGGER;
	
	//Function to override
	public abstract void apply(); 	
	
	public DBTransformer(String inputPath, String stylePath, String resultPath) {
		LOGGER = LogManager.getLogger(DocbookAdapterApp.class);
		input = getFile(inputPath); 
		style = getFile(stylePath); 
		//Double check if input and result are the same (error if they are)
		result = new File(resultPath);
		if (result.getAbsoluteFile().equals(input.getAbsoluteFile())) {
			//Input and result are the same, return err msg and exit
			exitPrint("Error: Result and input are at the same location. Please change one"); 
		}
		//Create the resulting file and overwrite if it previously exists
		if (result.exists()) {
			result.delete(); 
		}
		try {
			result.createNewFile();
		} catch (IOException e) {
			LOGGER.error("Cannot create resulting file. Printing stack trace: \n ");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	//Getter functions
	public File getInput() {
		return input;
	}
	
	public File getStyle() {
		return style;
	}
	
	public File getResult() {
		return result;
	}
	
	//Helper functions 
	
	//Given a file path, return the file 
	public File getFile(String path) {
		File file = new File (path); 
		if (!file.exists()) {
			exitPrint("File does not exist at: " + path);
			return null;
		}
		return file;
	}
	
	//Print msg to log error and exit
	public void exitPrint(String msg) {
		LOGGER.error(msg);
		System.exit(1);
	}
	
	// Adds params to the transformer that are common to both renders
	public void addCommonParams(Transformer transformer) {
		// Section.autolabel turns on section numbering, and 1 means to use Arabic numerals
		transformer.setParameter("section.autolabel", "1");
		// Section.label.includes.component.label set to 1 includes the chapter number in the numbering
		transformer.setParameter("section.label.includes.component.label", "1");
	}
	
	/**
	 * Creates an empty params and calls on applyWithParams, which does the work
	 */
	public void applyTransformation(File input, File style, File res) {
		HashMap<String, String> params = new HashMap<String, String>(); 
		applyWithParams(input, style, res, params);
	}
	
	/**
	 * Applies an XSLT on a given xml file using the Saxon HE XSL processor and sets a global param used in the XSLT
	 * @param input File path of the input xml to apply the XSLT to
	 * @param style File path of the XSL that will be applied
	 * @param res File path of the resulting xml file
	 * @param params HashMap(String, String); key = paramName value = paramValue
	 */
	public void applyWithParams(File input, File style, File res, Map<String, String> params) {
		try {
			Configuration config = new Configuration(); 
			config.setXIncludeAware(true);
			Transformer transformer = new TransformerFactoryImpl(config)
					.newTransformer(new StreamSource(style));
			//Add params to transformer
			params.forEach((key, value) -> {
				transformer.setParameter(key, value);
			});
			addCommonParams(transformer);
			transformer.transform(new StreamSource(input), new StreamResult(res));
		} catch (TransformerException e) {
			LOGGER.error("Cannot apply transformation. Printing stack trace: \n");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
}
