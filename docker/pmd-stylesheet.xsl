<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" indent="yes"/>
    <xsl:template match="/ruleset">
        <xsl:text>h1. PMD rules</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:value-of select="description" />
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>h2. Excluded</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>h3. Rulesets</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:call-template name="excludedPatterns"/>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>h3. Rules</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:call-template name="excludedRules"/>
        <xsl:text>h2. Included Rules</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:call-template name="includedRules"/>
    </xsl:template>
    <xsl:template name="excludedPatterns">
        <xsl:for-each select="exclude-pattern">
            <xsl:text>Ruleset: </xsl:text>
            <xsl:value-of select="."/>
            <xsl:text>&#xa;</xsl:text>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="excludedRules">
        <xsl:text>|| Rule || Priority || Description ||</xsl:text>
        <xsl:for-each select="rule[priority='5']">
            <xsl:text>&#xa;</xsl:text>
            <xsl:text>| </xsl:text>
            <xsl:value-of select="@ref"/>
            <xsl:text> | </xsl:text>
            <xsl:value-of select="priority"/>
            <xsl:text> | </xsl:text>
            <xsl:value-of select="description"/>
            <xsl:text> |</xsl:text>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="includedRules">
        <xsl:text>|| Rule ||</xsl:text>
        <xsl:for-each select="rule[not(priority)]">
            <xsl:text>&#xa;</xsl:text>
            <xsl:text>| </xsl:text>
            <xsl:value-of select="@ref"/>
            <xsl:text> |</xsl:text>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>