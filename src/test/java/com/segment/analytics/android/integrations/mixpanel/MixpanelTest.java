package com.segment.analytics.android.integrations.mixpanel;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.AliasPayloadBuilder;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Utils.createTraits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MixpanelAPI.class)
public class MixpanelTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanel;
  @Mock Application context;
  Logger logger;
  @Mock MixpanelAPI.People mixpanelPeople;
  @Mock Analytics analytics;

  MixpanelIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(MixpanelAPI.class);
    when(MixpanelAPI.getInstance(context, "foo")).thenReturn(mixpanel);
    when(mixpanel.getPeople()).thenReturn(mixpanelPeople);
    logger = Logger.with(Analytics.LogLevel.DEBUG);
    when(analytics.logger("Mixpanel")).thenReturn(logger);
    when(analytics.getApplication()).thenReturn(context);

    integration = new MixpanelIntegration(mixpanel, null, false, true, false, false, false, "foo", logger,
        Collections.<String>emptySet());
  }

  @Test public void factory() {
    ValueMap settings = new ValueMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true);

    MixpanelIntegration integration =
        (MixpanelIntegration) MixpanelIntegration.FACTORY.create(settings, analytics);

    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
    verify(mixpanel, never()).getPeople();

    assertThat(integration.token).isEqualTo("foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    assertThat(integration.increments).isNotNull().isEmpty();
  }

  @Test public void initializeWithIncrementsAndPeople() throws IllegalStateException {
    ValueMap settings = new ValueMap().putValue("token", "foo")
        .putValue("people", true)
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true)
        .putValue("increments", Arrays.asList("baz", "qaz", "qux"));

    MixpanelIntegration integration =
        (MixpanelIntegration) MixpanelIntegration.FACTORY.create(settings, analytics);

    verifyStatic();
    MixpanelAPI.getInstance(context, "foo");
    verify(mixpanel).getPeople();
    assertThat(integration.token).isEqualTo("foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    verify(mixpanel).getPeople();
    // Don't use containsExactly since the ordering differs between JDK versions.
    assertThat(integration.increments).hasSize(3).contains("qux", "baz", "qaz");
  }

  @Test public void activityCreate() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivityCreated(activity, bundle);
    verifyStatic();
    MixpanelAPI.getInstance(activity, "foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityStart() {
    Activity activity = mock(Activity.class);
    integration.onActivityStarted(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityResume() {
    Activity activity = mock(Activity.class);
    integration.onActivityResumed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityPause() {
    Activity activity = mock(Activity.class);
    integration.onActivityPaused(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityStop() {
    Activity activity = mock(Activity.class);
    integration.onActivityStopped(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activitySaveInstance() {
    Activity activity = mock(Activity.class);
    Bundle bundle = mock(Bundle.class);
    integration.onActivitySaveInstanceState(activity, bundle);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void activityDestroy() {
    Activity activity = mock(Activity.class);
    integration.onActivityDestroyed(activity);
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screen() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, false, false, false, "foo", logger,
            Collections.<String>emptySet());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenAllPages() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, false, false, "foo", logger,
            Collections.<String>emptySet());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanel).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenConsolidatedPages() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, true, false, false, true, "foo", logger,
            Collections.<String>emptySet());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    Properties properties = new Properties();
    properties.put("name", "foo");
    verify(mixpanel).track(eq("Loaded a Screen"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenNamedPages() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, false, false, true, "foo", logger,
            Collections.<String>emptySet());

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanel).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenCategorizedPages() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, false, true, false, "foo", logger,
            Collections.<String>emptySet());

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verify(mixpanel).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verify(mixpanel).track(eq("foo"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void trackIncrement() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, true, true, "foo", logger,
            Collections.singleton("baz"));

    integration.track(new TrackPayloadBuilder().event("baz").build());

    verify(mixpanel).track(eq("baz"), jsonEq(new JSONObject()));
    verify(mixpanelPeople).increment("baz", 1);
    verify(mixpanelPeople).set(eq("Last baz"), any());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void trackIncrementWithoutPeople() {
    // Disabling people should do a regular track call
    integration.track(new TrackPayloadBuilder().event("baz").build());
    verify(mixpanel).track(eq("baz"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void alias() {
    integration.alias(new AliasPayloadBuilder().traits(createTraits("foo")).newId("bar") //
        .build());
    verify(mixpanel).alias("bar", "foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void aliasWithoutAnonymousId() {
    String mpDistinctId = "mpDistinctId";
    when(mixpanel.getDistinctId()).thenReturn(mpDistinctId);
    integration.alias(new AliasPayloadBuilder().traits(new Traits() //
        .putValue("anonymousId", "qaz")).newId("qux").build());
    verify(mixpanel).alias("qux", mpDistinctId);
    verify(mixpanel).getDistinctId();
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identify() {
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanel).identify("foo");
    verify(mixpanel).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithoutUserId() {
    Traits traits = createTraits();
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanel, never()).identify(anyString());
    verify(mixpanel).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithPeople() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, true, true, "foo", logger,
            Collections.<String>emptySet());
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanel).identify("foo");
    verify(mixpanel).registerSuperProperties(jsonEq(traits.toJsonObject()));
    verify(mixpanelPeople).identify("foo");
    verify(mixpanelPeople).set(jsonEq(traits.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithSuperProperties() throws JSONException {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, true, true, "foo", logger,
            Collections.<String>emptySet());

    Traits traits = createTraits("foo")
        .putEmail("friends@segment.com")
        .putPhone("1-844-611-0621")
        .putCreatedAt("15th Feb, 2015")
        .putUsername("segmentio");
    traits.remove("anonymousId");
    JSONObject expected = new JSONObject();
    expected.put("userId", "foo");
    expected.put("$email", traits.email());
    expected.put("$phone", traits.phone());
    expected.put("$first_name", traits.firstName());
    expected.put("$last_name", traits.lastName());
    expected.put("$name", traits.name());
    expected.put("$username", traits.username());
    expected.put("$created", traits.createdAt());

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanel).identify("foo");
    verify(mixpanel).registerSuperProperties(jsonEq(expected));
    verify(mixpanelPeople).identify("foo");
    verify(mixpanelPeople).set(jsonEq(expected));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void event() {
    Properties properties = new Properties().putRevenue(20);
    integration.event("foo", properties);
    verify(mixpanel).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeople() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, true, true, "foo", logger,
            Collections.<String>emptySet());
    Properties properties = new Properties();
    integration.event("foo", properties);
    verify(mixpanel).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeopleAndRevenue() {
    integration =
        new MixpanelIntegration(mixpanel, mixpanelPeople, true, false, true, true, true, "foo", logger,
            Collections.<String>emptySet());
    Properties properties = new Properties().putRevenue(20);
    integration.event("foo", properties);
    verify(mixpanel).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verify(mixpanelPeople).trackCharge(eq(20.0), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void flush() {
    integration.flush();
    verify(mixpanel).flush();
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void reset() {
    integration.reset();
    verify(mixpanel).reset();
    verifyNoMoreMixpanelInteractions();
  }

  private void verifyNoMoreMixpanelInteractions() {
    verifyNoMoreInteractions(MixpanelAPI.class);
    verifyNoMoreInteractions(mixpanel);
    verifyNoMoreInteractions(mixpanelPeople);
  }

  public static JSONObject jsonEq(JSONObject expected) {
    return argThat(new JSONObjectMatcher(expected));
  }

  private static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject> {
    private final JSONObject expected;

    private JSONObjectMatcher(JSONObject expected) {
      this.expected = expected;
    }

    @Override public boolean matchesSafely(JSONObject jsonObject) {
      // todo: this relies on having the same order
      return expected.toString().equals(jsonObject.toString());
    }

    @Override public void describeTo(Description description) {
      description.appendText(expected.toString());
    }
  }
}
