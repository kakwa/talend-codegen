package au.org.emii.talend.codegen;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.PropertiesPackage;
import org.talend.core.repository.constants.FileConstants;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.utils.URIHelper;
import org.talend.core.repository.utils.XmiResourceManager;
import org.talend.repository.ui.actions.importproject.FilterFileSystemStructureProvider;
import org.talend.repository.ui.actions.importproject.ImportProjectsUtilities;
import org.talend.repository.ui.utils.AfterImportProjectUtil;

public class ProjectUtils {

    // Import project (refer org.talend.repository.ui.actions.importproject.ImportProjectsUtilities.importProjectAs
    // which is the TOS project import, but includes changing the name which we don't want here).
    	
    public static Project importProject(String projectDir) throws InvocationTargetException, InterruptedException, PersistenceException, CoreException, BusinessException, IOException {
        File directory = new File(projectDir);
        
        String technicalName = getTechnicalName(directory);

        // If project exists in repository already, delete it
//       	deleteIfExists(technicalName);
       	
        IImportStructureProvider provider = FilterFileSystemStructureProvider.INSTANCE;

        ArrayList fileSystemObjects = new ArrayList();
        ImportProjectsUtilities.getFilesForProject(fileSystemObjects, provider, directory);

        ImportOperation operation = new ImportOperation(new Path(technicalName), directory, provider, new MyOverwriteQuery(), fileSystemObjects);
        operation.setOverwriteResources(true);
        operation.setCreateContainerStructure(false);
        operation.run(new NullProgressMonitor());
        
        final IProject fsProject = ResourceUtils.getProject(technicalName);
        XmiResourceManager xmiManager = new XmiResourceManager();
        org.talend.core.model.properties.Project project = xmiManager.loadProject(fsProject);
        
        // do additional actions after importing projects
        AfterImportProjectUtil.runAfterImportProjectActions(new org.talend.core.model.general.Project(project));
        
        return getTalendProject(technicalName);
    }

	private static String getTechnicalName(File projectDir) throws IOException {
        XmiResourceManager xrm = new XmiResourceManager();
        Resource resource = xrm.resourceSet.getResource( URI.createFileURI(projectDir.getAbsolutePath() + File.separator + FileConstants.LOCAL_PROJECT_FILENAME), true);
        org.talend.core.model.properties.Project emfProject = (org.talend.core.model.properties.Project) EcoreUtil
                .getObjectByType(resource.getContents(), PropertiesPackage.eINSTANCE.getProject());
		return emfProject.getTechnicalLabel();
	}

	// Delete specified project from the repository if it exists 
	private static void deleteIfExists(String technicalName) throws CoreException {
		try {
			final IProject fsProject = ResourceUtils.getProject(technicalName);
			fsProject.delete(true, new NullProgressMonitor());
		} catch (PersistenceException e) {
			// Do nothing if the project doesn't exist
		}
	}
	
	// Get the specified talend project from the repository
	private static Project getTalendProject(String technicalName) throws PersistenceException, BusinessException {
		Project[] projects = ProxyRepositoryFactory.getInstance().readProject();
		
		for (Project project : projects) {
			if (project.getTechnicalLabel().equals(technicalName)) {
				return project;
			}
		}
		
		return null;
	}

    // Overwrite all files
    private static class MyOverwriteQuery implements IOverwriteQuery {
        public String queryOverwrite(String pathString) {
            return pathString;
        }
    }
    
}
