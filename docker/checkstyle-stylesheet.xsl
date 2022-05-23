<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" indent="yes"/>
    <xsl:template match="/module">
        <xsl:text>h1. Active rules</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>|| Rule || Properties ||</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:call-template name="rules"/>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>h1. Deactivated rules</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:text>|| Rule || Description ||</xsl:text>
        <xsl:text>&#xa;</xsl:text>
        <xsl:call-template name="deactivatedRules"/>
    </xsl:template>
    <xsl:template name="rules">
        <xsl:for-each select="module">
            <xsl:text>| </xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text> | </xsl:text>
            <xsl:for-each select="property">
                <xsl:text>* </xsl:text>
                <xsl:value-of select="@name"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="@value"/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:for-each>
            <xsl:text> |</xsl:text>
            <xsl:text>&#xa;</xsl:text>
            <xsl:call-template name="rules"/>
        </xsl:for-each>
    </xsl:template>
    <xsl:template name="deactivatedRules">
        <xsl:for-each select="metadata">
            <xsl:text>| </xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text> | </xsl:text>
            <xsl:value-of select="@value"/>
            <xsl:text> |</xsl:text>
            <xsl:text>&#xa;</xsl:text>
            <xsl:call-template name="rules"/>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>