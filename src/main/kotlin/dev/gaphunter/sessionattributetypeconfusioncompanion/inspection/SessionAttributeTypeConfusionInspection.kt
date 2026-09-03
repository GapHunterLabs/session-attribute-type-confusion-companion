package dev.gaphunter.sessionattributetypeconfusioncompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiFile
import dev.gaphunter.sessionattributetypeconfusioncompanion.detect.SessionAttributeReadFinder
import dev.gaphunter.sessionattributetypeconfusioncompanion.model.SessionAttributeHit
import dev.gaphunter.sessionattributetypeconfusioncompanion.review.ReviewPrompt

/** Flags a session/context attribute cast to a type genuinely incompatible with every known write site sharing the same string key -- see [SessionAttributeReadFinder]. */
class SessionAttributeTypeConfusionInspection : LocalInspectionTool() {

    companion object {
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (file.text.length > MAX_FILE_LENGTH) return null

        val hits = SessionAttributeReadFinder.findAll(file)
        if (hits.isEmpty()) return null

        val problems = hits.map { hit ->
            manager.createProblemDescriptor(
                hit.anchor,
                messageFor(hit),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            )
        }

        val path = file.virtualFile?.path
        if (path != null) {
            for (hit in hits) {
                val lineNumber = file.viewProvider.document?.getLineNumber(hit.anchor.textRange.startOffset) ?: -1
                ReviewPrompt.recordHit(file.project, "$path:$lineNumber:${hit.key}")
            }
        }

        return problems.toTypedArray()
    }

    private fun messageFor(hit: SessionAttributeHit): String =
        "Attribute '${hit.key}' is stored elsewhere as '${hit.writeTypeText}' but cast here to '${hit.readTypeText}' -- " +
            "the two types are genuinely unrelated (CWE-843), this cast will throw ClassCastException if this code path runs"
}
