package dev.gaphunter.sessionattributetypeconfusioncompanion.detect

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiType
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Real, whole-project, string-KEY correlation -- distinct from every
 * earlier whole-project analyzer in this catalog (`second-order-sqli-
 * field-companion` correlates by FIELD NAME on `@Entity` classes,
 * `github-actions-pwn-request-companion` correlates by workflow
 * filename). Catalogs every `session.setAttribute("key", expr)` /
 * `context.setAttribute("key", expr)` call anywhere in the project,
 * keyed by the exact string-literal key, recording the STATIC TYPE of
 * the stored expression.
 *
 * Cached per-project via [CachedValuesManager], same discipline as
 * every other whole-project analyzer in this catalog.
 *
 * **v0.1 scope, stated honestly:** only `HttpSession`/`ServletContext`
 * receivers (recognized by [PsiClassType.className], text-only --
 * never resolves the real servlet-api class, matching this catalog's
 * established JDK-type-resolution-fragility fix); the key must be a
 * string LITERAL (never resolves an indirect constant); a project
 * with more than [MAX_FILES] `.java` files skips analysis entirely.
 */
object SessionAttributeWriteScanner {

    const val MAX_FILES = 2000
    private const val MAX_FILE_LENGTH = 500_000
    private val RECEIVER_CLASS_NAMES = setOf("HttpSession", "ServletContext")

    private val CACHE_KEY: Key<CachedValue<Map<String, List<PsiType>>>> = Key.create("sessionAttributeTypeConfusionCompanion.writeSites")

    /** Every session-attribute WRITE site in the project, keyed by exact string-literal key -> the static types stored under it (one entry per write site; duplicates kept intentionally, see [SessionAttributeReadFinder]). */
    fun writeSitesByKey(project: Project): Map<String, List<PsiType>> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE_KEY,
            { CachedValueProvider.Result.create(computeWriteSites(project), PsiModificationTracker.MODIFICATION_COUNT) },
            false,
        )
    }

    private fun computeWriteSites(project: Project): Map<String, List<PsiType>> {
        val scope = GlobalSearchScope.projectScope(project)
        val files = FilenameIndex.getAllFilesByExt(project, "java", scope)
        if (files.size > MAX_FILES) return emptyMap()

        val psiManager = PsiManager.getInstance(project)
        val javaFiles = files.mapNotNull { psiManager.findFile(it) as? PsiJavaFile }
            .filter { it.text.length <= MAX_FILE_LENGTH }

        val result = mutableMapOf<String, MutableList<PsiType>>()
        for (psiFile in javaFiles) {
            psiFile.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitMethodCallExpression(call: PsiMethodCallExpression) {
                    super.visitMethodCallExpression(call)
                    if (call.methodExpression.referenceName != "setAttribute") return
                    val qualifier = call.methodExpression.qualifierExpression ?: return
                    val className = (qualifier.type as? PsiClassType)?.className ?: return
                    if (className !in RECEIVER_CLASS_NAMES) return

                    val args = call.argumentList.expressions
                    val keyLiteral = args.getOrNull(0) as? PsiLiteralExpression ?: return
                    val key = keyLiteral.value as? String ?: return
                    val valueExpr = args.getOrNull(1) ?: return
                    val valueType = valueExpr.type ?: return

                    result.getOrPut(key) { mutableListOf() } += valueType
                }
            })
        }
        return result
    }
}
