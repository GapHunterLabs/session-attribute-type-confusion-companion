# Session Attribute Type Confusion Companion

Flags `(TypeB) session.getAttribute("key")` when every known write
site for that exact key stores a genuinely incompatible type.

## Why it exists

CWE-843. The Servlet API predates Java 5 generics: `getAttribute`
always returns `Object`, so nothing in the language stops a mismatch
between what one part of the code stores under a key and what another
part expects when it reads it back -- a real, documented pattern in
technical forums (Coderanch, Experts Exchange), only discovered at
runtime as a `ClassCastException` otherwise. Searched explicitly for a
named competitor (FindBugs rule, CodeQL query, Datadog rule) for this
exact angle and found none -- stated honestly, same as this catalog's
other weaker-evidence Tier -3 entries.

## Why built this way

- **First real type-compatibility check in this catalog** --
  `PsiType.isAssignableFrom`, not a text comparison of type names. Every
  earlier mechanism (SpEL/XPath/LDAP/Lucene sinks, interface
  divergence, resource leaks) compared class/type NAMES as text; this
  is the first genuinely new kind of reasoning: real platform type
  system compatibility.
- **Correlation by exact string-literal KEY**, project-wide, not by
  call graph or field name -- the write and read sites can be in
  completely unrelated classes, same shape of "invisible connection"
  as `second-order-sqli-field-companion`'s field-name correlation, but
  applied to a new domain (session/context attributes instead of
  persisted entity fields).
- **Deliberately conservative**: a key with a MIX of compatible and
  incompatible write types is left unflagged, since this v0.1 can't
  fully disambiguate a legitimate polymorphic use from a real bug.

## v0.1 scope — stated honestly, not exhaustively

- Only `HttpSession`/`ServletContext` `setAttribute`/`getAttribute`
  (never a generic `Map<String,Object>` used for a similar purpose).
- The key must be an identical string literal on both sides (never
  resolves an indirect constant).
- Write-side type is the static type of the assigned expression;
  read-side type is the explicit cast immediately wrapping the
  `getAttribute(...)` call.
- Flags only when EVERY known write site for that key is incompatible
  with the cast type -- a mix of compatible/incompatible is left
  unflagged.

## Usage

Open a Java file with a `session.setAttribute("key", someTypeA)` call
anywhere in the project and a `(TypeB) session.getAttribute("key")`
cast elsewhere, where `TypeA` and `TypeB` are genuinely unrelated
types -- the cast is flagged.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
