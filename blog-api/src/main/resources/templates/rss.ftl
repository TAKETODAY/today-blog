<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:atom="http://www.w3.org/2005/Atom">
  <channel>
    <title>${opt['site.name']}</title>
    <link>${opt['site.host']}</link>
    <atom:link href="${opt['site.host']}${contextPath}/rss.xml" rel="self" type="application/rss+xml"/>
    <language>zh_CN</language>
    <description>${opt['site.subTitle']}</description>
    <webMaster>${author.email}</webMaster>
    <managingEditor>${author.email}</managingEditor>
    <generator>${opt['site.version']}</generator>
    <lastBuildDate>${rss.lastBuildDate?number_to_datetime}</lastBuildDate>
    <copyright>${opt['site.copyright']}</copyright>
      <#list rss.items as item>
        <item>
          <title>${item.title}</title>
            <#if item.image ??>
              <image>
                <url>${item.image}</url>
              </image>
            </#if>
          <author>${author.name}</author>
          <link>${opt['site.host']}${contextPath}/articles/${item.id}</link>
          <description><![CDATA[${item.summary}]]></description>
          <pubDate>${item.pubDate?number_to_datetime}</pubDate>
          <guid>${opt['site.host']}${contextPath}/articles/${item.id}</guid>
            <#list item.categories as category>
              <category>${category}</category>
            </#list>
        </item>
      </#list>
  </channel>
</rss>
