
// Description: Java 25 Main for testing the CFSec schema database creation

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

package server.markhome.mcf.v3_1.cfsec.cfsecappsetup;

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

import javafx.application.Application;

import ch.qos.logback.classic.LoggerContext;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.text.StringEscapeUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.ConfigurableEnvironment;

import server.markhome.mcf.v3_1.cflib.*;
import server.markhome.mcf.v3_1.cflib.inz.Inz;
import server.markhome.mcf.v3_1.cflib.inz.InzPathEntry;
import server.markhome.mcf.v3_1.cflib.dbutil.*;

import server.markhome.mcf.v3_1.cfsec.cfsecpub.*;
import server.markhome.mcf.v3_1.cfsec.cfsecpubobj.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.*;
import server.markhome.mcf.v3_1.cfsec.cfsecpubobj.*;
import server.markhome.mcf.v3_1.cfsec.cfsec.buff.*;
import server.markhome.mcf.v3_1.cfsec.cfsecappsetup.fx.CFSecAppSetupFxApplication;

@SpringBootApplication
@ComponentScan(basePackages = {
    "server.markhome.mcf.v3_1.cfsec.cfsecappsetup.fx",   // if you have service beans here
    "server.markhome.mcf.v3_1.cfsec.cfsecappsetup.spring"   // if you have service beans here
})
@EnableAutoConfiguration(exclude = {
})
public class CFSecAppSetup
{
    private static final AtomicReference<Properties> systemProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> applicationProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userDefaultProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> userProperties = new AtomicReference<>(null);
    private static final AtomicReference<Properties> mergedProperties = new AtomicReference<>(null);

	public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(server.markhome.mcf.v3_1.cfsec.cfsecappsetup.CFSecAppSetup.class.getName());
	public static LoggerContext loggerContext = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
	public static String startingupMessage = CFSecAppSetup.class.getName() + " starting.";

    /**
     * Loads the application properties file from the application resources.
     */
    public static Properties getApplicationProperties() {
        if (applicationProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFSecAppSetup.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException(Inz.x("cfsecappsetup.ApplicationPropertiesNotFound"));
                }
            } catch (IOException e) {
                throw new RuntimeException(Inz.x("cfsecappsetup.CouldNotLoadApplicationProperties"), e);
            }
            applicationProperties.compareAndSet(null, props);
        }
        return applicationProperties.get();
    }

    /**
     * Loads the system properties, which hopefully haven't had the merge applied yet.
     */
    public static Properties getSystemProperties() {
        if (systemProperties.get() == null) {
            Properties props = new Properties();
            props.putAll(System.getProperties());
            systemProperties.compareAndSet(null, props);
        }
        return systemProperties.get();
    }
  
    /**
     * Loads the user default properties file from the application resources.
     */
    public static Properties getUserDefaultProperties() {
        if (userDefaultProperties.get() == null) {
            Properties props = new Properties();
            try (var in = CFSecAppSetup.class.getClassLoader().getResourceAsStream("user-default.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    throw new RuntimeException(Inz.x("cfsecappsetup.UserDefaultPropertiesNotFound"));
                }
            } catch (IOException e) {
                throw new RuntimeException(Inz.x("cfsecappsetup.FailedToLoadUserDefaultProperties"), e);
            }
            userDefaultProperties.compareAndSet(null, props);
        }
        return userDefaultProperties.get();
    }

    /**
     * Make sure the user properties file doesn't exist yet
     */
    public static Properties getUserProperties() {
        if (userProperties.get() == null) {
            Properties props = new Properties();
            File userFile = new File(System.getProperty("user.home"), ".cfsecappsrv.properties");
            if (userFile.exists()) {
				throw new CFLibUsageException(CFSecAppSetup.class,
					"getUserProperties",
					String.format("%1$s already exists - application initialization previously completed", userFile.getPath ( ) ), String.format ( "%1$s already exists - application initialization previously completed", userFile.getPath()));
                
			}
			userProperties.compareAndSet(null, props);
        }
        return userProperties.get();
    }

    /**
     * Merges the System and User properties, giving preference to the User properties.
     */
    public static Properties getMergedProperties() {
        if (mergedProperties.get() == null) {
            Properties merged = new Properties();
            merged.putAll(getApplicationProperties());
            merged.putAll(getUserDefaultProperties());
            merged.putAll(getSystemProperties());
            merged.putAll(getUserProperties());
            mergedProperties.compareAndSet(null, merged);
        }
        return mergedProperties.get();
    }

    public static void main(String[] args) {
        Inz.addPathEntry(new InzPathEntry("/opt/mcf/v3_1/java" + "/server.markhome.mcf.v3_1.cfsec.cfsecappsetup/src/main/resources/" + "/server.markhome.mcf.v3_1.cfsec.cfsecappsetup".replace(".","/") + "/langs"));

        // This weird looking cadence ensures that all the sub-property lists are prepared before getMergedProperties() is invoked, ensuring that any errors and exceptions along the way are thrown first and in predictable order
        Properties mergedProperties = getApplicationProperties();
        mergedProperties = getUserDefaultProperties();
        mergedProperties = getSystemProperties();
        mergedProperties = getUserProperties();
        mergedProperties = getMergedProperties();
        System.getProperties().putAll(mergedProperties);

        SpringApplication app = new SpringApplication(CFSecAppSetup.class);
        app.addInitializers((applicationContext) -> {
            ConfigurableEnvironment env = applicationContext.getEnvironment();
            env.getPropertySources().addLast(new org.springframework.core.env.PropertiesPropertySource("userProperties", userProperties.get()));
        });
		int finalvalue = 0xf00000;
		CFSecBuffSchema buffCFSec = new CFSecBuffSchema();
		finalvalue = buffCFSec.initClassMapEntries(finalvalue);
		buffCFSec.setCFSecSchema(buffCFSec);
		logStartupInfo(args);
		logger.info("Runtime class codes are " + 0xf00000 + ".." + (finalvalue-1) + " (" + (finalvalue-0xf00000-1) + " tables in total)");
        Application.launch(CFSecAppSetupFxApplication.class, args);
    }

	public static void logStartupInfo(String[] args) {
		logger.info(startingupMessage);
		String argsString = "";
		if (args != null && args.length > 0) {
			StringBuilder sb = new StringBuilder(argsString);
			for (int i = 0; i < args.length; i++) {
				sb.append("arg ").append(i).append(" = >>>").append(args[i]).append("<<<");
			}
			argsString = sb.toString();
		}
		String message = "Spring boot Application args >>>" + argsString + "<<<";
		logger.info("The CFSec Application Setup is starting with args >>>{}<<<", message);
		String currentDirectory = new File(".").getAbsolutePath();
		message = "Application current Directory is: >>>" + currentDirectory + "<<<";
		logger.info(message);
	}

}


