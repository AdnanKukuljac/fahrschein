package org.zalando.fahrschein.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.concurrent.Immutable;
import java.util.Date;

@Immutable
public final class Metadata {
    private final String eventType;
    private final String eid;
    private final Date occuredAt;
    private final Date receivedAt;
    private final String flowId;

    @JsonCreator
    @Deprecated
    private Metadata(@JsonProperty("event_type") String eventType, @JsonProperty("eid") String eid, @JsonProperty("occured_at") Date occuredAt, @JsonProperty("received_at") Date receivedAt, @JsonProperty("flow_id") String flowId) {
        this.eventType = eventType;
        this.eid = eid;
        this.occuredAt = occuredAt;
        this.receivedAt = receivedAt;
        this.flowId = flowId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEid() {
        return eid;
    }

    public Date getOccuredAt() {
        return occuredAt;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public String getFlowId() {
        return flowId;
    }
}
