<?xml version="1.0" encoding="UTF-8"?>
<!--
    Transform order file and crawler metadata into body for a warcinfo record.
    TODO: Outputs XML.  Output Key-Value pairs.
    $Id: arcMetaheaderBody.xsl 2113 2004-05-21 21:30:10Z stack-sf $
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"
        omit-xml-declaration="no" standalone="yes"/>
    <xsl:strip-space elements="*"/>
    <!--Params that can be overridden.
     -->
    <xsl:param name="ip"/>
    <xsl:param name="hostname"/>
    <xsl:param name="software"/>
    <!--Its hard to do title because this stylesheet is
        run inside in the writer processor.  The writer
        processor passes the result of the transform to
        the WARCWriterPool.  The actual name of the
        WARC file we're being written into is hidden
        behind the WARCWriterPool; its not available
        to us.
     -->
    <xsl:param name="title"/>
    <xsl:param name="delimiter"/>
    <xsl:template match="/">
        <warcinfo xmlns="http://archive.org/warc/0.12/warcinfo"
            xmlns:warc="http://archive.org/warc/0.12/warcinfo"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://archive.org/warc/0.12/warcinfo http://www.archive.org/warc/0.12/warcinfo.xsd">
            <xsl:call-template name="element-printer">
                <xsl:with-param name="name" select="'dc:title'"/>
                <xsl:with-param name="value" select="$title"/>
                <xsl:with-param name="ns" select="'http://purl.org/dc/elements/1.1/'"/>
            </xsl:call-template>
            <xsl:call-template name="element-printer">
                <xsl:with-param name="name" select="'warc:software'"/>
                <xsl:with-param name="value" select="$software"/>
                <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/warcinfo'"/>
            </xsl:call-template>
            <xsl:call-template name="element-printer">
                <xsl:with-param name="name" select="'warc:hostname'"/>
                <xsl:with-param name="value" select="$hostname"/>
                <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/warcinfo'"/>
            </xsl:call-template>
            <xsl:call-template name="element-printer">
                <xsl:with-param name="name" select="'warc:ip'"/>
                <xsl:with-param name="value" select="$ip"/>
                <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/warcinfo'"/>
            </xsl:call-template>
            <xsl:apply-templates select="/crawl-order/meta"/>
            <xsl:apply-templates select="/crawl-order/controller/map[@name='http-headers']/string"/>
            <xsl:apply-templates select="/crawl-order/controller/newObject[@name='robots-honoring-policy']/string"/>
            <xsl:call-template name="element-printer">
                <xsl:with-param name="name" select="'dc:format'"/>
                <xsl:with-param name="value" select="'WARC file version 0.12'"/>
                <xsl:with-param name="ns" select="'http://purl.org/dc/elements/1.1/'"/>
            </xsl:call-template>
            <xsl:element name="dcterms:conformsTo" namespace="http://purl.org/dc/terms/">
                <xsl:attribute name="xsi:type" namespace="http://www.w3.org/2001/XMLSchema-instance">
                    <xsl:text>dcterms:URI</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="'http://www.archive.org/warc/0.12/warc_file_format.html'"/>
            </xsl:element>
        </warcinfo>
    </xsl:template>
    <xsl:template match="operator">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="concat('warc:', local-name())"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/warcinfo'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="organization">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="'dc:publisher'"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://purl.org/dc/elements/1.1/'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="audience">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="concat('dcterms:', local-name())"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://purl.org/dc/terms/'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="name">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="'dcterms:isPartOf'"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://purl.org/dc/terms/'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="date">
        <xsl:variable name="dateStr" select="normalize-space(.)"/>
        <xsl:if test="$dateStr != ''">
            <xsl:variable name="year" select="substring($dateStr, 0, 5)"/>
            <xsl:variable name="month" select="substring($dateStr, 5, 2)"/>
            <xsl:variable name="day" select="substring($dateStr, 7, 2)"/>
            <xsl:variable name="hour" select="substring($dateStr, 9, 2)"/>
            <xsl:variable name="minute" select="substring($dateStr,11, 2)"/>
            <xsl:variable name="second" select="substring($dateStr,13, 2)"/>
            <xsl:element name="dc:date" namespace="http://purl.org/dc/elements/1.1/">
                <xsl:attribute name="xsi:type" namespace="http://www.w3.org/2001/XMLSchema-instance">
                    <xsl:text>dcterms:W3CDTF</xsl:text>
                </xsl:attribute>
                <xsl:value-of select="$year"/>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$month"/>
                <xsl:text>-</xsl:text>
                <xsl:value-of select="$day"/>
                <xsl:text>T</xsl:text>
                <xsl:value-of select="$hour"/>
                <xsl:text>:</xsl:text>
                <xsl:value-of select="$minute"/>
                <xsl:text>:</xsl:text>
                <xsl:value-of select="$second"/>
                <xsl:text>+00:00</xsl:text>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <xsl:template match="description">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="'dc:description'"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://purl.org/dc/elements/1.1/'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="string[@name='from'] | string[@name='user-agent']">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="concat('warc:http-header-', @name)"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/warcinfo'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template match="string[@name='type']">
        <xsl:call-template name="element-printer">
            <xsl:with-param name="name" select="'warc:robots'"/>
            <xsl:with-param name="value" select="."/>
            <xsl:with-param name="ns" select="'http://archive.org/warc/0.12/'"/>
        </xsl:call-template>
    </xsl:template>
    <!--Element printer.
        Prints nought if element value is empty.
     -->
    <xsl:template name="element-printer">
        <xsl:param name="name"/>
        <xsl:param name="value"/>
        <xsl:param name="ns"/>
        <xsl:variable name="normalized-value">
            <xsl:value-of select="normalize-space($value)"/>
        </xsl:variable>
        <xsl:if test="$normalized-value != ''">
            <xsl:element name="{$name}" namespace="{$ns}">
                <xsl:value-of select="$normalized-value"/><xsl:text/>
            </xsl:element>
        </xsl:if>
    </xsl:template>
    <!--Default handler.  Does nothing.
     -->
    <xsl:template match="text()"/>
</xsl:stylesheet>
