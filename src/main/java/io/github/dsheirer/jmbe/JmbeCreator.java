/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.jmbe;

import io.github.dsheirer.jmbe.github.Asset;
import io.github.dsheirer.jmbe.github.GitHub;
import io.github.dsheirer.jmbe.github.Release;
import io.github.dsheirer.util.FileUtil;
import io.github.dsheirer.util.OSType;
import io.github.dsheirer.util.ThreadPool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.io.FileUtils;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates the JMBE library by downloading the JMBE Creator CLI application from GitHub and executing it.
 */
public class JmbeCreator
{
    private final static Logger mLog = LoggerFactory.getLogger(JmbeCreator.class);
    public static final String GITHUB_JMBE_RELEASES_URL = "https://api.github.com/repos/dsheirer/jmbe/releases";
    public static final String CREATOR_SCRIPT_LINUX = "creator";
    public static final String CREATOR_SCRIPT_WINDOWS = "creator.bat";

    private StringProperty mConsoleOutput = new SimpleStringProperty();
    private BooleanProperty mCompleteProperty = new SimpleBooleanProperty();
    private boolean mHasErrors = false;
    private Release mRelease;
    private Path mLibraryPath;
    private StringBuilder mConsoleStringBuilder = new StringBuilder();

    /**
     * Creates an instance.
     * @param release for the version to create
     * @param library path where the library should be created
     */
    public JmbeCreator(Release release, Path library)
    {
        mRelease = release;
        mLibraryPath = library;
    }

    /**
     * Path to where the library should be created
     */
    public Path getLibraryPath()
    {
        return mLibraryPath;
    }

    /**
     * Property that indicates if the creation process has finished.
     */
    public BooleanProperty completeProperty()
    {
        return mCompleteProperty;
    }

    /**
     * Console output from the build process.
     */
    public StringProperty consoleOutputProperty()
    {
        return mConsoleOutput;
    }

    /**
     * Prints (adds) the message text to the console property
     * @param message to add
     */
    private void printToConsole(String message)
    {
        mConsoleStringBuilder.append(message).append("\n");
        final String console = mConsoleStringBuilder.toString();
        Platform.runLater(() -> consoleOutputProperty().setValue(console));
    }

    /**
     * Indicates if the build process completed successfully (false) or there were errors (true).
     */
    public boolean hasErrors()
    {
        return mHasErrors;
    }

    /**
     * Executes the build process that downloads the JMBE creator utility, unzips it, and commands the utility
     * to create the library at the specified library path.
     */
    public void execute()
    {
        final Asset asset = getJMBECreatorAsset(mRelease);

        if(asset != null)
        {
            ThreadPool.CACHED.execute(() -> {
                Path tempDirectory = null;
                try
                {
                    tempDirectory = Files.createTempDirectory("sdrtrunk-jmbe-creator");
                    printToConsole("Created: Temp Directory [" + tempDirectory.toString() + "]");
                    printToConsole("Downloading: JMBE Creator [" + asset.toString() + "]");
                    printToConsole("Please wait ...");
                    Path creator = GitHub.downloadArtifact(asset.getDownloadUrl(), tempDirectory);

                    if(creator != null)
                    {
                        printToConsole("Downloaded: JMBE Creator [" + creator.toString() + "]");
                        Path unzipped = creator.getParent();
                        Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.ZIP);
                        archiver.extract(creator.toFile(), unzipped.toFile());
                        printToConsole("Unzipped: [" + unzipped.toString() + "]");

                        Path script = null;

                        OSType osType = OSType.getCurrentOSType();

                        if(osType.isLinux() || osType.isOsx())
                        {
                            script = FileUtil.findFile(unzipped, CREATOR_SCRIPT_LINUX);
                        }
                        else if(osType.isWindows())
                        {
                            script = FileUtil.findFile(unzipped, CREATOR_SCRIPT_WINDOWS);
                        }


                        if(script != null)
                        {
                            ProcessBuilder processBuilder = new ProcessBuilder();
                            processBuilder.command(script.toString(), mLibraryPath.toString());

                                try
                                {
                                    Process process = processBuilder.start();
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                                    String line;
                                    while((line = reader.readLine()) != null)
                                    {
                                        printToConsole(line);
                                    }

                                    int exitCode = process.waitFor();

                                    if(exitCode == 0)
                                    {
                                        printToConsole("Library Created Successfully!");
                                        Platform.runLater(() -> completeProperty().set(true));
                                    }
                                    else
                                    {
                                        terminateWithErrors("Failed: Exit Code [" + exitCode + "]");
                                        mLog.error("Script failed with exit code: " + exitCode);
                                    }
                                }
                                catch(InterruptedException ie)
                                {
                                    terminateWithErrors("Failed: Script Process was interrupted");
                                    mLog.error("Interrupted", ie);
                                }
                        }
                        else
                        {
                            terminateWithErrors("Failed: Unable to find JMBE creator launch script for this OS");
                            mLog.error("Script was null.  Unable to find JMBE creator launch script");
                        }
                    }
                }
                catch(Throwable t)
                {
                    terminateWithErrors("Failed: Unknown Error - " + t.getLocalizedMessage());
                    mLog.error("Failed to create the JMBE library", t);
                }
                finally
                {
                    if(tempDirectory != null)
                    {
                        try
                        {
                            FileUtils.deleteDirectory(tempDirectory.toFile());
                            printToConsole("Deleted: Temporary Directory [" + tempDirectory.toString() + "]");
                        }
                        catch(IOException ioe)
                        {
                            printToConsole("Failed: Deleting Temporary Directory [" + tempDirectory.toString() + "]");
                            mLog.error("Error deleting temporary directory [" + tempDirectory.toString() + "]");
                        }
                    }
                }
            });
        }
        else
        {
            terminateWithErrors("Failed: Unable to identify correct JMBE creator utility from GitHub " +
                    "repository for this computer's operating system and architecture");
            mLog.error("Unable to create JMBE library.  Can't find JMBE Creator for this OS and architecture");
        }


    }


    /**
     * Terminates the execution and updates flags to indicate error state
     * @param message to display as console output
     */
    private void terminateWithErrors(String message)
    {
        printToConsole(message);
        printToConsole("Please follow the instructions for manually creating and installing the JMBE library");
        Platform.runLater(() -> {
            mHasErrors = true;
            mCompleteProperty.set(true);
        });
    }

    /**
     * Attempts to find the correct JMBE creator for this operating system and architecture for the specified release
     * @param release to find
     * @return JMBE creator asset or null.
     */
    public static Asset getJMBECreatorAsset(Release release)
    {
        OSType osType = OSType.getCurrentOSType();

        if(release != null)
        {
            for(Asset asset: release.getAssets())
            {
                if(isCorrectAsset(asset, osType))
                {
                    return asset;
                }
            }
        }

        return null;
    }

    /**
     * Indicates if the asset is correct for the host operating system and architecture
     * @param asset to check
     * @param osType for the current host (OS & architecture)
     * @return true if the asset is correct for this host.
     */
    public static boolean isCorrectAsset(Asset asset, OSType osType)
    {
        if(isJMBECreator(asset))
        {
            String name = asset.getName();

            switch(osType)
            {
                case LINUX_AARCH_64:
                    return name.contains("linux") && name.contains("aarch64");
                case LINUX_ARM_32:
                    return name.contains("linux") && name.contains("arm32");
                case LINUX_X86_32:
                    return name.contains("linux") && name.contains("_32");
                case LINUX_X86_64:
                    return name.contains("linux") && name.contains("_64");
                case OSX_AARCH_64:
                    return name.contains("osx") && name.contains("aarch64");
                case OSX_X86_64:
                    return name.contains("osx") && name.contains("_64");
                case WINDOWS_AARCH_64:
                    return name.contains("windows") && name.contains("aarch64");
                case WINDOWS_X86_32:
                    return name.contains("windows") && name.contains("_32");
                case WINDOWS_X86_64:
                    return name.contains("windows") && name.contains("_64");
                case UNKNOWN:
                default:
                    return false;
            }
        }

        return false;
    }

    /**
     * Indicates if the GitHub asset has a non-null asset name and is a JMBE Creator asset
     */
    private static boolean isJMBECreator(Asset asset)
    {
        return asset.getName() != null && asset.getName().startsWith("jmbe-creator");
    }

    public static void main(String[] args)
    {
        Release release = GitHub.getLatestRelease(GITHUB_JMBE_RELEASES_URL);
        Path library = Paths.get("/home/denny/temp/jmbe.jar");
        JmbeCreator jmbeCreator = new JmbeCreator(release, library);

        jmbeCreator.execute();

        while(true);
    }
}
