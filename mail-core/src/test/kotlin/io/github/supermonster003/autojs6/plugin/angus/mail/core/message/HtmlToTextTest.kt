package io.github.supermonster003.autojs6.plugin.angus.mail.core.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Roadmap D20: the derived `text` of html-only mail. */
class HtmlToTextTest {

    @Test
    fun blocksBreaksAndListsBecomeLines() {
        val text = HtmlToText.convert("<div>one<br>two</div><p>three</p><ul><li>a</li><li>b</li></ul>")
        assertEquals("one\ntwo\n\nthree\n\n- a\n- b", text)
    }

    @Test
    fun scriptsStylesHeadAndCommentsAreDropped() {
        val text = HtmlToText.convert("<html><head><title>t</title><style>p{color:red}</style></head><body><!-- c --><script>alert(1)</script><p>kept</p></body></html>")
        assertEquals("kept", text)
        assertFalse(text.contains("alert"))
    }

    @Test
    fun linksKeepTheirTargetWhenItDiffersFromTheLabel() {
        assertEquals("Verify now <https://example.org/verify?x=1>", HtmlToText.convert("""<a href="https://example.org/verify?x=1">Verify now</a>"""))
        assertEquals("https://example.org/", HtmlToText.convert("""<a href="https://example.org/">https://example.org/</a>"""))
        assertEquals("top", HtmlToText.convert("""<a href="#top">top</a>"""))
        assertEquals("bold link <https://example.org/>", HtmlToText.convert("""<a href='https://example.org/'><b>bold</b> link</a>"""))
    }

    @Test
    fun entitiesAreDecodedIncludingNumericOnes() {
        assertEquals("a & b < c > \"d\" 'e' 你 好 ©", HtmlToText.convert("a &amp; b &lt; c &gt; &quot;d&quot; &apos;e&apos; &#20320; &#x597D; &copy;"))
        assertEquals("&unknown; &#xZZ;", HtmlToText.convert("&unknown; &#xZZ;"))
        assertEquals("10 minutes", HtmlToText.convert("10&nbsp;minutes"))
    }

    @Test
    fun tableCellsAreTabSeparatedAndRowsAreLines() {
        assertEquals("a\tb\nc\td", HtmlToText.convert("<table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>"))
        assertEquals("a\tb\nc\td", HtmlToText.convert("<table>\n  <tr>\n    <td>a</td>\n    <td>b</td>\n  </tr>\n  <tr><td>c</td><td>d</td></tr>\n</table>"))
    }

    @Test
    fun preformattedBlocksKeepTheirLineBreaks() {
        assertEquals("line 1\n  line 2\n\nafter", HtmlToText.convert("<pre>line 1\n  line 2</pre>\n<p>after</p>"))
    }

    @Test
    fun sourceLineBreaksInsideParagraphsAreSpaces() {
        assertEquals("a wrapped\nline", HtmlToText.convert("<p>a\n   wrapped<br>\nline</p>"))
    }

    @Test
    fun whitespaceIsCollapsedAndBlankLinesAreLimited() {
        val text = HtmlToText.convert("<p>  a   \n   b </p>\n\n\n\n<p>c</p><p></p><p></p><p>d</p>")
        assertEquals("a b\n\nc\n\nd", text)
    }

    @Test
    fun malformedMarkupDegradesToText() {
        val text = HtmlToText.convert("<p>unclosed <b>bold <i>x</p> tail < not a tag")
        assertTrue(text, text.startsWith("unclosed bold x"))
        assertTrue(text, text.contains("tail < not a tag"))
    }
}
