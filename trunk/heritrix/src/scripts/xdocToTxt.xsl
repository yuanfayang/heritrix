<?xml version="1.0" encoding="UTF-8"?>
<!--Transform xdoc files to text.

    $Id$
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" version="1.0" encoding="UTF-8"/>
    <xsl:template match="/">
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="section|subsection">
        <xsl:value-of select="@name"/>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="a">
        <xsl:value-of select="."/> &lt;<xsl:value-of select="@href"/>&gt;
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="img"> &lt;<xsl:value-of select="@src"/>&gt;
    <xsl:apply-templates/>
    </xsl:template>
</xsl:stylesheet>
