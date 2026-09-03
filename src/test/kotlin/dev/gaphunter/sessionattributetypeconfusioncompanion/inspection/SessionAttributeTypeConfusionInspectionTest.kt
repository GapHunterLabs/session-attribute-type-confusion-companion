package dev.gaphunter.sessionattributetypeconfusioncompanion.inspection

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/** Every test method uses its own uniquely-suffixed key/type names, same discipline as this catalog's other cross-file test suites. */
class SessionAttributeTypeConfusionInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SessionAttributeTypeConfusionInspection::class.java)
    }

    fun `test a write of one type and a cast to a genuinely unrelated type in another file is flagged`() {
        myFixture.addFileToProject(
            "Writer1.java",
            """
            import javax.servlet.http.HttpSession;

            class Cat1 {}

            class Writer1 {
                void write(HttpSession session) {
                    session.setAttribute("profile1", new Cat1());
                }
            }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "Reader1.java",
            """
            import javax.servlet.http.HttpSession;

            class Dog1 {}

            class Reader1 {
                void read(HttpSession session) {
                    Dog1 d = (Dog1) session.getAttribute("profile1");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("CWE-843") == true })
    }

    fun `test a cast to the exact same type it was stored as is not flagged`() {
        myFixture.configureByText(
            "Same2.java",
            """
            import javax.servlet.http.HttpSession;

            class SameType2 {}

            class Same2 {
                void run(HttpSession session) {
                    session.setAttribute("profile2", new SameType2());
                    SameType2 v = (SameType2) session.getAttribute("profile2");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-843") == true })
    }

    fun `test a cast to a real supertype of the stored value is not flagged`() {
        myFixture.configureByText(
            "Compat3.java",
            """
            import javax.servlet.http.HttpSession;

            class Base3 {}
            class Impl3 extends Base3 {}

            class Compat3 {
                void run(HttpSession session) {
                    session.setAttribute("profile3", new Impl3());
                    Base3 v = (Base3) session.getAttribute("profile3");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-843") == true })
    }

    fun `test a write and a read under different keys is not flagged`() {
        myFixture.configureByText(
            "DiffKey4.java",
            """
            import javax.servlet.http.HttpSession;

            class Foo4 {}
            class Bar4 {}

            class DiffKey4 {
                void run(HttpSession session) {
                    session.setAttribute("keyA4", new Foo4());
                    Bar4 v = (Bar4) session.getAttribute("keyB4");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-843") == true })
    }

    fun `test a key with a mix of compatible and incompatible write types is left unflagged`() {
        myFixture.configureByText(
            "Mixed5.java",
            """
            import javax.servlet.http.HttpSession;

            class Foo5 {}
            class Bar5 {}

            class Mixed5 {
                void writeCompatible(HttpSession session) {
                    session.setAttribute("profile5", new Bar5());
                }
                void writeIncompatible(HttpSession session) {
                    session.setAttribute("profile5", new Foo5());
                }
                void read(HttpSession session) {
                    Bar5 v = (Bar5) session.getAttribute("profile5");
                }
            }
            """.trimIndent(),
        )
        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("CWE-843") == true })
    }
}
