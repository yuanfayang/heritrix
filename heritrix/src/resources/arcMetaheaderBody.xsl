<?xml version="1.0" encoding="UTF-8"?>
<!--
    Transform order file to arc file meta data body info.  Insert other info
    while we're at it such as hostname and ip.

    Produces iso-8859-1 content encoding chars that are not of this charset.
    All of an element white space gets flattened and multiple lines are made
    into one line only.

    $Id$
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" encoding="ISO-8859-1" />
    <xsl:strip-space elements="*"/>

    <!--Params that can be overridden.
     -->
    <xsl:param name="ip" select="'IP UNDEFINED'"/>
    <xsl:param name="hostname" select="'HOSTNAME UNDEFINED'"/>
    <xsl:param name="software" select="'SOFTWARE UNDEFINED'"/>
    <xsl:param name="delimiter" select="': '"/>

    <xsl:template match="/" >
        <xsl:apply-templates select="/crawl-order/meta" />
        <xsl:apply-templates select="/crawl-order/controller/map[@name='http-headers']/string[@name='user-agent']" />
        <xsl:apply-templates select="/crawl-order/controller/newObject[@name='robots-honoring-policy']/string[@name='type']" />
        <xsl:call-template name="print">
            <xsl:with-param name="key" select="'software'" />
            <xsl:with-param name="value" select="$software" />
        </xsl:call-template>
        <xsl:call-template name="print">
            <xsl:with-param name="key" select="'hostname'" />
            <xsl:with-param name="value" select="$hostname" />
        </xsl:call-template>
        <xsl:call-template name="print">
            <xsl:with-param name="key" select="'ip'" />
            <xsl:with-param name="value" select="$ip" />
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="name|description|operator|organization|audience">
        <xsl:call-template name="print">
            <xsl:with-param name="key">
                <xsl:text>crawl-order-</xsl:text>
                <xsl:value-of select="local-name()"/><xsl:text />
            </xsl:with-param>
            <xsl:with-param name="value" select="." />
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="string[@name='user-agent']">
        <xsl:call-template name="print">
            <xsl:with-param name="key">
                <xsl:text>http-header-</xsl:text>
                <xsl:value-of select="@name"/><xsl:text />
            </xsl:with-param>
            <xsl:with-param name="value" select="." />
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="string[@name='type']">
        <xsl:call-template name="print">
            <xsl:with-param name="key" select="'robots-honoring-policy'" />
            <xsl:with-param name="value" select="." />
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="print" >
        <xsl:param name="key" />
        <xsl:param name="value" />
        <xsl:value-of select="normalize-space($key)"/><xsl:text />
        <xsl:value-of select="$delimiter" /><xsl:text />
        <xsl:value-of select="normalize-space($value)"/><xsl:text />
        <xsl:text>&#xd;&#xa;</xsl:text>
    </xsl:template>

    <!--Default handler.  Does nothing.
     -->
    <xsl:template match="text()"  />
</xsl:stylesheet>
