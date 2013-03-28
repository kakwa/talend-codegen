package au.org.emii.talend.codegen;

import org.eclipse.core.runtime.Platform;

public class Params {

	// Read a boolean option from the command line, returning the specified default if not found
	
	public static Boolean getBooleanOption(String option, Boolean defaultValue) {
		String  [] cmdLineArgs = Platform.getCommandLineArgs();

		for (int i = 0; i < cmdLineArgs.length; i++) {
			if (option.equalsIgnoreCase(cmdLineArgs[i])) { 
				String optionValue = null;
				
		        if (cmdLineArgs.length > i + 1) {
		        	optionValue = cmdLineArgs[i + 1];
		        }
		        
		        return Boolean.parseBoolean(optionValue);
		    }
		}
		
		return defaultValue;
	}

	// Read a string option from the command line throwing an exception if its not found
	
	public static String getMandatoryStringOption(String option) {
		String optionValue = getStringOption(option, null);
		
		if (optionValue == null) {
			throw new RuntimeException(option + " must be supplied");
		}
		
		return optionValue;
	};
	
	// Read a string option from the command line, returning the specified default if not found
	
	public static String getStringOption(String option, String defaultValue) {
		String  [] cmdLineArgs = Platform.getCommandLineArgs();

		for (int i = 0; i < cmdLineArgs.length; i++) {
			if (option.equalsIgnoreCase(cmdLineArgs[i])) { 
				String optionValue = null;
				
		        if (cmdLineArgs.length > i + 1) {
		        	optionValue = cmdLineArgs[i + 1];
		        }
		        
		        return optionValue;
		    }
		}
		
		return defaultValue;
	}

}
