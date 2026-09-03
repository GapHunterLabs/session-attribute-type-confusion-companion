package dev.gaphunter.sessionattributetypeconfusioncompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import dev.gaphunter.sessionattributetypeconfusioncompanion.model.SessionAttributeHit

/**
 * Per-file pass: finds `(TypeB) session.getAttribute("key")` /
 * `(TypeB) context.getAttribute("key")` -- an explicit cast whose
 * operand is directly a `getAttribute` call on a recognized receiver
 * -- then correlates against every WRITE site the project holds for
 * that exact key (see [SessionAttributeWriteScanner]). Flags only
 * when EVERY known write site for that key is genuinely incompatible
 * with the cast type according to the platform's real type system
 * (`PsiType.isAssignableFrom` -- the first use of real type-
 * compatibility checking in this catalog; every earlier mechanism
 * compared type NAMES as text) -- deliberately conservative: a key
 * written with a MIX of compatible and incompatible types (a
 * legitimate polymorphic use this v0.1 can't fully disambiguate) is
 * left unflagged rather than risk a false positive.
 */
object SessionAttributeReadFinder {

    private val RECEIVER_CLASS_NAMES = setOf("HttpSession", "ServletContext")

    fun findAll(file: PsiFile): List<SessionAttributeHit> {
        if (file !is PsiJavaFile) return emptyList()
        val writeSitesByKey = SessionAttributeWriteScanner.writeSitesByKey(file.project)
        if (writeSitesByKey.isEmpty()) return emptyList()

        val hits = mutableListOf<SessionAttributeHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitTypeCastExpression(expression: PsiTypeCastExpression) {
                super.visitTypeCastExpression(expression)
                val call = expression.operand as? PsiMethodCallExpression ?: return
                if (call.methodExpression.referenceName != "getAttribute") return
                val qualifier = call.methodExpression.qualifierExpression ?: return
                val className = (qualifier.type as? PsiClassType)?.className ?: return
                if (className !in RECEIVER_CLASS_NAMES) return

                val keyLiteral = call.argumentList.expressions.getOrNull(0) as? PsiLiteralExpression ?: return
                val key = keyLiteral.value as? String ?: return
                val castType = expression.castType?.type as? PsiClassType ?: return
                if (castType.className == "Object") return

                val writeTypes = writeSitesByKey[key] ?: return
                val realWriteTypes = writeTypes.filterIsInstance<PsiClassType>()
                if (realWriteTypes.isEmpty()) return
                val allIncompatible = realWriteTypes.all { isIncompatible(it, castType) }
                if (!allIncompatible) return

                hits += SessionAttributeHit(expression, key, realWriteTypes.first().presentableText, castType.presentableText)
            }
        })
        return hits
    }

    private fun isIncompatible(writeType: PsiType, readType: PsiType): Boolean =
        !readType.isAssignableFrom(writeType) && !writeType.isAssignableFrom(readType)
}
