<%@ include file="/common/taglibs.jsp"%>

<head>
    <title><fmt:message key="logEventList.title"/></title>
    <meta name="heading" content="<fmt:message key='logEventList.heading'/>"/>
    <meta name="menu" content="LogEventMenu"/>
</head>

<c:set var="buttons">
    <input type="button" style="margin-right: 5px" class="button"
        onclick="location.href='<c:url value="/editLogEvent.html"/>'"
        value="<fmt:message key="button.add"/>"/>

    <input type="button" class="button" onclick="location.href='<c:url value="/mainMenu.html"/>'"
        value="<fmt:message key="button.done"/>"/>
</c:set>

<c:out value="${buttons}" escapeXml="false" />

<display:table name="logEvents" class="table" requestURI="" id="logEventList" export="true" pagesize="25">
    <display:column property="id" sortable="true" href="editLogEvent.html" media="html"
        paramId="id" paramProperty="id" titleKey="logEvent.id"/>
    <display:column property="id" media="csv excel xml pdf" titleKey="logEvent.id"/>
    <display:column property="message" sortable="true" titleKey="logEvent.message"/>
    <display:column sortProperty="timestamp" sortable="true" titleKey="logEvent.timestamp">
         <fmt:formatDate value="${logEventList.timestamp}" pattern="${datePattern}"/>
    </display:column>
    <display:column property="groupId" sortable="true" titleKey="logEvent.groupId"/>
    <display:column property="level" sortable="true" titleKey="logEvent.level"/>
    <display:column property="instanceId" sortable="true" titleKey="logEvent.instanceId"/>
    <display:column property="bioDatasource" sortable="true" titleKey="logEvent.bioDatasource"/>
    <display:column property="user" sortable="true" titleKey="logEvent.user"/>
    <display:column property="infoAsJSON" sortable="true" titleKey="logEvent.infoAsJSON"/>
    <display:column property="messageParams" sortable="true" titleKey="logEvent.messageParams"/>

    <display:setProperty name="paging.banner.item_name"><fmt:message key="logEventList.logEvent"/></display:setProperty>
    <display:setProperty name="paging.banner.items_name"><fmt:message key="logEventList.logEvents"/></display:setProperty>

    <display:setProperty name="export.excel.filename"><fmt:message key="logEventList.title"/>.xls</display:setProperty>
    <display:setProperty name="export.csv.filename"><fmt:message key="logEventList.title"/>.csv</display:setProperty>
    <display:setProperty name="export.pdf.filename"><fmt:message key="logEventList.title"/>.pdf</display:setProperty>
</display:table>

<c:out value="${buttons}" escapeXml="false" />

<script type="text/javascript">
    highlightTableRows("logEventList");
</script>
