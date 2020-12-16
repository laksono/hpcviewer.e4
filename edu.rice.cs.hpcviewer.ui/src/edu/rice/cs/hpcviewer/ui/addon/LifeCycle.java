package edu.rice.cs.hpcviewer.ui.addon;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import edu.rice.cs.hpcviewer.ui.resources.IconManager;
import edu.rice.cs.hpcviewer.ui.util.ApplicationProperty;
import edu.rice.cs.hpcviewer.ui.util.IConstants;
import edu.rice.cs.hpcviewer.ui.util.Utilities;

@Creatable
@Singleton
public class LifeCycle 
{
	@Inject EPartService partService;
	@Inject IEventBroker broker;
	@Inject EModelService modelService;

	@Inject DatabaseCollection databaseCollection;

	private Image listImages[];

	@PostContextCreate
	public void startup() {
		
		// setup a list of images 
		// Note: this is for Windows. On mac, we don't need this.
		
		Display display = Display.getDefault();
		
		listImages = new Image[IconManager.Image_Viewer.length];
		int i = 0;
		
		for (String imageName : IconManager.Image_Viewer) {
			try {
				URL url = FileLocator.toFileURL(new URL(imageName));
				listImages[i] = new Image(display, url.getFile());
				i++;
			} catch (IOException e) {
			}
		}
		Window.setDefaultImages(listImages);
		
		// set the default location
		Location location = Platform.getInstanceLocation();
		
		// stop if location is set
		if (location.isSet()) {
			setUserLog();
			return;
		}

		final String workDir = Utilities.getWorkspaceDirectory();
		
		final File newLoc = new File(workDir);
		
		try {
			URL url  = newLoc.toURI().toURL(); 
			location.set(url, false);
			
		} catch (IllegalStateException | IOException e) {
			e.printStackTrace();
		}
		setUserLog();
	}
	
	
	
	@PreDestroy
	void preDestro() {
		if (listImages != null) {
			for(Image image: listImages) {
				if (!image.isDisposed())
					image.dispose();
			}
		}
	}

	@PreSave
	void preSave(IEclipseContext workbenchContext) {
	}

	@ProcessAdditions
	void processAdditions(IEclipseContext workbenchContext, MApplication app, EModelService modelService) {
	    MTrimmedWindow window = (MTrimmedWindow)modelService.find(IConstants.ID_MAIN_WINDOW, app);
		Rectangle screen = Display.getCurrent().getPrimaryMonitor().getClientArea();
		
		int x = (int) ((screen.width  - window.getWidth())  * 0.5);
		int y = (int) ((screen.height - window.getHeight()) * 0.5);
		window.setX(x);
		window.setY(y);
	}

	@ProcessRemovals
	void processRemovals(IEclipseContext workbenchContext) {}

	
	private void setUserLog() {
		String logFile = ApplicationProperty.getFileLogLocation();
		System.setProperty("log.name", logFile);
	}
}
