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
  <link rel="self" type="application/atom+xml" href="${opt['site.host']}${contextPath}/atom.xml"/>
  <generator>${opt['site.version']}</generator>
  <copyright>${opt['site.copyright']}</copyright>
    <#list atom.entries as entry>
      <entry>
        <title>${entry.title}</title>
          <#if entry.image ??>
            <image>
              <url>${entry.image}</url>
            </image>
          </#if>
        <link rel="alternate" type="text/html" href="${opt['site.host']}${contextPath}/articles/${entry.id}"/>
        <id>${opt['site.host']}${contextPath}/articles/${entry.id}</id>
        <published>${entry.published?number_to_datetime}</published>
        <updated>${entry.updated?number_to_datetime}</updated>
        <summary>
          <![CDATA[${entry.summary}]]>
        </summary>
          <#list entry.categories as category>
            <category term="${category}" href="${opt['site.host']}${contextPath}/tags/${category}"/>
          </#list>
      </entry>
    </#list>
</feed>
