package wniemiec.mobilang.ama.framework;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import wniemiec.mobilang.ama.coder.exception.CoderException;
import wniemiec.mobilang.ama.export.exception.AppGenerationException;
import wniemiec.mobilang.ama.models.ProjectCodes;
import wniemiec.mobilang.ama.models.PropertiesData;
import wniemiec.mobilang.ama.models.ScreenData;


/**
 * Responsible for defining a framework compatible with Abstract Syntax Tree to 
 * Mobile Application (AMA) compiler. The framework class must be in a package
 * with its name in lower case.
 */
public interface Framework {

    /**
     * Creates a new framework project.
     * 
     * @param       propertiesData Application information
     * @param       location Path where the project will be created
     * 
     * @throws      IOException If project cannot be created
     */
    void createProject(PropertiesData propertiesData, Path location) 
    throws IOException;
  
    /**
     * Adds a dependency on the project.
     * 
     * @param       dependency Dependency name
     * @param       location Path where the project is
     * 
     * @throws      IOException If dependency cannot be added
     */
    void addProjectDependency(String dependency, Path location) 
    throws IOException;

    /**
     * Generates application code.
     * 
     * @param       screensData Information about application screens 
     * 
     * @return      Code files along with the necessary dependencies
     * 
     * @throws      CoderException If code cannot be generated
     */
    ProjectCodes generateCode(List<ScreenData> screensData) 
    throws CoderException;

    /**
     * Generates mobile application for a platform.
     * 
     * @param       platform Mobile platform that the application will be 
     * generated
     * @param       source Path where source code is
     * @param       output Path where mobile application will be created
     * 
     * @throws      AppGenerationException If mobile application cannot be 
     * created
     */
    void generateMobileApplicationFor(String platform, Path source, Path output) 
    throws AppGenerationException;
}