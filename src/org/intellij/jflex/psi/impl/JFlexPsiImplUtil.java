/*
 * Copyright 2011-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.jflex.psi.impl;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.*;
import com.intellij.util.containers.ContainerUtil;
import org.intellij.jflex.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author gregsh
 */
public class JFlexPsiImplUtil extends JavaRefHelper {
  @NotNull
  public static String getName(PsiNameIdentifierOwner o) {
    return ObjectUtils.assertNotNull(o.getNameIdentifier()).getText();
  }

  @NotNull
  public static PsiNameIdentifierOwner setName(PsiNameIdentifierOwner o, String newName) {
    ObjectUtils.assertNotNull(o.getNameIdentifier()).replace(JFlexPsiElementFactory.createIdFromText(o.getProject(), newName));
    return o;
  }

  @NotNull
  public static PsiElement getNameIdentifier(JFlexMacroDefinition o) {
    return o.getId();
  }

  @NotNull
  public static PsiReference getReference(JFlexMacroReference o) {
    return new PsiReferenceBase<JFlexMacroReference>(o, TextRange.from(0, o.getTextRange().getLength())) {
      @Nullable
      @Override
      public PsiElement resolve() {
        final String name = getElement().getId().getText();
        CommonProcessors.FindFirstProcessor<JFlexMacroDefinition> processor =
          new CommonProcessors.FindFirstProcessor<JFlexMacroDefinition>() {
            @Override
            protected boolean accept(JFlexMacroDefinition o) {
              return Comparing.equal(o.getName(), name);
            }
          };
        processMacroVariants(getElement(), processor);
        return processor.getFoundValue();
      }

      @NotNull
      @Override
      public Object[] getVariants() {
        CommonProcessors.CollectProcessor<JFlexMacroDefinition> processor =
          new CommonProcessors.CollectProcessor<>();
        processMacroVariants(getElement(), processor);
        return ArrayUtil.toObjectArray(processor.getResults());
      }

      @Override
      public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        return getElement().getId().replace(JFlexPsiElementFactory.createIdFromText(getElement().getProject(), newElementName));
      }
    };
  }

  private static boolean processMacroVariants(PsiElement context, Processor<JFlexMacroDefinition> processor) {
    final PsiFile containingFile = context.getContainingFile();
    List<JFlexMacroDefinition> macros = CachedValuesManager.getCachedValue(
      containingFile,
      () -> CachedValueProvider.Result.create(computeDefinitions(containingFile, JFlexMacroDefinition.class), containingFile));
    return ContainerUtil.process(macros, processor);
  }

  public static <T> List<T> computeDefinitions(PsiFile psiFile, final Class<T> clazz) {
    final List<T> result = ContainerUtil.newArrayList();
    psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (clazz.isInstance(element)) {
          result.add((T)element);
        }
        else if (!(element instanceof JFlexLexicalRulesSection) &&
                 !(element instanceof JFlexUserCodeSection)) {
          super.visitElement(element);
        }
      }
    });
    return result;
  }

  @NotNull
  public static PsiElement getNameIdentifier(JFlexStateDefinition o) {
    return o.getId();
  }

  @NotNull
  public static PsiReference getReference(JFlexStateReference o) {
    return new StateRef(o);
  }

  public static boolean isYYINITIAL(String s) {
    return "YYINITIAL".equals(s);
  }

}
