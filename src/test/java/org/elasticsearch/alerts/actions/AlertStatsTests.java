/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.alerts.actions;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.alerts.AbstractAlertingTests;
import org.elasticsearch.alerts.client.AlertsClient;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsRequest;
import org.elasticsearch.alerts.transport.actions.stats.AlertsStatsResponse;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.hamcrest.core.IsEqual.equalTo;


/**
 */
public class AlertStatsTests extends AbstractAlertingTests {

    @Test
    public void testStartedStats() throws Exception {
        AlertsStatsRequest alertsStatsRequest = alertClient().prepareAlertsStats().request();
        AlertsStatsResponse response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getAlertActionManagerQueueSize(), equalTo(0L));
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(0L));
        assertThat(response.getAlertActionManagerLargestQueueSize(), equalTo(0L));
    }

    @Test
    public void testAlertCountStats() throws Exception {
        AlertsClient alertsClient = alertClient();

        AlertsStatsRequest alertsStatsRequest = alertsClient.prepareAlertsStats().request();
        AlertsStatsResponse response = alertsClient.alertsStats(alertsStatsRequest).actionGet();

        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());

        SearchRequest searchRequest = createTriggerSearchRequest("my-index").source(searchSource().query(termQuery("field", "value")));
        BytesReference alertSource = createAlertSource("* * * * * ? *", searchRequest, "hits.total == 1");
        alertClient().preparePutAlert("testAlert")
                .setAlertSource(alertSource)
                .get();

        response = alertClient().alertsStats(alertsStatsRequest).actionGet();

        //Wait a little until we should have queued an action
        TimeValue waitTime = new TimeValue(30, TimeUnit.SECONDS);
        Thread.sleep(waitTime.getMillis());

        assertTrue(response.isAlertActionManagerStarted());
        assertTrue(response.isAlertManagerStarted());
        assertThat(response.getNumberOfRegisteredAlerts(), equalTo(1L));
        //assertThat(response.getAlertActionManagerLargestQueueSize(), greaterThan(0L));
    }
}
