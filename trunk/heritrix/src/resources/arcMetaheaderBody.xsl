<?xml version="1.0" encoding="UTF-8"?>
<!--
    Transform order file to arc file meta data body info.  Insert other info
    while we're at it such as hostname and ip.  The arc metadata schema
    is available at http://archive.org/arc/1.0/arc.xsd.

    $Id$
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"
        omit-xml-declaration="no" standalone="yes"/>
    <xsl:strip-space elements="*"/>
    <!--Params that can be overridden.
     -->
    <xsl:param name="ip" select="'IP UNDEFINED'"/>
    <xsl:param name="operator" select="'OPERATOR UNDEFINED'"/>
    <xsl:param name="organization" />
    <xsl:param name="audience" />
    <xsl:param name="hostname" />
    <xsl:param name="software" />
    <xsl:param name="filename" />
    <xsl:param name="delimiter" select="': '"/>
    <xsl:template match="/">
        <arcmetadata xmlns:arc="http://archive.org/arc/1.0/"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://archive.org/arc/1.0/ http://archive.org/arc/1.0/arc.xsd">
            <xsl:element name="dc:title" 
                    namespace="http://purl.org/dc/elements/1.1/">
                <xsl:value-of select="normalize-space($filename)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="arc:operator" 
                    namespace="http://archive.org/arc/1.0/">
                <xsl:value-of select="normalize-space($operator)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="dc:publisher" 
                    namespace="http://purl.org/dc/elements/1.1/">
                <xsl:value-of select="normalize-space($organization)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="dcterms:audience" 
                    namespace="http://purl.org/dc/terms/">
                <xsl:value-of select="normalize-space($audience)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="arc:software" 
                    namespace="http://archive.org/arc/1.0/">
                <xsl:value-of select="normalize-space($software)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="arc:hostname" 
                    namespace="http://archive.org/arc/1.0/">
                <xsl:value-of select="normalize-space($hostname)"/><xsl:text/>
            </xsl:element>
            <xsl:element name="arc:ip" namespace="http://archive.org/arc/1.0/">
                <xsl:value-of select="normalize-space($ip)"/><xsl:text/>
            </xsl:element>
            <xsl:apply-templates select="/crawl-order/meta"/>
            <xsl:apply-templates 
             select="/crawl-order/controller/map[@name='http-headers']/string"/>
            <xsl:apply-templates 
                select="/crawl-order/controller/newObject[@name='robots-honoring-policy']/string"/>
            <xsl:element name="dc:format" 
                    namespace="http://purl.org/dc/elements/1.1/">
                <xsl:text>ARC file version 1.1</xsl:text>
            </xsl:element>
            <xsl:element name="dcterms:conformsTo"
                    namespace="http://purl.org/dc/terms/">
                <xsl:attribute name="xsi:type"
                        namespace="http://www.w3.org/2001/XMLSchema-instance">
                    <xsl:text>dcterms:URI</xsl:text>
                </xsl:attribute>
                <xsl:text>
                    http://www.archive.org/web/researcher/ArcFileFormat.php
                </xsl:text>
            </xsl:element>
        </arcmetadata>
    </xsl:template>
    <xsl:template match="name">
        <xsl:element name="dcterms:isPartOf" 
                namespace="http://purl.org/dc/terms/">
            <xsl:value-of select="normalize-space(.)"/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="date">
        <xsl:element name="dcterms:created" 
                namespace="http://purl.org/dc/terms/">
            <xsl:attribute name="xsi:type" 
                    namespace="http://www.w3.org/2001/XMLSchema-instance">
                <xsl:text>dcterms:W3CDTF</xsl:text>
            </xsl:attribute>
            <xsl:text>TODO </xsl:text>
            <xsl:value-of select="normalize-space(.)"/><xsl:text/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="description">
        <xsl:element name="dc:description" 
                namespace="http://purl.org/dc/elements/1.1/">
            <xsl:value-of select="normalize-space(.)"/><xsl:text/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="string[@name='user-agent']">
        <xsl:element name="arc:http-header-user-agent" 
                namespace="http://archive.org/arc/1.0/">
            <xsl:value-of select="normalize-space(.)"/><xsl:text/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="string[@name='from']">
        <xsl:element name="arc:http-header-from" 
                namespace="http://archive.org/arc/1.0/">
            <xsl:value-of select="normalize-space(.)"/><xsl:text/>
        </xsl:element>
    </xsl:template>
    <xsl:template match="string[@name='type']">
        <xsl:element name="arc:robots" 
                namespace="http://archive.org/arc/1.0/">
            <xsl:value-of select="normalize-space(.)"/><xsl:text/>
        </xsl:element>
    </xsl:template>
    <!--Default handler.  Does nothing.
     -->
    <xsl:template match="text()"/>
</xsl:stylesheet>
