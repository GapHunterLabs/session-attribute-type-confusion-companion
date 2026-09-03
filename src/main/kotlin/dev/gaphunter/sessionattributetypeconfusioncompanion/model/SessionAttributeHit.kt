package dev.gaphunter.sessionattributetypeconfusioncompanion.model

import com.intellij.psi.PsiElement

/** A confirmed session-attribute type confusion: [key] is written elsewhere in the project with a value of [writeTypeText], but read back at [anchor] with an explicit cast to [readTypeText] -- and the two types are genuinely unrelated according to the platform's own type system, guaranteeing a `ClassCastException` if this code path runs. */
data class SessionAttributeHit(val anchor: PsiElement, val key: String, val writeTypeText: String, val readTypeText: String)
