package io.opencaesar.docbook.adapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.IParameterValidator;
import com.google.common.io.CharStreams;

public class DocbookAdapterApp {
	@Parameter(
		names = { "--input", "-i" },
		description = "DocBook file to apply the XSLT to (Required)",
		required = true,
		order = 1)
	private String inputPath;
	
	@Parameter(
		names = { "--type", "-t" },
		description = "Type of operation. Options are pdf or html (Required)",
		validateWith = TypeValidator.class,
		required = true,
		order = 2)
	private String type;
	
	@Parameter(
		names = { "--xsl", "-x" },
		description = "Path to the required XSL. (Required) Different for each type: \n" +
				"For original render for tag/html, give the path of the original DocBook XSL: \n" +
				"PDF: should be located in path/to/dockbook_xsl/fo/docbook.xsl \n" + 
				"HTML: should be located in path/to/dockbook_xsl/html/dockbook.xsl \n",
		required = true,
		order = 3)
	private String xslPath;
	
	@Parameter(
		names = { "--output", "-o" },
		description = "Output filename (Required)",
		required = true,
		order = 4)
	private String outputPath = null;

	@Parameter(
			names = { "--css", "-c" },
			description = "Path to a CSS file to be used for html rendering. A default one is given in stylesheets-gen/default.css (Optional; only affects HTML)",
			required = false,
			order = 5)
	private String cssPath = "";
	
	@Parameter(
		names = { "-d", "--debug" },
		description = "Shows debug logging statements",
		order = 6)
	private boolean debug;

	@Parameter(
		names = { "--help", "-h" },
		description = "Displays summary of options",
		help = true,
		order =7)
	private boolean help;
	
	
	private final Logger LOGGER = LogManager.getLogger(DocbookAdapterApp.class); {
        DOMConfigurator.configure(ClassLoader.getSystemClassLoader().getResource("log4j.xml"));
	}
	
	public static void main(final String... args) {
		final DocbookAdapterApp app = new DocbookAdapterApp();
		final JCommander builder = JCommander.newBuilder().addObject(app).build();
		builder.parse(args);
		if (app.help) {
			builder.usage();
			return;
		}
		if (app.debug) {
			final Appender appender = LogManager.getRootLogger().getAppender("stdout");
			((AppenderSkeleton) appender).setThreshold(Level.DEBUG);
		}
	    try {
			app.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() throws Exception {
		LOGGER.info("=================================================================");
		LOGGER.info("                        S T A R T");
		LOGGER.info("                     DocBook Adapter " + getAppVersion());
		LOGGER.info("=================================================================");
		LOGGER.info("DocBook: " + inputPath);
		LOGGER.info("Output: " + outputPath);
		//Get DBTransformer class 
		DBTransformer trans = getTransformer(inputPath, outputPath, type);
		if (trans != null) {
			trans.apply();
		} else {
			LOGGER.error("Unable to apply transformation");
			System.exit(1);
		}
	    LOGGER.info("=================================================================");
		LOGGER.info("                          E N D");
		LOGGER.info("=================================================================");
	}
	
	/**
	 * Get application version id from properties file.
	 * 
	 * @return version string from build.properties or UNKNOWN
	 */
	public String getAppVersion() {
		String version = "UNKNOWN";
		try {
			InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("version.txt");
			InputStreamReader reader = new InputStreamReader(input);
			version = CharStreams.toString(reader);
		} catch (IOException e) {
			String errorMsg = "Could not read version.txt file." + e;
			LOGGER.error(errorMsg, e);
			System.exit(1);
		}
		return version;
	}
	
	//Validate type parameter
	public static class TypeValidator implements IParameterValidator {
		@Override
		public void validate(final String name, final String value) throws ParameterException {
			final List<String> validTypes = Arrays.asList("pdf", "html", "tag");
			if (!validTypes.contains(value.toLowerCase())) {
				throw new ParameterException("Paramter " + name + " must be either pdf, html, or tag");
			}
		}
	}
	
	//Get style sheet depending on task
	private DBTransformer getTransformer(String inputPath, String outputPath, String type) {
		//Use the inputPath's file name as the output's name
		File input = new File(inputPath); 
		if (!input.exists()) {
			LOGGER.error("Input doesn't exist at: " + inputPath); 
			return null;
		}
		switch (type.toLowerCase()) {
			case "pdf":
				return new PDFTransform(inputPath, xslPath, outputPath);
			case "html":
				return new HTMLTransform(inputPath, xslPath, outputPath, cssPath);
			default: 
				LOGGER.error(type + " is not a supported type. Please choose tag, pdf, or html");
				return null;
		}
	}
}
