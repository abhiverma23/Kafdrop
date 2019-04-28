<#import "lib/template.ftl" as template>
<#import "/spring.ftl" as spring />
<@template.header "Topic: ${topic.name}: Messages">
   <style type="text/css">
       h1 { margin-bottom: 16px; }
       #messageFormPanel { margin-top: 16px; }
       #partitionSizes { margin-left: 16px; }
       .toggle-msg { float: left;}
   </style>

  <script src="/js/message-inspector.js"></script>
</@template.header>
<#setting number_format="0">


<h1>Topic Messages: <a href="/topic/${topic.name}">${topic.name}</a></h1>
<center><button><a href="#bottom">Go to bottom &#8681;</a></button></center>

<#assign selectedPartition=messageForm.partition!0?number>
<#assign selectedFormat=messageForm.format!defaultFormat>

<div id="partitionSizes">
    <#assign curPartition=topic.getPartition(selectedPartition).get()>
    <span class="label label-default">First Offset:</span> <span id="firstOffset">${curPartition.firstOffset}</span>
    <span class="label label-default">Last Offset:</span> <span id="lastOffset">${curPartition.size}</span>
    <span class="label label-default">Size:</span> <span id="partitionSize">${curPartition.size - curPartition.firstOffset}</span>
</div>

<div id="messageFormPanel" class="panel panel-default">
<form method="GET" action="/topic/${topic.name}/messages" id="messageForm" class="form-inline panel-body">

    <div class="form-group">
        <label for="partition">Partition</label>
        <select id="partition" name="partition">
        <#list topic.partitions as p>
            <option value="${p.id}" data-first-offset="${p.firstOffset}" data-last-offset="${p.size}" <#if p.id == selectedPartition>selected="selected"</#if>>${p.id}</option>
        </#list>
        </select>
    </div>

    <@spring.bind path="messageForm.offset"/>
    <div class="form-group ${spring.status.error?string("has-error", "")}">
        <label class="control-label" for="offset">Offset</label>
        <@spring.formInput path="messageForm.offset" attributes='class="form-control"'/>
        <#if spring.status.error>
            <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
        </#if>
    </div>

    <@spring.bind path="messageForm.count"/>
    <div class="form-group ${spring.status.error?string("has-error", "")}">
        <label class=control-label" for="count">Num Messages</label>
        <@spring.formInput path="messageForm.count" attributes='class="form-control ${spring.status.error?string("has-error", "")}"'/>
        <#if spring.status.error>
           <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
        </#if>
    </div>

    <@spring.bind path="messageForm.searchBy"/>
    <div class="form-group ${spring.status.error?string("has-error", "")}">
        <label class=control-label" for="searchBy">Message Contains</label>
        <@spring.formInput path="messageForm.searchBy" attributes='class="form-control ${spring.status.error?string("has-error", "")}"'/>
        <#if spring.status.error>
           <span class="text-danger"><i class="fa fa-times-circle"></i><@spring.showErrors "<br/>"/></span>
        </#if>
    </div>

    <div class="form-group">
        <label for="format">Message Format</label>
        <select id="format" name="format">
        <#list messageFormats as f>
            <option value="${f}"<#if f == selectedFormat>selected="selected"</#if>>${f}</option>
        </#list>
        </select>
    </div>

    <button class="btn btn-primary" type="submit"><i class="fa fa-search"></i> View Messages</button>

</form>
</div>

<@spring.bind path="messageForm.*"/>
<div id="message-display" class="container">
    <#if messages?? && messages?size gt 0>
    <#list messages as msg>
        <#--<#assign offset=messageForm.offset + msg_index>-->
        <div data-offset="${msg.offset}" class="message-detail">
            <span class="label label-default">Offset:</span> ${msg.offset}
            <span class="label label-default">Key:</span> ${msg.key!''}
            <span class="label label-default">Checksum/Computed:</span> <span <#if !msg.valid>class="text-danger"</#if>>${msg.checksum}/${msg.computedChecksum}</span>
            <span class="label label-default">Compression:</span> ${msg.compressionCodec}
            <div>
            <span class="bs-label">Headers:</span>
            <a href="#" class="toggle-msg" script="margin-top:500px;"><i class="fa fa-chevron-circle-right">&nbsp;</i></a>
            <pre class="message-body">${msg.headers!''}</pre>
            </div>
            <div>
            <span class="bs-label">Message:</span>
            <a href="#" class="toggle-msg"><i class="fa fa-chevron-circle-right">&nbsp;</i></a>
            <pre class="message-body">${msg.message!''}</pre>
            </div>
            <#--<hr>-->
        </div>
    </#list>
    <#elseif !(spring.status.error) && !(messageForm.empty)>
        No messages found in partition ${(messageForm.partition)!"PARTITION_NOT_SET"} at offset ${messageForm.offset}
    </#if>
</div>

<@template.footer/>
