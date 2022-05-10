package wniemiec.mobilang.ama.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Responsible for parsing MobiLang directives in screen behavior.
 */
public abstract class MobiLangDirectiveParser {

    //-------------------------------------------------------------------------
    //      Attributes
    //-------------------------------------------------------------------------
    private List<String> screenParameters;
    private List<String> parsedLines;

    
    //-------------------------------------------------------------------------
    //      Methods
    //-------------------------------------------------------------------------
    public final List<String> parse(List<String> lines) {
        parsedLines = new ArrayList<>();
        screenParameters = new ArrayList<>();

        for (String line : lines) {
            parseLine(line);
        }

        return parsedLines;
    }

    private void parseLine(String line) {
        String parsedLine = line;

        if (isMobiLangDirective(line)) {
            parsedLine = parseMobiLangDirective(line);
        }

        parsedLines.add(parsedLine);
    }

    private boolean isMobiLangDirective(String line) {
        return line.contains("mobilang:");
    }

    private String parseMobiLangDirective(String line) {
        String parsedLine = line;

        if (isScreenDirective(line)) {
            parsedLine = parseScreenDirective(line);
        }
        else if (isParamDirective(line)) {
            parsedLine = parseParamDirective(line);
        }
        else if (isInputDirective(line)) {
            parsedLine = parseInputDirective(line);
        }

        return parsedLine;
    }

    private boolean isScreenDirective(String line) {
        return line.contains("mobilang:screen:");
    }

    private String parseScreenDirective(String line) {
        if (line.matches(".+mobilang:screen:([A-z0-9-_]+\\?).+")) {
            return parseScreenDirectiveWithParameters(line);
        }

        return parseScreenDirectiveWithoutParameters(line);
    }

    private String parseScreenDirectiveWithParameters(String line) {
        Pattern pattern = Pattern.compile(".+mobilang:screen:([A-z0-9-_]+)\\?([^?\\/\\\\]+).+");
        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            return line;
        }

        String screenName = matcher.group(1);
        String directive = "\"mobilang:screen:([A-z0-9-_]+\\?).+;";
        Map<String, String> parameters = new HashMap<>();

        String rawParameters = matcher.group(2); // id=${data[item].id}&q=123
        for (String rawParameter : rawParameters.split("&")) {
            String[] terms = rawParameter.split("=");
            
            parameters.put(terms[0], terms[1]);
            screenParameters.add(terms[0]);
        }

        return line.replaceAll(directive, swapScreenDirectiveWithParametersFor(screenName, parameters));
    }

    protected abstract String swapScreenDirectiveWithParametersFor(String screenName, Map<String, String> parameters);

    private String parseScreenDirectiveWithoutParameters(String line) {
        Pattern pattern = Pattern.compile(".+mobilang:screen:([A-z0-9-_]+).+");
        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            return line;
        }

        String screenName = matcher.group(1);
        String directive = "mobilang:screen:" + screenName;

        return line.replace(directive, swapScreenDirectiveFor(screenName));
    }

    protected abstract String swapScreenDirectiveFor(String screenName);

    private boolean isParamDirective(String line) {
        return line.contains("mobilang:param:");
    }

    private String parseParamDirective(String line) {
        Pattern pattern = Pattern.compile(".+mobilang:param:([A-z0-9-_]+).+");
        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            return line;
        }

        String paramName = matcher.group(1);
        String directive = "\"mobilang:param:" + paramName + "\"";

        return line.replace(directive, replaceParamDirectiveWith(paramName));
    }

    protected abstract String replaceParamDirectiveWith(String paramName);

    private boolean isInputDirective(String line) {
        return line.contains("mobilang:input:");
    }

    private String parseInputDirective(String line) {
        Pattern pattern = Pattern.compile(".+mobilang:input:([A-z0-9-_]+).+");
        Matcher matcher = pattern.matcher(line);

        if (!matcher.matches()) {
            return line;
        }

        String inputId = matcher.group(1);
        String directive = "\"mobilang:input:" + inputId + "\"";

        return line.replace(directive, swapInputDirectiveFor(inputId));
    }

    protected abstract String swapInputDirectiveFor(String inputId);


    //-------------------------------------------------------------------------
    //      Getters
    //-------------------------------------------------------------------------
    public List<String> getScreenParameters() {
        return screenParameters;
    }
}