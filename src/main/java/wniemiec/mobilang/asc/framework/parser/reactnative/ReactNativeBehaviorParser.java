package wniemiec.mobilang.asc.framework.parser.reactnative;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wniemiec.mobilang.asc.models.Tag;
import wniemiec.mobilang.asc.models.Variable;
import wniemiec.mobilang.asc.models.behavior.Instruction;
import wniemiec.mobilang.asc.parser.exception.ParseException;
import wniemiec.mobilang.asc.parser.html.HtmlParser;
import wniemiec.mobilang.asc.parser.screens.structure.StructureParser;


/**
 * Responsible for parsing screen behavior of React Native framework.
 */
class ReactNativeBehaviorParser {

    //-------------------------------------------------------------------------
    //		Attributes
    //-------------------------------------------------------------------------
    private List<Variable> stateDeclarations;
    private List<String> stateBody;
    private List<String> declaredStateBodyVariables;
    private List<Instruction> behaviorCode;

    /**
     * key:     Variable id
     * value:   Tag id
     */
    private Map<String, String> symbolTable;

    private Tag structure;
    private ReactNativeStyleApplicator styleApplicator;


    //-------------------------------------------------------------------------
    //		Constructor
    //-------------------------------------------------------------------------
    public ReactNativeBehaviorParser(
        List<Instruction> behaviorCode,
        Tag structure,
        ReactNativeStyleApplicator styleApplicator
    ) {
        this.behaviorCode = behaviorCode;
        this.structure = structure;
        this.styleApplicator = styleApplicator;
        stateDeclarations = new ArrayList<>();
        stateBody = new ArrayList<>();
        symbolTable = new HashMap<>();
        declaredStateBodyVariables = new ArrayList<>();
    }


    //-------------------------------------------------------------------------
    //		Methods
    //-------------------------------------------------------------------------
    public void parse() throws IOException, ParseException {
        parseCode();
        parseDeclarations();
    }

    private void parseCode() throws IOException, ParseException {
        for (Instruction instruction : behaviorCode) {
            for (String line : extractCodeLines(instruction)) {
                parseCodeLine(line);
            }
        }
    }

    private String[] extractCodeLines(Instruction instruction) {
        return instruction.toCode().split("\n");
    }
    
    private void parseCodeLine(String code) throws IOException, ParseException {
        // TODO: Add compatibility with getElementsByClass or byQuery

        String parsedCode = code;

        if (isWindowLocationHref(parsedCode)) {
            parsedCode = parseWindowLocationHref(parsedCode);
        }
        else if (isInnerHtml(parsedCode)) { 
            parsedCode = parseInnerHtml(parsedCode);
        }

        if (isDeclarationWithGetElementById(parsedCode)) {
            parsedCode = parseDeclarationWithGetElementById(parsedCode);
        }
        else if (isCallFromGetElementById(parsedCode)) {
            parsedCode = parseCallFromGetElementById(parsedCode);
        }

        if (isDeclaration(parsedCode)) {
            parseDeclaration(parsedCode);
        }

        if (!parsedCode.isEmpty()) {
            stateBody.add(parsedCode);
        }
    }

    private boolean isWindowLocationHref(String code) {
        return code.contains("window.location.href");
    }

    private String parseWindowLocationHref(String code) {
        return code.replace("window.location.href", "props.route.params.query");
    }

    private boolean isInnerHtml(String code) {
        return code.contains("innerHTML");
    }

    /**
     * Parses inner html code using the following approach:
     * 
     *     <id> = <attribution>
     * 1. if <id> is in symbolTable
     * 2. then replace <id> by its content obtained from symbolTable
     * 3. create '<id>' as list if it has not been created
     * 4. create local var '_<id>' as list if it has not been created
     * 5. '_<id>'.push(<attribution>) 
     * 6. set<id>(_<id>)
     * 
     * @param       code Inner html code
     * 
     * @return      Parsed code
     * 
     * @throws      IOException If code is invalid
     * @throws      ParseException If parsing failed
     */
    private String parseInnerHtml(String code) throws IOException, ParseException {
        String htmlTarget = extractInnerHtmlTarget(code);
        String targetTagId = parseInnerHtmlTarget(htmlTarget, code);
        String normalizedHtmlTarget = normalizeIdentifier(htmlTarget);
        
        parseInnerHtmlVariable(normalizedHtmlTarget, targetTagId);
        parseInnerHtmlAssignment(normalizedHtmlTarget, code);

        return "";
    }

    private String extractInnerHtmlTarget(String parsedCode) {
        return parsedCode.split(".innerHTML")[0];
    }

    private String parseInnerHtmlTarget(String htmlTarget, String code) {
        Variable stateVar = buildStateVariable(htmlTarget);

        if (!stateDeclarations.contains(stateVar)) {
            stateDeclarations.add(stateVar);
        }

        return getTagIdFromHtmlTarget(htmlTarget, code);
    }

    private Variable buildStateVariable(String tagId) {
        String normalizedTagId = normalizeIdentifier(tagId);
        
        return new Variable(normalizedTagId, "state", "[]");
    }

    private String normalizeIdentifier(String identifier) {
        return identifier.replace("-", "_");
    }

    private String getTagIdFromHtmlTarget(String htmlTarget, String code) {
        if (symbolTable.containsKey(htmlTarget)) {
            return symbolTable.get(htmlTarget);
        }
        
        return extractIdFromGetElementById(code);
    }

    private String extractIdFromGetElementById(String line) {
        String id = "";
        Pattern p = Pattern.compile(".*(getElementById\\(\\\"(.+)\\\"\\)).*");
        Matcher m = p.matcher(line);

        if (m.matches()) {
            id = m.group(2);
        }

        return id;
    }
    
    private void parseInnerHtmlVariable(String htmlTarget, String targetTagId) {
        if (wasHtmlTargetDeclared(htmlTarget)) {
            return;
        }

        String variableDeclaration = buildHtmlTargetVariable(htmlTarget);
        String variableAttribution = buildHtmlTargetAttribution(targetTagId);

        declaredStateBodyVariables.add(variableDeclaration);
        stateBody.add(variableDeclaration + variableAttribution);
    }

    private String buildHtmlTargetAttribution(String targetTagId) {
        StringBuilder code = new StringBuilder();
        Tag referedTag = structure.getTagWithId(targetTagId);

        code.append("=[");
        code.append(referedTag.toChildrenCode());
        code.append(']');

        return code.toString();
    }

    private boolean wasHtmlTargetDeclared(String htmlTarget) {
        String variableDeclaration = buildHtmlTargetVariable(htmlTarget);

        return declaredStateBodyVariables.contains(variableDeclaration);
    }

    private String buildHtmlTargetVariable(String htmlTarget) {
        return "let _" + htmlTarget;
    }

    private void parseInnerHtmlAssignment(String htmlTarget, String parsedCode) 
    throws IOException, ParseException {
        String innerHtmlAssignment = extractInnerHtmlAssignment(parsedCode);
        String parsedHtmlAssignment = parseInnerHtmlAssignment(innerHtmlAssignment); 
        
        if (parsedCode.contains(".innerHTML=")) { 
            stateBody.add(buildInnerHtmlDeclarationCode(htmlTarget, parsedHtmlAssignment));
        }
        else if (parsedCode.contains(".innerHTML+=")) { 
            stateBody.add(buildInnerHtmlAppendCode(htmlTarget, parsedHtmlAssignment));
        }
    }

    private String buildInnerHtmlDeclarationCode(String target, String assignment) {
        StringBuilder code = new StringBuilder();

        code.append('_');
        code.append(target);
        code.append("=[");
        code.append(assignment);
        code.append(']');

        return code.toString();
    }

    private String buildInnerHtmlAppendCode(String target, String assignment) {
        StringBuilder code = new StringBuilder();

        code.append('_');
        code.append(target);
        code.append(".push(");
        code.append(assignment);
        code.append(')');

        return code.toString();
    }

    private String extractInnerHtmlAssignment(String parsedCode) {
        return parsedCode.substring(parsedCode.indexOf("=")+1);
    }

    private String parseInnerHtmlAssignment(String innerHtmlAssignment) 
    throws IOException, ParseException {
        if (isEmptyString(innerHtmlAssignment)) {
            return "";
        }

        Tag htmlTag = parseHtml(innerHtmlAssignment);
        Tag reactNativeTag = convertHtmlToReactNative(htmlTag);

        return buildTagCode(reactNativeTag);
    }

    private boolean isEmptyString(String innerHtmlAssignment) {
        return innerHtmlAssignment.matches("\"[\\s\\t]*\"");
    }

    private Tag parseHtml(String html) throws IOException, ParseException {
        String ast = buildAstForHtml(html);
        Tag tag = parseHtmlAst(ast);
        
        applyStyleTo(tag);

        return tag;
    }

    private String buildAstForHtml(String html) throws IOException {
        String normalizedHtml = html.replace("`", "");
        HtmlParser htmlParser = new HtmlParser();
        
        return htmlParser.parse(normalizedHtml);
    }

    private Tag parseHtmlAst(String ast) throws ParseException {
        StructureParser structureParser = new StructureParser(ast);
        
        return structureParser.parse();
    }

    private void applyStyleTo(Tag root) {
        styleApplicator.apply(root);
    }

    private Tag convertHtmlToReactNative(Tag htmlTag) {
        ReactNativeStructureParser parser = new ReactNativeStructureParser(htmlTag);
        Tag rnTag = parser.parse();
        
        // TODO: mandar para o behavior parse 'root' (pd ter behavior para alguma tag)

        return rnTag;
    }

    private String buildTagCode(Tag rnTag) {
        StringBuilder code = new StringBuilder();

        for (String line : rnTag.toCode()) {
            code.append(line);
        }

        return code.toString();
    }    

    private boolean isDeclarationWithGetElementById(String code) {
        return code.matches("(const|var|let)[\\s\\t]+[A-z0-9_$]+.+document\\.getElementById\\(.+\\)");
    }

    private String parseDeclarationWithGetElementById(String code) {
        String parsedCode = code;
        Pattern pattern = Pattern.compile(".*(getElementById\\(\\\"(.+)\\\"\\)).*");
        Matcher matcher = pattern.matcher(code);

        if (matcher.matches()) {
            String tagId = matcher.group(2);
            Variable stateVariable = buildStateVariable(tagId);

            if (!stateDeclarations.contains(stateVariable)) {
                parseStateVariable(stateVariable);
                parseTagIdentifier(tagId, code);
            }
            
            parsedCode = "";
        }

        return parsedCode;
    }

    private void parseStateVariable(Variable stateVar) {
        stateDeclarations.add(stateVar);
    }

    private void parseTagIdentifier(String tagId, String code) {
        String varName = extractIdentifierFromDeclaration(code);

        symbolTable.put(varName, tagId);
    }

    private boolean isCallFromGetElementById(String code) {
        return code.matches("document\\.getElementById\\(.+\\)\\..+");
    }

    private String parseCallFromGetElementById(String code) {
        String attributeAssignment = extractAttributeAssignment(code);
        String attributeName = extractVariableFromAssignment(attributeAssignment);
        String attributeValue = extractValueFromAssignment(attributeAssignment);
        Tag tag = getReferedTag(code);

        tag.addAttribute(attributeName, attributeValue);

        return "";
    }

    private Tag getReferedTag(String code) {
        String tagId = extractIdFromGetElementById(code);
        
        return structure.getTagWithId(tagId);
    }

    private String extractAttributeAssignment(String code) {
        return code.substring(code.indexOf(").")+1);
    }

    private String extractVariableFromAssignment(String tagProperty) {
        return tagProperty.split("=")[0];
    }

    private String extractValueFromAssignment(String tagProperty) {
        return tagProperty.split("=")[1];
    }    

    private boolean isDeclaration(String code) {
        return code.matches("(const|var|let)[\\s\\t]+[A-z0-9_$]+.+");
    }

    private void parseDeclaration(String code) {
        parseTagIdentifier(code, code);
    }

    private String extractIdentifierFromDeclaration(String code) {
        return code.split(" ")[1].split("=")[0];
    }

    private void parseDeclarations() {
        for (Variable declaration : stateDeclarations) {
            stateBody.add(buildDeclarationCode(declaration));
        }
    }

    private String buildDeclarationCode(Variable declaration) {
        StringBuilder code = new StringBuilder();

        code.append("set");
        code.append(declaration.getId());
        code.append("(_");
        code.append(declaration.getId());
        code.append(')');

        return code.toString();
    }   


    //-------------------------------------------------------------------------
    //		Getters
    //-------------------------------------------------------------------------
    public List<Variable> getStateDeclarations() {
        return stateDeclarations;
    }

    public List<String> getStateBody() {
        return stateBody;
    }
}
