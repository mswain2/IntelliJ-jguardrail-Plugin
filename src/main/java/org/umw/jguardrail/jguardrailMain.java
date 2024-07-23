package org.umw.jguardrail;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import net.ianfinlayson.jguardrail.*;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Collection;

//A jguardrail main class to run the parser from the jguardrail API.
public class jguardrailMain {

    public static ArrayList<Warnings.Warning> runParser(Project project, String fileName){
        //Return no warnings if the file does not end in .java.
        if(!fileName.endsWith(".java")){
            return null;
        }
        boolean [] warned = {false};
        ArrayList<Warnings.Warning> allWarnings = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            //Run the parser for the specified file.
            Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(fileName, GlobalSearchScope.projectScope(project));
            for (VirtualFile file : files) {
                // set up streams
                JavaLexer lexer = null;
                try {
                    lexer = new JavaLexer(CharStreams.fromPath(file.toNioPath()));
                    Warnings.setupWarnings(file.getName());
                } catch (Exception e) {
                    System.out.println("Could not open " + file.getName() + " for reading.");
                    return (Boolean) false;
                }

                CommonTokenStream tokens = new CommonTokenStream(lexer);
                JavaParser parser = new JavaParser(tokens);

                // do not not do the default thing on errors (printing them out)
                // instead, add our own whose job it is to simply record if there are errors
                // if there are, jguardrail will not do any analysis (leaving them for javac)
                lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
                parser.removeErrorListener(ConsoleErrorListener.INSTANCE);

                ErrorListener errorListener = new ErrorListener();
                lexer.addErrorListener(errorListener);
                parser.addErrorListener(errorListener);

                // do the parsing
                ParseTree tree = parser.compilationUnit();
                if (tree == null) {
                    System.out.println("Couldn't parse at all!");
                }
                //if there were any errors, we bail now
                if (errorListener.errorsExist()) {
                    System.out.println("Errors exist!");
                    return (Boolean) false;
                }

                // make a list of all the checks we have
                JavaParserBaseVisitor[] checkers = {
                        new SwitchCheckVisitor(),
                        new StringEqualsVisitor(),
                        new IntDivideVisitor(),
                        new MethodNameVisitor(),
                        new ShadowCheckVisitor(),
                        new SelfSetVisitor(),
                        new InstVarInitVisitor(),
                        new ControlStructureVisitor()
                };

                // run all the checkers
                for (JavaParserBaseVisitor checker : checkers) {
                    // there have been some cases of a checker barfing up
                    // exceptions, confusing students.  So we ignore any
                    // errors not caught within the checker itself, but
                    // do log them so I can investigate
                    try {
                        checker.visit(tree);
                    } catch (Exception e) {
                        System.out.println("Warnings fail");
                        Warnings.fail();
                    }
                }

                // If there were warnings found, add them to the list.
                if (Warnings.getWarnings() != null && !Warnings.getWarnings().isEmpty()) {
                    ArrayList<Warnings.Warning> warningList = Warnings.getWarnings();
                    allWarnings.addAll(warningList);
                    warned[0] = true;
                }
                ;
            }
            if (warned[0]) {
                return (Boolean) false;
            } else {
                return (Boolean) true;
            }
        });

        //If there were warnings, return the list.
        //If there were no warnings, return null.
        if(warned[0]){
            return allWarnings;
        }else{
            return null;
        }
    }

}
