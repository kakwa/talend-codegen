package au.org.emii.talend.codegen;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.talend.commons.CommonsPlugin;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.model.genhtml.FileCopyUtils;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.User;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryViewObject;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.model.RepositoryFactoryProvider;
import org.talend.core.repository.utils.XmiResourceManager;
import org.talend.designer.codegen.CodeGenInit;
import org.talend.designer.core.model.utils.emf.talendfile.ContextParameterType;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.repository.documentation.ArchiveFileExportOperationFullPath;
import org.talend.repository.documentation.ExportFileResource;
import org.talend.repository.ui.actions.importproject.FilterFileSystemStructureProvider;
import org.talend.repository.ui.actions.importproject.ImportProjectsUtilities;
import org.talend.repository.ui.utils.AfterImportProjectUtil;
import org.talend.repository.ui.wizards.exportjob.JavaJobExportReArchieveCreator;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobJavaScriptsManager;
import org.talend.repository.ui.wizards.exportjob.scriptsmanager.JobScriptsManager.ExportChoice;

public class Generator implements IApplication {
	private ProxyRepositoryFactory repository; 
	private Project project; 

	@Override
    public Object start(IApplicationContext context) throws Exception {
		
		// Get details of job to export
		String jobName = Params.getMandatoryStringOption("-jobName");
		String projectDir = Params.getMandatoryStringOption("-projectDir");
		String targetDir = Params.getMandatoryStringOption("-targetDir");
		String version = Params.getMandatoryStringOption("-version");
		
	    // Get export options 
		Map<ExportChoice, Object> exportChoiceMap = getExportOptions();
		
		// Let talend services know we are running in headless mode
		// so they don't use ui stuff like messageboxes for exceptions
        CommonsPlugin.setHeadless(true);

        // Initialise connection to the local repository (the workspace) 
        repository = connectToRepository();
        
       	// Copy project into workspace
       	project = ProjectUtils.importProject(projectDir);

       	// Log on to project
        System.out.println("Logging onto " + project.getLabel() + "...");

        repository.logOnProject(project, new NullProgressMonitor());
        
        //Initialise code generation engine
        System.out.println("Initialising code generation engine...");

        initCodeGenerationEngine();
        
        // Export the job
		exportJob(jobName, targetDir, version, exportChoiceMap);

		// Log off the project
        System.out.println("Logging off " + project.getLabel() + "...");
		
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
			Map<ExportChoice, Object> exportChoiceMap) throws PersistenceException,
			ProcessorException, InvocationTargetException, InterruptedException {

		System.out.println("Exporting " + jobName + "...");

		// Get job to build
        IRepositoryViewObject job = getJob(jobName, version);

        // Create the job script manager that performs the build
        
		JobJavaScriptsManager manager = new JobJavaScriptsManager(exportChoiceMap, "Default", "Unix", 
				IProcessor.NO_STATISTICS, IProcessor.NO_TRACES);
		
		manager.setDestinationPath(targetDir + "/" + job.getLabel() + "_" + version + ".zip");
		manager.setContextEditableResultValuesList(new ArrayList<ContextParameterType>());
		manager.setMultiNodes(false);
		manager.setJobVersion(version);
		manager.setBundleVersion(version);
		manager.setProgressMonitor(null);
		
		List<ExportFileResource> resourcesToExport = generateExportResources(
				job, manager);

        createArchive(manager.getDestinationPath(), resourcesToExport);
        
        manager.deleteTempFiles();
        
        ProcessorUtilities.resetExportConfig();

	}

	private List<ExportFileResource> generateExportResources(
			IRepositoryViewObject job, JobJavaScriptsManager manager)
			throws ProcessorException {
		
		List<ExportFileResource> processes = new ArrayList<ExportFileResource>();
		
		ProcessItem processItem = (ProcessItem) job.getProperty().getItem();
		ExportFileResource resource = new ExportFileResource(processItem, processItem.getProperty().getLabel());
		
		processes.add(resource);
		
        List<ExportFileResource> resourcesToExport = manager.getExportResources(processes.toArray(new ExportFileResource[] {}));
        
        // need to check for errors - but do this later - just trying to generate code at the moment
//        IStructuredSelection selection = new StructuredSelection(nodes);
//        // if job has compile error, will not export to avoid problem if run jobscript
//        boolean hasErrors = CorePlugin.getDefault().getRunProcessService().checkExportProcess(selection, true);
//        if (hasErrors) {
//            manager.deleteTempFiles();
//            return false;
//        }
        
		return resourcesToExport;
	}

	private void createArchive(String destinationZipFile,
			List<ExportFileResource> resourcesToExport)
			throws InvocationTargetException, InterruptedException {
		
		String tempDestinationPath = JavaJobExportReArchieveCreator.getTmpDestinationFolder() + "/export.zip";
        ArchiveFileExportOperationFullPath op = new ArchiveFileExportOperationFullPath(resourcesToExport,
        		tempDestinationPath);
        
        op.setCreateLeadupStructure(true);
        op.setUseCompression(true);

        op.run(new NullProgressMonitor());

//  Not sure what this does - leave it for the moment
//		        boolean generated = generatedCodes(version, monitor, processes);
//		        if (!generated) {
//		            return false;
//		        }
//
//		        boolean addClasspathJar = true;
//
//		        IDesignerCoreService designerCoreService = CoreRuntimePlugin.getInstance().getDesignerCoreService();
//
//		        if (designerCoreService != null) {
//		            addClasspathJar = designerCoreService.getDesignerCorePreferenceStore().getBoolean(
//		                    IRepositoryPrefConstants.ADD_CLASSPATH_JAR);
//		        }
//
//		        if (addClasspathJar) {
//		            reBuildJobZipFile(processes);
//		        } else {
        
            String zipFile = tempDestinationPath;
            FileCopyUtils.copy(zipFile, destinationZipFile);
//		        }
	}

	private IRepositoryViewObject getJob(String jobName, String version)
			throws PersistenceException {
		IRepositoryViewObject job = null;
        
		List<IRepositoryViewObject> processObjects = repository.getAll(
				project, ERepositoryObjectType.PROCESS, false, false);
		
		for (IRepositoryViewObject processObject : processObjects) {
			if (processObject.getLabel().equals(jobName) && processObject.getVersion().equals(version)) {
				job = processObject;
				break;
			}
		}
		return job;
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
