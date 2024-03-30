<#-- @ftlvariable name="atom" type="cn.taketoday.blog.model.feed.Atom" -->
<#-- @ftlvariable name="author" type="cn.taketoday.blog.model.Blogger" -->
<#-- @ftlvariable name="opt" type="java.util.Map" -->
<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>${opt['site.name']}</title>
  <id>${opt['site.host']}</id>
  <subtitle>${opt['site.subTitle']}</subtitle>
  <author>
    <name>${author.name}</name>
  </author>
  <updated>${atom.updated?number_to_datetime}</updated>
  <link rel="alternate" type="text/html" href="${opt['site.host']}"/>
  <link rel="self" type="application/atom+xml" href="${opt['site.host']}/feed.atom"/>
  <generator>${opt['site.version']}</generator>
  <copyright><![CDATA[${opt['site.copyright']}]]></copyright>
    <#list atom.entries as entry>
      <entry>
        <title>${entry.title}</title>
          <#if entry.image ??>
            <image>
              <url>${entry.image}</url>
            </image>
          </#if>
        <link rel="alternate" type="text/html" href="${opt['site.host']}/articles/${entry.uri}"/>
        <id>${opt['site.host']}/articles/${entry.uri}</id>
        <published>${entry.published}</published>
        <updated>${entry.updated}</updated>
        <summary>
          <![CDATA[${entry.summary}]]>
        </summary>
          <#list entry.categories as category>
            <category term="${category}" href="${opt['site.host']}/tags/${category}"/>
          </#list>
      </entry>
    </#list>
</feed>
