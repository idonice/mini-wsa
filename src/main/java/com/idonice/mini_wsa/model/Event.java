package com.idonice.mini_wsa.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "events")
public class Event {

    @Id
    private String eventId;

    private Instant timestamp;
    private Long configId;
    private String policyId;
    private String clientIp;
    private String hostname;
    private String path;
    private String method;
    private Integer statusCode;
    private String userAgent;

    @Enumerated(EnumType.STRING)
    private Action action;

    private Long requestSize;
    private Long responseSize;

    @Embedded
    private Rule rule;

    @Embedded
    private GeoLocation geoLocation;

    private String attackType;
    private int threatScore;
    private Instant receivedAt;

    // Required by JPA: Hibernate creates an empty object, then fills the fields.
    // Public because the ingest service also builds new events this way.
    public Event() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public Long getRequestSize() { return requestSize; }
    public void setRequestSize(Long requestSize) { this.requestSize = requestSize; }

    public Long getResponseSize() { return responseSize; }
    public void setResponseSize(Long responseSize) { this.responseSize = responseSize; }

    public Rule getRule() { return rule; }
    public void setRule(Rule rule) { this.rule = rule; }

    public GeoLocation getGeoLocation() { return geoLocation; }
    public void setGeoLocation(GeoLocation geoLocation) { this.geoLocation = geoLocation; }

    public String getAttackType() { return attackType; }
    public void setAttackType(String attackType) { this.attackType = attackType; }

    public int getThreatScore() { return threatScore; }
    public void setThreatScore(int threatScore) { this.threatScore = threatScore; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
