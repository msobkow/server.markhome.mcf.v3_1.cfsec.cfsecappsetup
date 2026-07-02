
// Description: JavaFX Application Setup entry point

/*
 *	server.markhome.mcf.CFSec
 *
 *	Copyright (c) 2016-2026 Mark Stephen Sobkow
 *	
 *	Mark's Code Fractal 3.1 CFSec - Security Services
 *	
 *	Copyright (c) 2016-2026 Mark Stephen Sobkow mark.sobkow@gmail.com
 *	
 *	These files are part of Mark's Code Fractal CFSec.
 *	
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 *	
 */

package server.markhome.mcf.v3_1.cfsec.cfsecappsetup.fx;

import java.lang.reflect.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.rmi.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.text.StringEscapeUtils;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import server.markhome.mcf.v3_1.cfsec.cfsecappsetup.CFSecAppSetup;

import server.markhome.mcf.v3_1.cflib.*;
import server.markhome.mcf.v3_1.cflib.inz.Inz;
import server.markhome.mcf.v3_1.cflib.inz.InzPathEntry;
import server.markhome.mcf.v3_1.cflib.dbutil.*;

import server.markhome.mcf.v3_1.cfsec.cfsecpub.*;
import server.markhome.mcf.v3_1.cfsec.cfsecpubobj.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.*;
import server.markhome.mcf.v3_1.cfsec.cfsecpubobj.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.buff.*;
import server.markhome.mcf.v3_1.cfsec.cfsecappsetup.CFSecAppSetup;

@Component
public class CFSecAppSetupFxApplication extends Application {

	public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(server.markhome.mcf.v3_1.cfsec.cfsecappsetup.CFSecAppSetup.class.getName());

	public static String INIT_LOG_MESSAGE1 = "The CFSecAppSetupFxApplication init method is called";

	private static CFSecAppSetupFxApplication instance = null;

	private Parent rootNode = null;

	private Scene scene;

	private Stage stage = null;

	@Override
	public void init() throws Exception {
		//Only initialize once
		if (getInstance() != null ) {
			return;

		}
		logMessage(INIT_LOG_MESSAGE1);

		ApplicationContextInitializer<GenericApplicationContext> initializer = ac -> {
			ac.registerBean(Application.class, () -> CFSecAppSetupFxApplication.this);
			ac.registerBean(Parameters.class, () -> getParameters());
			ac.registerBean(HostServices.class, () -> getHostServices());
		};

		this.context = new SpringApplicationBuilder().sources(CFSecAppSetup.class).initializers(initializer)
				.run(getParameters().getRaw().toArray(new String[0]));

		// provide a handle for the testing framework to inspect
		setInstance(this);
	}

	//This method exists so that the correct behavior and existence of the logger can be tested separately from within a junit test
	// because this class CFSecAppSetupFxApplication extends Application the failure of the logger may be more likely than POJOs
	public static void logMessage(String message) {
		log.info(message);
	}

	private void setUpParameterOverride() {
		Map<String, String> parameterMap;
		Parameters javaFxParameters = getParameters();
		if (javaFxParameters != null) {
			parameterMap = javaFxParameters.getNamed();
			for (String key : parameterMap.keySet()) {
				switch (key) {
				case "interpretation" -> javaFxResource
						.setInterpretation(StreamInterpretation.valueOf(parameterMap.get("interpretation")));
				case "defaultResource" -> javaFxResource.setDefaultResource(parameterMap.get("defaultResource"));
				default -> {}
				}
			}
		}
	}

	@Override
	public void start(Stage stage) {
		String logRootPropertyString = System.getProperty("LOG_ROOT");
		String logRootPropertyMessage ="The CFSecAppSetupFxApplication sees System Property LOG_ROOT: >>>"+logRootPropertyString+"<<<";
		logMessage(logRootPropertyMessage);
		// Save the stage
		setStage(stage);
		// Set the stage to not always on top
		stage.setAlwaysOnTop(false);
		// Set the title of the stage
		stage.setTitle("CFSec Application Setup");
		// Make the stage available through javaFxResource
		javaFxResource.setStage(getStage());
		double sceneMargin = 5; // some margin at the edge of the scene
		String message = "The CFSecAppSetupFxApplication start method is called";
		log.info( message);
//		getLoader().setControllerFactory(context::getBean);
		setUpParameterOverride();
		rootNode = getRoot();

		double defaultWidth = 100;
		double defaultHeight = 100;
		if (rootNode instanceof Region) {
			defaultWidth = ((Region) rootNode).getPrefWidth() + sceneMargin;
			defaultHeight = ((Region) rootNode).getPrefHeight() + sceneMargin;
		}
		// Override the preferences if there is a resource setting
		double fxResourceWidth = javaFxResource.getRootSceneWidth();
		double fxResourceHeight = javaFxResource.getRootSceneHeight();
		defaultWidth = (fxResourceWidth != 0.0) ? defaultWidth : fxResourceWidth;
		defaultHeight = (fxResourceHeight != 0.0) ? defaultHeight : fxResourceHeight;
		scene = new Scene(rootNode, defaultWidth, defaultHeight);
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop() throws Exception {
		stage.close();
		setInstance(null);
		context.close();
		System.gc();
		System.runFinalization();
		Platform.exit();
	}

	public Parent getRoot() {
		if (javaFxResource != null) {
		logMessage("getRoot javaFxResource is not null");
		} else {
			logMessage("getRoot javaFxResource is null");
		}
		return getRoot(javaFxResource.getDefaultResourceStream());
	}

	public static Parent getRoot(InputStream inputStream) {
		if (inputStream == null) {
			String message = "The input stream is null.";
			throw new MmsRuntimeException(message);
		}
		Parent result = null;
		try {
			FXMLLoader aLoader = getInstance().getLoader();
			result = aLoader.getRoot();
			if (JavaFxResource.locationUrl != null) {
				URL aLocation = JavaFxResource.locationUrl;
				aLoader.setLocation(aLocation);
			}
			if (result == null) {
				Parent layout = (Parent) aLoader.load(inputStream);
				result = layout;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new MmsRuntimeException(e.getMessage());
		}
		return result;
	}

	public static CFSecAppSetupFxApplication getInstance() {
		return instance;
	}

	public static void setInstance(CFSecAppSetupFxApplication instance) {
		CFSecAppSetupFxApplication.instance = instance;
	}

	public void main(String[] args) {
		SpringApplication.run(CFSecAppSetupFxApplication.class, args);
	}
}

