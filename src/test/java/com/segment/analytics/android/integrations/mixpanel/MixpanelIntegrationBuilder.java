package com.segment.analytics.android.integrations.mixpanel;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Analytics;
import com.segment.analytics.integrations.Logger;
import java.util.Collections;
import java.util.Set;

class MixpanelIntegrationBuilder {
  private MixpanelAPI mixpanel;
  private MixpanelAPI.People mixpanelPeople;
  private boolean isPeopleEnabled;
  private boolean consolidatedPageCalls;
  private boolean trackAllPages;
  private boolean trackCategorizedPages;
  private boolean trackNamedPages;
  private String token;
  private Logger logger;
  private Set<String> increments;
  private boolean setAllTraitsByDefault;
  private Set<String> peopleProperties;
  private Set<String> superProperties;

  MixpanelIntegrationBuilder() {
    isPeopleEnabled = false;
    consolidatedPageCalls = true;
    trackAllPages = false;
    trackCategorizedPages = false;
    trackNamedPages = false;
    token = "foo";
    increments = Collections.emptySet();
    setAllTraitsByDefault = true;
    peopleProperties = Collections.emptySet();
    superProperties = Collections.emptySet();
    logger = Logger.with(Analytics.LogLevel.DEBUG);
  }

  MixpanelIntegrationBuilder setMixpanel(MixpanelAPI mixpanel) {
    this.mixpanel = mixpanel;
    return this;
  }

  MixpanelIntegrationBuilder setMixpanelPeople(MixpanelAPI.People mixpanelPeople) {
    this.mixpanelPeople = mixpanelPeople;
    return this;
  }

  MixpanelIntegrationBuilder setIsPeopleEnabled(boolean isPeopleEnabled) {
    this.isPeopleEnabled = isPeopleEnabled;
    return this;
  }

  MixpanelIntegrationBuilder setConsolidatedPageCalls(boolean consolidatedPageCalls) {
    this.consolidatedPageCalls = consolidatedPageCalls;
    return this;
  }

  MixpanelIntegrationBuilder setTrackAllPages(boolean trackAllPages) {
    this.trackAllPages = trackAllPages;
    return this;
  }

  MixpanelIntegrationBuilder setTrackCategorizedPages(boolean trackCategorizedPages) {
    this.trackCategorizedPages = trackCategorizedPages;
    return this;
  }

  MixpanelIntegrationBuilder setTrackNamedPages(boolean trackNamedPages) {
    this.trackNamedPages = trackNamedPages;
    return this;
  }

  public MixpanelIntegrationBuilder setToken(String token) {
    this.token = token;
    return this;
  }

  public MixpanelIntegrationBuilder setLogger(Logger logger) {
    this.logger = logger;
    return this;
  }

  MixpanelIntegrationBuilder setIncrements(Set<String> increments) {
    this.increments = increments;
    return this;
  }

  MixpanelIntegrationBuilder setSetAllTraitsByDefault(boolean setAllTraitsByDefault) {
    this.setAllTraitsByDefault = setAllTraitsByDefault;
    return this;
  }

  MixpanelIntegrationBuilder setPeopleProperties(Set<String> peopleProperties) {
    this.peopleProperties = peopleProperties;
    return this;
  }

  MixpanelIntegrationBuilder setSuperProperties(Set<String> superProperties) {
    this.superProperties = superProperties;
    return this;
  }

  MixpanelIntegration createMixpanelIntegration() {
    return new MixpanelIntegration(mixpanel, mixpanelPeople, isPeopleEnabled, consolidatedPageCalls,
        trackAllPages, trackCategorizedPages, trackNamedPages, token, logger, increments,
        setAllTraitsByDefault, peopleProperties, superProperties);
  }
}