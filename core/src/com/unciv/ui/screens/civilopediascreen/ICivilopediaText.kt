package com.unciv.ui.screens.civilopediascreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.models.ruleset.IRulesetObject
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetObject
import com.unciv.models.stats.INamed
import com.unciv.ui.objectdescriptions.uniquesToCivilopediaTextLines
import yairm210.purity.annotations.Readonly

/** Addon common to most ruleset game objects managing civilopedia display
 *
 * ### Usage:
 * 1. Let [Ruleset] object implement this (by inheriting and implementing class [ICivilopediaText])
 * 2. Add `"civilopediaText": ["",…],` in the json for these objects
 * 3. Optionally override [getCivilopediaTextHeader] to supply a different header line
 * 4. Optionally override [getCivilopediaTextLines] to supply automatic stuff like tech prerequisites, uniques, etc.
 * 4. Optionally override [assembleCivilopediaText] to handle assembly of the final set of lines yourself.
 */
interface ICivilopediaText {
    /** List of strings supporting simple [formatting rules][FormattedLine] that [CivilopediaScreen] can render.
     *  May later be merged with automatic lines generated by the deriving class
     *  through overridden [getCivilopediaTextHeader] and/or [getCivilopediaTextLines] methods.
     */
    var civilopediaText: List<FormattedLine>

    /** Generate header line from object metadata.
     * Default implementation will take [INamed.name] and render it in 150% normal font size with an icon from [makeLink].
     * @return A [FormattedLine] that will be inserted on top
     */
    fun getCivilopediaTextHeader(): FormattedLine? =
        if (this is INamed) FormattedLine(name, icon = makeLink(), header = 2)
        else null

    /** Generate automatic lines from object metadata.
     *
     * This function ***MUST not rely*** on [UncivGame.Current.gameInfo][UncivGame.gameInfo]
     * **or** [UncivGame.Current.worldScreen][UncivGame.worldScreen] being initialized,
     * this should be able to run from the main menu.
     * (And the info displayed should be about the **ruleset**, not the player situation)
     *
     * Default implementation is empty - no need to call super in overrides.
     * Note that for inclusion of Uniques, two helpers named [uniquesToCivilopediaTextLines] exist (for Sequence or MutableCollection context).
     *
     * @param ruleset The current ruleset for the Civilopedia viewer
     * @return A list of [FormattedLine]s that will be inserted before
     *         the first line of [civilopediaText] having a [link][FormattedLine.link]
     */
    fun getCivilopediaTextLines(ruleset: Ruleset): List<FormattedLine> = listOf()

    /** Build a Gdx [Table] showing our [formatted][FormattedLine] [content][civilopediaText]. */
    fun renderCivilopediaText (labelWidth: Float, linkAction: ((id: String)->Unit)? = null): Table {
        return MarkupRenderer.render(civilopediaText, labelWidth, linkAction = linkAction)
    }

    /** Assemble json-supplied lines with automatically generated ones.
     *
     * The default implementation will insert [getCivilopediaTextLines] before the first [linked][FormattedLine.link] [civilopediaText] line and [getCivilopediaTextHeader] on top.
     *
     * @param ruleset The current ruleset for the Civilopedia viewer
     * @return A new CivilopediaText instance containing original [civilopediaText] lines merged with those from [getCivilopediaTextHeader] and [getCivilopediaTextLines] calls.
     */
    fun assembleCivilopediaText(ruleset: Ruleset): ICivilopediaText {
        val outerLines = civilopediaText.iterator()
        val newLines = sequence {
            var middleDone = false
            var outerNotEmpty = false
            val header = getCivilopediaTextHeader()
            if (header != null) {
                yield(header)
                yield(FormattedLine(separator = true))
            }
            while (outerLines.hasNext()) {
                val next = outerLines.next()
                if (!middleDone && !next.isEmpty() && next.linkType != FormattedLine.LinkType.None) {
                    middleDone = true
                    if (outerNotEmpty) yield(FormattedLine())
                    yieldAll(getCivilopediaTextLines(ruleset))
                    yield(FormattedLine())
                }
                outerNotEmpty = true
                yield(next)
            }
            if (!middleDone) {
                if (outerNotEmpty) yield(FormattedLine())
                yieldAll(getCivilopediaTextLines(ruleset))
            }
            if (this@ICivilopediaText is IRulesetObject && ruleset.mods.size > 1 && originRuleset.isNotEmpty()) {
                yield(FormattedLine())
                yield(FormattedLine("Mod: [$originRuleset]", starred = true, color = "#daa520"))
            }
        }
        return SimpleCivilopediaText(newLines.toList())
    }

    /** Create the correct string for a Civilopedia link.
     *
     *  To actually make it work both as link and as icon identifier, return a string in the form
     *  category/entryname where `category` **must** correspond exactly to either name or label of
     *  the correct [CivilopediaCategories] member. `entryname` must equal the
     *  [ruleset object name][RulesetObject] as defined by the [INamed] interface.
     */
    @Readonly fun makeLink(): String

    /** Overrides alphabetical sorting in Civilopedia
     *  @param ruleset The current ruleset in case the function needs to do lookups
     */
    fun getSortGroup(ruleset: Ruleset): Int = 0

    /** Overrides Icon used for Civilopedia entry list (where you select the instance)
     *  This will still be passed to the category-specific image getter.
     */
    fun getIconName() = if (this is INamed) name else ""
}
