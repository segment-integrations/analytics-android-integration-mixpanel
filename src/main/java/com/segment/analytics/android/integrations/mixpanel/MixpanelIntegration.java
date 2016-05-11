package com.segment.analytics.android.integrations.mixpanel;

import android.app.Activity;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.AliasPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import static com.segment.analytics.internal.Utils.transform;

public class MixpanelIntegration extends Integration<MixpanelAPI> {
  private static final String VIEWED_EVENT_FORMAT = "Viewed %s Screen";
  public static final Factory FACTORY = new Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      Logger logger = analytics.logger(MIXPANEL_KEY);
      boolean consolidatedPageCalls = settings.getBoolean("consolidatedPageCalls", true);
      boolean trackAllPages = settings.getBoolean("trackAllPages", false);
      boolean trackCategorizedPages = settings.getBoolean("trackCategorizedPages", false);
      boolean trackNamedPages = settings.getBoolean("trackNamedPages", false);
      boolean isPeopleEnabled = settings.getBoolean("people", false);
      String token = settings.getString("token");
      Set<String> increments = getStringSet(settings, "increments");
      MixpanelAPI.People people;

      MixpanelAPI mixpanel = MixpanelAPI.getInstance(analytics.getApplication(), token);
      logger.verbose("MixpanelAPI.getInstance(context, %s);", token);
      if (isPeopleEnabled) {
        people = mixpanel.getPeople();
      } else {
        people = null;
      }

      return new MixpanelIntegration(
              mixpanel,
              people,
              isPeopleEnabled,
              consolidatedPageCalls,
              trackAllPages,
              trackCategorizedPages,
              trackNamedPages,
              token,
              logger,
              increments
      );
    }

    @Override public String key() {
      return MIXPANEL_KEY;
    }
  };
  private static final String MIXPANEL_KEY = "Mixpanel";

  static final Map<String, String> MAPPER;

  static {
    Map<String, String> mapper = new LinkedHashMap<>();
    mapper.put("email", "$email");
    mapper.put("phone", "$phone");
    mapper.put("firstName", "$first_name");
    mapper.put("lastName", "$last_name");
    mapper.put("name", "$name");
    mapper.put("username", "$username");
    mapper.put("createdAt", "$created");
    MAPPER = Collections.unmodifiableMap(mapper);
  }

  final MixpanelAPI mixpanel;
  final MixpanelAPI.People mixpanelPeople;
  final boolean isPeopleEnabled;
  final boolean consolidatedPageCalls;
  final boolean trackAllPages;
  final boolean trackCategorizedPages;
  final boolean trackNamedPages;
  final String token;
  final Logger logger;
  final Set<String> increments;

  static Set<String> getStringSet(ValueMap valueMap, Object key) {
    try {
      List<Object> incrementEvents = (List<Object>) valueMap.get(key);
      if (incrementEvents == null || incrementEvents.size() == 0) {
        return Collections.emptySet();
      }
      Set<String> stringSet = new HashSet<>(incrementEvents.size());
      for (int i = 0; i < incrementEvents.size(); i++) {
        stringSet.add((String) incrementEvents.get(i));
      }
      return stringSet;
    } catch (ClassCastException e) {
      return Collections.emptySet();
    }
  }

  public MixpanelIntegration(
          MixpanelAPI mixpanel,
          MixpanelAPI.People mixpanelPeople,
          boolean isPeopleEnabled,
          boolean consolidatedPageCalls,
          boolean trackAllPages,
          boolean trackCategorizedPages,
          boolean trackNamedPages,
          String token,
          Logger logger,
          Set<String> increments
  ) {
    this.mixpanel = mixpanel;
    this.mixpanelPeople = mixpanelPeople;
    this.isPeopleEnabled = isPeopleEnabled;
    this.consolidatedPageCalls = consolidatedPageCalls;
    this.trackAllPages = trackAllPages;
    this.trackCategorizedPages = trackCategorizedPages;
    this.trackNamedPages = trackNamedPages;
    this.token = token;
    this.logger = logger;
    this.increments = increments;
  }

  @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    super.onActivityCreated(activity, savedInstanceState);

    // This is needed to trigger a call to #checkIntentForInboundAppLink.
    // From Mixpanel's source, this won't trigger a creation of another instance. It caches
    // instances by the application context and token, both of which remain the same.
    MixpanelAPI.getInstance(activity, token);
  }

  @Override public MixpanelAPI getUnderlyingInstance() {
    return mixpanel;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    mixpanel.identify(userId);
    logger.verbose("mixpanel.identify(%s)", userId);
    JSONObject traits = new ValueMap(transform(identify.traits(), MAPPER)).toJsonObject();
    mixpanel.registerSuperProperties(traits);
    logger.verbose("mixpanel.registerSuperProperties(%s)", traits);

    if (!isPeopleEnabled) {
      return;
    }
    mixpanelPeople.identify(userId);
    logger.verbose("mixpanelPeople.identify(%s)", userId);
    mixpanelPeople.set(traits);
    logger.verbose("mixpanelPeople.set(%s)", traits);
  }

  @Override public void flush() {
    super.flush();
    mixpanel.flush();
    logger.verbose("mixpanel.flush()");
  }

  @Override public void reset() {
    super.reset();
    mixpanel.reset();
    logger.verbose("mixpanel.reset()");
  }

  @Override public void alias(AliasPayload alias) {
    super.alias(alias);
    String previousId = alias.previousId();
    if (previousId.equals(alias.anonymousId())) {
      // If the previous ID is an anonymous ID, pass null to mixpanel, which has generated it's own
      // anonymous ID
      previousId = null;
    }
    mixpanel.alias(alias.userId(), previousId);
    logger.verbose("mixpanel.alias(%s, %s)", alias.userId(), previousId);
  }

  @Override public void screen(ScreenPayload screen) {
    if (consolidatedPageCalls) {
      Properties properties = screen.properties();
      properties.put("name", screen.event());
      event("Loaded a Screen", properties);
      return;
    }
    if (trackAllPages) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.event()), screen.properties());
    } else if (trackCategorizedPages && !isNullOrEmpty(screen.category())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.category()), screen.properties());
    } else if (trackNamedPages && !isNullOrEmpty(screen.name())) {
      event(String.format(VIEWED_EVENT_FORMAT, screen.name()), screen.properties());
    }
  }

  @Override public void track(TrackPayload track) {
    String event = track.event();

    event(event, track.properties());

    if (increments.contains(event) && isPeopleEnabled) {
      mixpanelPeople.increment(event, 1);
      mixpanelPeople.set("Last " + event, new Date());
    }
  }

  void event(String name, Properties properties) {
    JSONObject props = properties.toJsonObject();
    mixpanel.track(name, props);
    logger.verbose("mixpanel.track(%s, %s)", name, props);
    if (!isPeopleEnabled) {
      return;
    }
    double revenue = properties.revenue();
    if (revenue == 0) {
      return;
    }
    mixpanelPeople.trackCharge(revenue, props);
    logger.verbose("mixpanelPeople.trackCharge(%s, %s)", revenue, props);
  }
}
