<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:ext="http://org.archive.crawler.admin.TextUtils"
	xmlns:java="java" >

<xsl:output method="html" />
<xsl:param name="orderFilePath" />

<xsl:template match="/">

<xsl:variable name="width" select="48" />
<!--
<xsl:for-each select="//processor">
<xsl:value-of select="@name"/><br></br>
<xsl:value-of select="@class"/><br></br>
<xsl:value-of select="@next"/><br></br>
</xsl:for-each>
-->
<form>
<table>
<tr>
<td>
<strong>CRAWL NAME :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//crawl-order/@name" 
	value="{//crawl-order/@name}" />
</td>
</tr> 
<tr>
<td>
<strong>COMMENT :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//@comment" 
	value="{//@comment}" />
</td>
</tr>
 
<xsl:for-each select="//behavior/http-headers/*">
<tr>
<td>
<strong><xsl:value-of select="java:toUpperCase(name())" /> :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//behavior/http-headers/@User-Agent" 
	value="{//behavior/http-headers/@User-Agent}" />
</td>
</tr>
</xsl:for-each>

<tr>
<td>
<strong>DISK-PATH :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//disk/@path" 
	value="{//disk/@path}" />
</td>
</tr>

<tr>
<td>
<strong>ARC-PREFIX :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//arc-file/@prefix" 
	value="{//arc-files/@prefix}" />
</td>
</tr>

<tr>
<td>
<strong>ARC-COMPRESSION-IN-USE :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//compression/@use" 
	value="{//compression/@use}" />
</td>
</tr>

<tr>
<td>
<strong>MAX-ARC-SIZE-IN-BYTES :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//arc-files/@max-size-bytes" 
	value="{//arc-files/@max-size-bytes}" />
</td>
</tr>

<tr>
<td>
<strong>MAX-LINK-HOPS :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//@max-link-hops" 
	value="{//@max-link-hops}" />
</td>
</tr>

<tr>
<td>
<strong>MAX-TRANS-HOPS :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//@max-trans-hops" 
	value="{//@max-trans-hops}" />
</td>
</tr>

<xsl:for-each select="//limits/*">
<tr>
<td>
<strong><xsl:value-of select="java:toUpperCase(name())" /> :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//limits/{name()}/@value" 
	value="{@*}" />
</td>
</tr>
</xsl:for-each>

<tr>
<td>
<strong>FILTER MODE :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//@mode" 
	value="{//@mode}" />
</td>
</tr>

<tr>
<td>
<strong>SEEDS-FILE :</strong></td>
<td><input 
	type="text" 
	size="{$width}" 
	name="//selector/seeds/@src" 
	value="{//selector/seeds/@src}" />
</td>
</tr>
<tr>
<td>
<strong>SEEDS :</strong></td>
<td><textarea 
	rows="8" 
	cols="{$width}" 
	wrap="off"
	name="seeds" >
<xsl:variable name="seedsFile" select="concat($orderFilePath, //selector/seeds/@src)" />
<xsl:value-of select="ext:getText($seedsFile)" />
</textarea>
 <input 
	type="submit"
	value="Update Order" >
</input>

</td>
</tr>
</table>
<input 
	type="hidden" 
	name="CrawlerAction"
	value="2" >
</input>
</form>

</xsl:template>

</xsl:stylesheet>
