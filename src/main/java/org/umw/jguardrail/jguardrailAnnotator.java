package org.umw.jguardrail;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import net.ianfinlayson.jguardrail.Warnings;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

//Annotator class used for highlighting in the editor
public class jguardrailAnnotator implements Annotator {

    /*An arrayList to hold the line numbers of each line that has already been highlighted. Prevents the editor from flagging
      a line multiple times for only one error*/
    private final ArrayList<Integer> done = new ArrayList<>();

    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        //If the element is white space, a comment, or something really short, don't highlight it.
        if (element instanceof PsiWhiteSpace || element instanceof PsiComment) {
            return;
        }
        if(element.getText().length() < 3){
            return;
        }

        //Run the jguardrail parser and get an arraylist of the warnings in the file.
        ArrayList<Warnings.Warning> warnings = jguardrailMain.runParser(element.getProject(), element.getContainingFile().getName());
        if (warnings == null || warnings.isEmpty()) {
            return;
        }

        /*If the file the element is in is not the same as the file where the warnings were found, do not highlight.
          Prevents the annotator from highlighting lines in one file for errors in another.*/
        String warningsFile = Warnings.getFileName();
        String elementFile = element.getContainingFile().getName();
        if(!warningsFile.equals(elementFile)){
            return;
        }

        for (Warnings.Warning warning : warnings) {
            //Get the line number of the element.
            int line = element.getContainingFile().getFileDocument().getLineNumber(element.getTextRange().getStartOffset() + 1) + 1;
            int newLine = element.getContainingFile().getFileDocument().getLineNumber(element.getTextRange().getStartOffset());
            //Create a text range for the entire line the element is found in.
            TextRange textRange = new TextRange(element.getContainingFile().getFileDocument().getLineStartOffset(newLine), element.getContainingFile().getFileDocument().getLineEndOffset(newLine));
            String s = element.getContainingFile().getFileDocument().getText(textRange);
            s = s.trim();
            //Highlight from the beginning of the element to the end of the line.
            TextRange highlightRange;
            if(textRange.getEndOffset() > element.getTextRange().getEndOffset()){
                highlightRange = new TextRange(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset());
            }else{
                highlightRange = new TextRange(element.getTextRange().getStartOffset(), textRange.getEndOffset());
            }
            /*If the warning is found on the same line as the element, the line hasn't already been highlighted, and the
              element contains the line we want to highlight, highlight the line as an error with the given warning message.*/
            if(warning.line == line && !done.contains(Integer.valueOf(line)) && element.getText().contains(s)) {
                done.add(Integer.valueOf(line));
                holder.newAnnotation(HighlightSeverity.ERROR, warning.message).range(highlightRange).highlightType(ProblemHighlightType.ERROR).create();
            }
        }
    }
}