<#import "lib/template.ftl" as template>
<@template.header "Broker List"/>

<script src="/js/powerFilter.js"></script>

<#setting number_format="0">
    <div>
        <h2>Kafka Cluster Overview</h2>

        <div id="zookeeper">
            <b>Zookeeper Hosts:</b> <#list zookeeper.connectList as z>${z}<#if z_has_next>, </#if></#list>
        </div>

        <div id="brokers">
            <h3>${brokers?size} Brokers</h3>
            <table class="bs-table default">
                <thead>
                <tr>
                    <th>ID</th>
                    <th>Host</th>
                    <th>Port</th>
                    <th>JMX Port</th>
                    <th>Version</th>
                    <th>Start Time</th>
                    <th>Controller?</th>
                </tr>
                </thead>
                <tbody>
                <#if brokers?size == 0>
                    <tr>
                        <td class="error" colspan="7">No brokers available!</td>
                    </tr>
                </#if>
                <#list brokers as b>
                <tr>
                    <td><a href="/broker/${b.id}"><i class="fa fa-info-circle fa-lg"></i> ${b.id}</a></td>
                    <td>${b.host}</td>
                    <td>${b.port?string}</td>
                    <td>${b.jmxPort?string}</td>
                    <td>${b.version}</td>
                    <td>${b.timestamp?string["yyyy-MM-dd HH:mm:ss.SSSZ"]}</td>
                    <td><@template.yn b.controller/></td>
                </tr>
                </#list>
                </tbody>
            </table>
        </div>

        <div id="topics">
            <h3>${topics?size} Topics</h3>
            <table class="bs-table default">
                <thead align="left">
                <tr>
                    <th>
                        Topic Name

                        <span style="font-weight:normal;">
                            &nbsp;<INPUT id='filter' size=25 NAME='searchRow' title='Just type to filter the topics' placeholder="Just type to filter the topics">&nbsp;
                            <span id="rowCount"></span>
                        </span>
                    </th>
                    <th>
                        Partitions
                        <a title="Number of partitions in the topic"
                           data-toggle="tooltip" data-placement="top" href="#"
                        ><i class="fa fa-question-circle"></i></a>
                    </th>
                    <th>
                        % Preferred
                        <a title="Percent of partitions where the preferred broker has been assigned leadership"
                           data-toggle="tooltip" data-placement="top" href="#"
                        ><i class="fa fa-question-circle"></i></a>
                    </th>
                    <th>
                        # Under Replicated
                        <a title="Number of partition replicas that are not in sync with the primary partition"
                           data-toggle="tooltip" data-placement="top" href="#"
                        ><i class="fa fa-question-circle"></i></a>
                    </th>
                    <th>Custom Config?</th>
                    <#--<th>Consumers</th>-->
                </tr>
                </thead>
                <tbody>
                <#if topics?size == 0>
                <tr>
                    <td colspan="5">No topics available</td>
                </tr>
                </#if>
                <#list topics as t>
                <tr class="dataRow">
                    <td><a class="bs-btn info" href="/topic/${t.name}" title="Topic Info"><i class="fa fa-gears"></i></a> <a class="bs-btn success" href="/topic/${t.name}/messages" title="View Messages"><i class="fa fa-envelope"></i> ${t.name}</a></td>
                    <td>${t.partitions?size}</td>
                    <td <#if t.preferredReplicaPercent lt 1.0>class="warn"</#if>>${t.preferredReplicaPercent?string.percent}</td>
                    <td <#if t.underReplicatedPartitions?size gt 0>class="warn"</#if>>${t.underReplicatedPartitions?size}</td>
                    <td><@template.yn t.config?size gt 0/></td>
                    <#--<td>${t.consumers![]?size}</td>-->
                </tr>
                </#list>
                </tbody>
            </table>
        </div>
    </div>

<@template.footer/>

<script>
    $(document).ready(function() {
        $('#filter').focus();

    <#if filter??>
        $('#filter').val('${filter}');
    </#if>
        $('[data-toggle="tooltip"]').tooltip()
    });
</script>