package com.tokbox.android.IB.events;

import com.tokbox.android.IB.events.ActiveFan;

import java.util.HashMap;

public class ActiveBroadcast {
    private Boolean producerActive;
    private Boolean hostActive = false;
    private Boolean celebrityActive  = false;
    private Boolean archiving;
    private Boolean hlsEnabled;
    private int interactiveLimit;
    private String name;
    private String hlsUrl;
    private String status;
    private HashMap<String, String> startImage;
    private HashMap<String, ActiveFan> activeFans;

    private ActiveBroadcast() {}

    public ActiveBroadcast(String name, String status, Boolean producerActive, Boolean hostActive, Boolean celebrityActive, Boolean archiving, Boolean hlsEnabled, int interactiveLimit, HashMap<String, String> startImage, HashMap<String, ActiveFan> activeFans, String hlsUrl) {
        this.name = name;
        this.status = status;
        this.producerActive = producerActive;
        this.archiving = archiving;
        this.hlsEnabled = hlsEnabled;
        this.hlsUrl = hlsUrl;
        this.interactiveLimit = interactiveLimit;
        this.startImage = startImage;
        this.hostActive = hostActive;
        this.celebrityActive = celebrityActive;
        this.activeFans = activeFans;
    }

    public void setProducerActive(Boolean producerActive) {
        this.producerActive = producerActive;
    }

    public void setHostActive(Boolean hostActive) {
        this.hostActive = hostActive;
    }

    public void setCelebrityActive(Boolean celebrityActive) {
        this.celebrityActive = celebrityActive;
    }

    public void setArchiving(Boolean archiving) {
        this.archiving = archiving;
    }

    public void setHlsEnabled(Boolean hlsEnabled) {
        this.hlsEnabled = hlsEnabled;
    }

    public void setInteractiveLimit(int interactiveLimit) {
        this.interactiveLimit = interactiveLimit;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStartImage(HashMap<String, String> startImage) {
        this.startImage = startImage;
    }

    public Boolean getHostActive() {
        return hostActive;
    }

    public Boolean getCelebrityActive() {
        return celebrityActive;
    }

    public HashMap<String, String> getStartImage() {
        return startImage;
    }

    public Boolean getProducerActive() {
        return producerActive;
    }

    public Boolean getArchiving() {
        return archiving;
    }

    public Boolean getHlsEnabled() {
        return hlsEnabled;
    }

    public int getInteractiveLimit() {
        return interactiveLimit;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public HashMap<String, ActiveFan> getActiveFans() {
        return activeFans;
    }

    public void setActiveFans(HashMap<String, ActiveFan> activeFans) {
        this.activeFans = activeFans;
    }

    public String getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }
}