package au.org.emii.talend.codegen;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.exception.SystemException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.IService;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.components.IComponentsFactory;
import org.talend.core.model.general.Project;
import org.talend.core.model.genhtml.FileCopyUtils;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.User;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.model.RepositoryFactoryProvider;
import org.talend.designer.codegen.CodeGenInit;
import org.talend.designer.codegen.CodeGeneratorActivator;
import org.talend.designer.codegen.ICodeGeneratorService;
import org.talend.designer.codegen.ITalendSynchronizer;
import org.talend.designer.codegen.components.ui.IComponentPreferenceConstant;
import org.talend.designer.core.model.utils.emf.talendfile.ContextParameterType;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ItemCacheManager;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.documentation.ArchiveFileExportOperationFullPath;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.ui.wizards.exportjob.JavaJobExportReArchieveCreator;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobJavaScriptsManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;

public class Generator implements IApplication {
	private static Logger log = Logger.getLogger(Generator.class);
	
	private ProxyRepositoryFactory repository;
	
	private Project project; 

	@Override
    public Object start(IApplicationContext context) throws Exception {
		
		// Get details of job to export
		String jobName = Params.getMandatoryStringOption("-jobName");
		String projectDir = Params.getMandatoryStringOption("-projectDir");
		String targetDir = Params.getMandatoryStringOption("-targetDir");
		String version = Params.getStringOption("-version", "Latest");
		String componentDir = Params.getStringOption("-componentDir", "");
		
	    // Get export options 
		Map<ExportChoice, Object> exportChoiceMap = getExportOptions();

		// Set custom component directory
		
        CodeGeneratorActivator.getDefault().getPreferenceStore()
        .setValue(IComponentPreferenceConstant.USER_COMPONENTS_FOLDER, componentDir);

		log.info("Building " + jobName + "...");
		
		// Let talend services know we are running in headless mode
		// so they don't use ui stuff like messageboxes for exceptions
        CommonsPlugin.setHeadless(true);

        // Initialise connection to the local repository (the workspace) 
        repository = connectToRepository();
        
       	// Copy project into workspace
       	project = ProjectUtils.importProject(projectDir);

       	// Log on to project
       	log.info("Logging onto " + project.getLabel() + "...");

        repository.logOnProject(project, new NullProgressMonitor());
        
        //Initialise code generation engine
        log.info("Initialising code generation engine...");

        initCodeGenerationEngine();
        
        // Export the job
		exportJob(jobName, targetDir, version, exportChoiceMap);

		// Log off the project
		log.info("Logging off " + project.getLabel() + "...");
		
		repository.logOffProject();
		
		// All good
        return EXIT_OK;
    }

	@Override
    public void stop() {
		// TODO Auto-generated method stub
		
    }

	// Code generation engine must be initialised.  Initialisation loads java_jet template emitters
	// used for code generation
	private void initCodeGenerationEngine() throws Exception {
		CodeGenInit initialiser = new CodeGenInit();
        initialiser.init();
	}

	// Build export file
	private void exportJob(String jobName, String targetDir, String version,
			Map<ExportChoice, Object> exportChoiceMap) throws ProcessorException, InvocationTargetException, InterruptedException, SystemException, CoreException {

		log.info("Exporting " + jobName + "...");

		// Get job to build
		ProcessItem job = getJob(jobName, version);

        // Create the job script manager that performs the build
		JobJavaScriptsManager manager = new JobJavaScriptsManager(exportChoiceMap, "Default", "Unix", 
				IProcessor.NO_STATISTICS, IProcessor.NO_TRACES);
		
		manager.setDestinationPath(targetDir + "/" + job.getProperty().getLabel() + "_" + version + ".zip");
		manager.setContextEditableResultValuesList(new ArrayList<ContextParameterType>());
		manager.setMultiNodes(false);
		manager.setJobVersion(job.getProperty().getVersion());
		manager.setBundleVersion(job.getProperty().getVersion());
		manager.setProgressMonitor(null);
		
		try {
			List<ExportFileResource> resourcesToExport = generateExportResources(
					job, job.getProperty().getVersion(), manager);
	
			// Add resources built to archive
	        createArchive(manager.getDestinationPath(), resourcesToExport);
		} finally {
	        // Cleanup
	        manager.deleteTempFiles();
	        
	        ProcessorUtilities.resetExportConfig();
		}
	}

	// Generate resources to be exported
	private List<ExportFileResource> generateExportResources(
			ProcessItem job, String version, JobJavaScriptsManager manager)
			throws ProcessorException, SystemException, CoreException {
		
		List<ExportFileResource> processes = new ArrayList<ExportFileResource>();
		
		ExportFileResource resource = new ExportFileResource(job, job.getProperty().getLabel());
		
		processes.add(resource);
		
        List<ExportFileResource> resourcesToExport = manager.getExportResources(processes.toArray(new ExportFileResource[] {}));
        
        checkForErrors(job);
        
		return resourcesToExport;
	}

	private void checkForErrors(ProcessItem job) throws SystemException, CoreException {
        boolean error = false;

        ITalendSynchronizer synchronizer = CorePlugin.getDefault().getCodeGeneratorService().createRoutineSynchronizer();
        IFile sourceFile = synchronizer.getFile(job);
        
        // check whether the item had any compile errors when exporting the job
        IMarker[] markers = sourceFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
        
        for (IMarker marker : markers) {
            Integer lineNr = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
            String message = (String) marker.getAttribute(IMarker.MESSAGE);
            Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
            Integer start = (Integer) marker.getAttribute(IMarker.CHAR_START);
            Integer end = (Integer) marker.getAttribute(IMarker.CHAR_END);
            
            if (lineNr != null && message != null && severity != null && start != null && end != null) {
            	error = error || severity == IMarker.SEVERITY_ERROR;
            }
            
            String logEntry = message + " line number: " + lineNr.toString() + " start: " + start.toString() + " end: " + end.toString();
            
            if (severity == IMarker.SEVERITY_ERROR) {
            	log.error(logEntry);
            } else if (severity == IMarker.SEVERITY_WARNING) {
            	log.warn(logEntry);
            } else {
            	log.info(logEntry);
            }
        }
        
        if (error) {
        	throw new RuntimeException("Error building job.  Aborting...");
        }
	}
	
	// Add provided resources to archive
	private void createArchive(String destinationZipFile,
			List<ExportFileResource> resourcesToExport)
			throws InvocationTargetException, InterruptedException {
		
		String tempDestinationPath = JavaJobExportReArchieveCreator.getTmpDestinationFolder() + "/export.zip";
        ArchiveFileExportOperationFullPath op = new ArchiveFileExportOperationFullPath(resourcesToExport,
        		tempDestinationPath);
        
        op.setCreateLeadupStructure(true);
        op.setUseCompression(true);

        op.run(new NullProgressMonitor());
        
        String zipFile = tempDestinationPath;
        FileCopyUtils.copy(zipFile, destinationZipFile);
	}

	// Find specified version of job
	private ProcessItem getJob(String jobName, String version)
			throws PersistenceException {
		List<IRepositoryViewObject> processObjects = repository.getAll(
				project, ERepositoryObjectType.PROCESS, false, false);
		
		for (IRepositoryViewObject processObject : processObjects) {
			if (processObject.getLabel().equals(jobName)) {
				return ItemCacheManager.getProcessItem(processObject.getId(), version);
			}
		}
		
		return null;
	}

	// Initialise and connect to the local repository (workspace)
	private ProxyRepositoryFactory connectToRepository()
			throws PersistenceException {
		
		ProxyRepositoryFactory repositoryFactory = ProxyRepositoryFactory.getInstance();
        repositoryFactory.setRepositoryFactoryFromProvider(RepositoryFactoryProvider.getRepositoriyById("local")); //$NON-NLS-1$
        repositoryFactory.initialize();

        RepositoryContext repositoryContext = new RepositoryContext();
        repositoryContext.setUser(createUser());
        HashMap<String, String> fields = new HashMap<String, String>();
        repositoryContext.setFields(fields);

        Context ctx = CorePlugin.getContext();
        ctx.putProperty(Context.REPOSITORY_CONTEXT_KEY, repositoryContext);
        
		return repositoryFactory;
	}

	// Read export options from the command line
    private Map<ExportChoice, Object> getExportOptions() {
        Map<ExportChoice, Object> exportChoiceMap = new EnumMap<ExportChoice, Object>(ExportChoice.class);

        exportChoiceMap.put(ExportChoice.needLauncher, Params.getBooleanOption("-needLauncher", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needSystemRoutine, Params.getBooleanOption("-needSystemRoutine", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needUserRoutine, Params.getBooleanOption("-needUserRoutine", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needTalendLibraries, Params.getBooleanOption("-needTalendLibraries", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needJobItem, Params.getBooleanOption("-needJobItem", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needSourceCode, Params.getBooleanOption("-needSourceCode", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needDependencies, Params.getBooleanOption("-needDependencies", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needJobScript, Params.getBooleanOption("-needJobScript", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.needContext, Params.getBooleanOption("-needContext", Boolean.TRUE));
        exportChoiceMap.put(ExportChoice.applyToChildren, Params.getBooleanOption("-applyToChildren", Boolean.FALSE));
        exportChoiceMap.put(ExportChoice.setParameterValues, Params.getBooleanOption("-setParameterValues", Boolean.FALSE));
        
        return exportChoiceMap;
    }

    // Create a new user
   private User createUser() {
        User user = PropertiesFactory.eINSTANCE.createUser();
        user.setLogin("user@talend.com"); //$NON-NLS-1$
        return user;
    }
   
}
