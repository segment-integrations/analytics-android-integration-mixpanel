package com.segment.analytics.android.integrations.mixpanel;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import com.google.common.collect.ImmutableMap;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.segment.analytics.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.GroupPayload;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.AliasPayloadBuilder;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static com.segment.analytics.Utils.createTraits;
import static com.segment.analytics.android.integrations.mixpanel.MixpanelIntegration.filter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(MixpanelAPI.class) public class MixpanelTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock MixpanelAPI mixpanel;
  @Mock Application context;
  @Mock MixpanelAPI.People mixpanelPeople;
  @Mock MixpanelAPI.Group mixpanelGroup;
  @Mock Analytics analytics;
  private MixpanelIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(MixpanelAPI.class);
    Logger logger = Logger.with(Analytics.LogLevel.DEBUG);
    when(MixpanelAPI.getInstance(context, "foo", false)).thenReturn(mixpanel);
    when(mixpanel.getPeople()).thenReturn(mixpanelPeople);
    when(analytics.logger("Mixpanel")).thenReturn(logger);
    when(analytics.getApplication()).thenReturn(context);
    when(mixpanel.getGroup(anyString(), anyString())).thenReturn(mixpanelGroup);

    integration =
        new MixpanelIntegrationBuilder().setMixpanel(mixpanel).createMixpanelIntegration();
  }

  @Test public void factory() {
    ValueMap settings = new ValueMap().putValue("token", "foo")
        .putValue("trackAllPages", true)
        .putValue("trackCategorizedPages", false)
        .putValue("trackNamedPages", true)
        .putValue("setAllTraitsByDefault", true);

    MixpanelIntegration integration =
        (MixpanelIntegration) MixpanelIntegration.FACTORY.create(settings, analytics);

    MixpanelAPI.getInstance(context, "foo", false);
    verify(mixpanel, never()).getPeople();

    assertThat(integration.token).isEqualTo("foo");
    assertThat(integration.trackAllPages).isTrue();
    assertThat(integration.trackCategorizedPages).isFalse();
    assertThat(integration.trackNamedPages).isTrue();
    assertThat(integration.increments).isNotNull().isEmpty();
    assertThat(integration.setAllTraitsByDefault).isTrue();
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

    MixpanelAPI.getInstance(context, "foo", false);
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
    MixpanelAPI.getInstance(activity, "foo", false);
    verifyNoMoreInteractions(mixpanel);
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
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setConsolidatedPageCalls(false)
        .createMixpanelIntegration();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenAllPages() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setConsolidatedPageCalls(false)
        .setTrackAllPages(true)
        .createMixpanelIntegration();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanel).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenConsolidatedPages() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setTrackNamedPages(true)
        .createMixpanelIntegration();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    Properties properties = new Properties();
    properties.put("name", "foo");
    verify(mixpanel).track(eq("Loaded a Screen"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenNamedPages() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setConsolidatedPageCalls(false)
        .setTrackNamedPages(true)
        .createMixpanelIntegration();

    integration.screen(new ScreenPayloadBuilder().name("foo").build());
    verify(mixpanel).track(eq("Viewed foo Screen"), jsonEq(new JSONObject()));
    verifyNoMoreMixpanelInteractions();

    integration.screen(new ScreenPayloadBuilder().category("foo").build());
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void screenCategorizedPages() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setConsolidatedPageCalls(false)
        .setTrackCategorizedPages(true)
        .createMixpanelIntegration();

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
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setConsolidatedPageCalls(false)
        .setTrackAllPages(true)
        .setTrackCategorizedPages(true)
        .setTrackNamedPages(true)
        .setIncrements(Collections.singleton("baz"))
        .createMixpanelIntegration();

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
    assertThat(integration.mixpanelPeople).isNull();
    assertThat(integration.isPeopleEnabled).isFalse();

    integration.identify(new IdentifyPayload.Builder()
        .userId("prateek")
        .traits(new Traits().putAge(25))
        .build());
    verify(mixpanel).identify("prateek");
    verify(mixpanel).registerSuperProperties(jsonEq(new JSONObject(ImmutableMap.of("age", 25))));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithoutUserId() {
    integration.identify(new IdentifyPayload.Builder()
        .anonymousId("anonymousId")
        .traits(new Traits().putAge(25))
        .build());
    verify(mixpanel, never()).identify(anyString());
    verify(mixpanel).registerSuperProperties(jsonEq(new JSONObject(ImmutableMap.of("age", 25))));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithPeople() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .createMixpanelIntegration();

    integration.identify(new IdentifyPayload.Builder()
        .traits(new Traits().putAge(25))
        .userId("prateek")
        .build());

    verify(mixpanel).identify("prateek");
    verify(mixpanel).registerSuperProperties(jsonEq(new JSONObject(ImmutableMap.of("age", 25))));
    verify(mixpanelPeople).identify("prateek");
    verify(mixpanelPeople).set(jsonEq(new JSONObject(ImmutableMap.of("age", 25))));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithSuperProperties() throws JSONException {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .createMixpanelIntegration();

    Traits traits = new Traits()
        .putEmail("friends@segment.com")
        .putPhone("1-844-611-0621")
        .putCreatedAt("15th Feb, 2015")
        .putUsername("segmentio");
    JSONObject expected = new JSONObject();
    expected.put("$email", traits.email());
    expected.put("$phone", traits.phone());
    expected.put("$first_name", traits.firstName());
    expected.put("$last_name", traits.lastName());
    expected.put("$name", traits.name());
    expected.put("$username", traits.username());
    expected.put("$created", traits.createdAt());

    integration.identify(new IdentifyPayload.Builder()
        .userId("prateek")
        .traits(traits)
        .build());
    verify(mixpanel).identify("prateek");
    verify(mixpanel).registerSuperProperties(jsonEq(expected));
    verify(mixpanelPeople).identify("prateek");
    verify(mixpanelPeople).set(jsonEq(expected));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithSuperPropertiesValues() throws JSONException {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setSuperProperties(Collections.singleton("parasite"))
        .setSetAllTraitsByDefault(false)
        .createMixpanelIntegration();
    Traits traits = createTraits("foo").putEmail("Raptor@segment.com")
        .putValue("parasite", "Photography Raptor");
    JSONObject expected = new JSONObject();
    expected.put("parasite", "Photography Raptor");

    integration.identify(new IdentifyPayloadBuilder(

    ).traits(traits).build());
    verify(mixpanel).identify("foo");
    verify(mixpanel).registerSuperProperties(jsonEq(expected));
    verify(mixpanelPeople).identify("foo");
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void identifyWithPeopleProperties() throws JSONException {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .setPeopleProperties(Collections.singleton("parasite"))
        .setSetAllTraitsByDefault(false)
        .createMixpanelIntegration();

    Traits traits = createTraits("foo").putEmail("Pencilvester@segment.com")
        .putValue("parasite", "Pencilvester");
    JSONObject expected = new JSONObject();
    expected.put("parasite", "Pencilvester");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verify(mixpanel).identify("foo");
    verify(mixpanelPeople).set(jsonEq(expected));
    verify(mixpanelPeople).identify("foo");
    verifyNoMoreMixpanelInteractions();
  }


  @Test public void group(){
    Traits traits = createTraits();
    integration.group(new GroupPayload.Builder()
            .userId("foo")
            .groupId("testGroupId")
            .traits(traits)
            .build());
    // check mixpanel getGroup called with groupKey default "[Segment] Group" and groupID "testGroupId"
    verify(mixpanel).getGroup("[Segment] Group", "testGroupId");

    // verify to see that the same Traits passed in the integration
    // transformed to a JsonObject are setOnce on the Group object
    verify(mixpanelGroup).setOnce(refEq(traits.toJsonObject()));

    // groupKey as default, since no name is set
    // groupdId as integration
    verify(mixpanel).setGroup("[Segment] Group","testGroupId");

    // verify that no more interactions are being made
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void groupWithGroupName(){
    Traits traits = createTraits().putName("someGroup");
    integration.group(new GroupPayload.Builder()
            .userId("foo")
            .groupId("testGroupId")
            .traits(traits)
            .build());
    // check mixpanel getGroup called with groupKey "someGroup" and groupID "testGroupId"
    verify(mixpanel).getGroup("someGroup", "testGroupId");

    // verify to see that the same Traits passed in the integration
    // transformed to a JsonObject are setOnce on the Group object
    verify(mixpanelGroup).setOnce(refEq(traits.toJsonObject()));

    // check groupKey as integration
    // check groupId as integration
    verify(mixpanel).setGroup("someGroup","testGroupId");

    // verify that no more interactions are being made
    verifyNoMoreMixpanelInteractions();
  }


  @Test public void testFilter() {
    Map<String, String> map = Collections.singletonMap("foo", "bar");
    assertThat(filter(map, Collections.<String>emptySet())).isEqualTo(Collections.emptyMap());
    assertThat(filter(map, Collections.singletonList("bar"))).isEqualTo(Collections.emptyMap());
    assertThat(filter(map, Collections.singletonList("foo"))).isEqualTo(
        Collections.singletonMap("foo", "bar"));
  }

  @Test public void event() {
    Properties properties = new Properties().putRevenue(20);
    integration.event("foo", properties);
    verify(mixpanel).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeople() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .createMixpanelIntegration();
    Properties properties = new Properties();
    integration.event("foo", properties);
    verify(mixpanel).track(eq("foo"), jsonEq(properties.toJsonObject()));
    verifyNoMoreMixpanelInteractions();
  }

  @Test public void eventWithPeopleAndRevenue() {
    integration = new MixpanelIntegrationBuilder().setMixpanel(mixpanel)
        .setMixpanelPeople(mixpanelPeople)
        .setIsPeopleEnabled(true)
        .createMixpanelIntegration();
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

  private JSONObject jsonEq(JSONObject expected) {
    return argThat(new JSONMatcher(expected));
  }

  class JSONMatcher implements ArgumentMatcher<JSONObject> {
    JSONObject expected;

    JSONMatcher(JSONObject expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(JSONObject argument) {
      try {
        JSONAssert.assertEquals(expected, argument, JSONCompareMode.STRICT);
        return true;
      } catch (JSONException e) {
        return false;
      }
    }
  }
}
