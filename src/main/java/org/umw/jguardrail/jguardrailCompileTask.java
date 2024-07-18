package org.umw.jguardrail;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileTask;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import net.ianfinlayson.jguardrail.Warnings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

//CompileTask class used to prevent the project from compiling if there are errors.
public class jguardrailCompileTask implements CompileTask {

    @Override
    public boolean execute(@NotNull CompileContext context) {
        Project project = context.getProject();
        //Have to read files from within a read action to be safe.
        ApplicationManager.getApplication().runReadAction(()->{
            //Get every file in the project that has the .java extension.
            Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));
            for(VirtualFile file : files){
                //Run the jguardrail parser and get the list of warnings.
                ArrayList<Warnings.Warning> warnings = jguardrailMain.runParser(project, file.getName());
                //Break if there are no warnings.
                if(warnings == null){
                    break;
                }
                //For each warning found, add a compile error message that automatically fails compilation.
                for (Warnings.Warning warning : warnings) {
                    String message = file.getName() + ": " + warning.line + "\n" + warning.message;
                    context.addMessage(CompilerMessageCategory.ERROR, message, project.getPresentableUrl(), -1, -1);
                }
            }
        });
        //Allow the compile to run if there are no errors found.
        return true;
    }
}
