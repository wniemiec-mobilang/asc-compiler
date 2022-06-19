package wniemiec.mobilang.ama.framework.reactnative.coder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wniemiec.mobilang.ama.coder.exception.CoderException;
import wniemiec.mobilang.ama.models.CodeFile;
import wniemiec.mobilang.ama.models.Screen;
import wniemiec.mobilang.ama.models.Style;
import wniemiec.mobilang.ama.models.StyleSheetRule;
import wniemiec.mobilang.ama.models.behavior.Behavior;
import wniemiec.mobilang.ama.models.behavior.Declaration;
import wniemiec.mobilang.ama.models.behavior.Declarator;
import wniemiec.mobilang.ama.models.behavior.Instruction;
import wniemiec.mobilang.ama.models.behavior.Literal;
import wniemiec.mobilang.ama.models.tag.Tag;


class ReactNativeScreensCoderTest {
    //-------------------------------------------------------------------------
    //		Attributes
    //-------------------------------------------------------------------------
    private static final int INDEX_ANDROID;
    private static final int INDEX_IOS;
    private static final String TAG_ID;
    private ReactNativeScreensCoder coder;
    private List<Screen> screens;
    private List<CodeFile> obtainedCode;


    //-------------------------------------------------------------------------
    //		Initialization block
    //-------------------------------------------------------------------------
    static {
        INDEX_ANDROID = 0;
        INDEX_IOS = 1;
        TAG_ID = "tagid";
    }


    //-------------------------------------------------------------------------
    //		Test hooks
    //-------------------------------------------------------------------------
    @BeforeEach
    void setUp() {
        screens = new ArrayList<>();
    }
    

    //-------------------------------------------------------------------------
    //		Tests
    //-------------------------------------------------------------------------
    @Test
    void testSingleAndroidScreen() throws CoderException {
        withScreen(new Screen.Builder()
            .name("about")
            .structure(buildButtonWithOnClickAndValue("click me"))
            .style(buildButtonStyleUsingBlueAndWhite())
            .behavior(buildDeclarationWithIdAndAssignment("hello", "world"))
            .build()
        );
        runCoder();
        assertCoderGeneratedSomething();
        assertCodeFileHasName(
            "android/app/src/main/assets/about.html",
            "ios/assets/about.html"
        );
        assertAndroidCodeEquals(
            "<!DOCTYPE html>",
            "<html>",
            "    <head>",
            "        <title>about</title>",
            "            <style>",
            "                button { padding: 0; }",
            "                button {",
            "                    background-color: blue;",
            "                    color: white;",
            "                }",
            "            </style>",
            "    </head>",
            "    <body>",
            "        <button onclick=\"alert('hey!! you pressed the button!')\" id=\"" + TAG_ID + "\">",
            "            click me", 
            "        </button>",
            "    </body>",
            "    <script>",
            "\"use strict\";",
            "",
            "        var hello = \"world\";",
            "    </script>",
            "</html>"
        );
    }

    @Test
    void testSingleIosScreen() throws CoderException {
        withScreen(new Screen.Builder()
            .name("about")
            .structure(buildButtonWithOnClickAndValue("click me"))
            .style(buildButtonStyleUsingBlueAndWhite())
            .behavior(buildDeclarationWithIdAndAssignment("hello", "world"))
            .build()
        );
        runCoder();
        assertCoderGeneratedSomething();
        assertCodeFileHasName(
            "android/app/src/main/assets/about.html",
            "ios/assets/about.html"
        );
        assertIosCodeEquals(
            "<!DOCTYPE html>",
            "<html>",
            "    <head>",
            "        <title>about</title>",
            "            <style>",
            "                button { padding: 0; }",
            "                button {",
            "                    background-color: blue;",
            "                    color: white;",
            "                }",
            "            </style>",
            "    </head>",
            "    <body>",
            "        <button onclick=\"alert('hey!! you pressed the button!')\" id=\"" + TAG_ID + "\">",
            "            click me", 
            "        </button>",
            "    </body>",
            "    <script>",
            "\"use strict\";",
            "",
            "        var hello = \"world\";",
            "    </script>",
            "</html>"
        );
    }


    //-------------------------------------------------------------------------
    //		Methods
    //-------------------------------------------------------------------------
    private void withScreen(Screen screen) {
        screens.add(screen);
    }

    private Tag buildButtonWithOnClickAndValue(String value) {
        Tag buttonTag = Tag.getNormalInstance("button");
        
        buttonTag.addAttribute("onclick", "alert('hey!! you pressed the button!')");
        buttonTag.setValue(value);
        buttonTag.addAttribute("id", TAG_ID);
        
        return buttonTag;
    }

    private Style buildButtonStyleUsingBlueAndWhite() {
        Style style = new Style();
        StyleSheetRule rule = new StyleSheetRule();

        rule.addSelector("button");
        rule.addDeclaration("background-color", "blue");
        rule.addDeclaration("color", "white");
        style.addRule(rule);

        return style;
    }

    private Behavior buildDeclarationWithIdAndAssignment(String id, String assignment) {
        Declaration declaration = buildLiteralDeclaration(id, assignment);

        return buildBehaviorWith(declaration);
    }

    private Declaration buildLiteralDeclaration(String id, String assignment) {
        Declarator declarator = new Declarator(
            "string", 
            "let", 
            id, 
            Literal.ofString(assignment)
        );
        
        return new Declaration("let", declarator);
    }

    private Behavior buildBehaviorWith(Instruction... declarations) {
        return new Behavior(Arrays.asList(declarations));
    }

    private void runCoder() throws CoderException {
        coder = new ReactNativeScreensCoder(screens);
        obtainedCode = coder.generateCode();
    }

    private void assertCoderGeneratedSomething() {
        Assertions.assertFalse(obtainedCode.isEmpty());
    }

    private void assertCodeFileHasName(String... names) {
        for (int i = 0; i < names.length; i++) {
            Assertions.assertEquals(names[i], obtainedCode.get(i).getName());
        }
    }

    private void assertAndroidCodeEquals(String... lines) {
        assertCodeEquals(INDEX_ANDROID, lines);
    }

    private void assertCodeEquals(int index, String... lines) {
        List<String> expectedCode = Arrays.asList(lines);

        assertHasSameSize(expectedCode, obtainedCode.get(index).getCode());
        assertHasSameLines(expectedCode, obtainedCode.get(index).getCode());
    }

    private void assertHasSameSize(List<String> expected, List<String> obtained) {
        Assertions.assertEquals(expected.size(), obtained.size());
    }

    private void assertHasSameLines(List<String> expected, List<String> obtained) {
        for (int i = 0; i < expected.size(); i++) {            
            assertHasSameLine(expected.get(i), obtained.get(i));
        }
    }

    private void assertHasSameLine(String expected, String obtained) {
        Assertions.assertEquals(
            removeWhiteSpaces(expected),
            removeWhiteSpaces(obtained)
        );
    }

    private String removeWhiteSpaces(String text) {
        return text.replaceAll("[\\s\\t]+", "");
    }

    private void assertIosCodeEquals(String... lines) {
        assertCodeEquals(INDEX_IOS, lines);
    }
}
