Version 1.0.8 (2nd February, 2017)
===================================
*(Supports Mixpanel 4.9.8)*

  * Fix: Don't mutate properties object in payload.

Version 1.0.7 (2nd February, 2017)
===================================
*(Supports Mixpanel 4.9.2)*

  * [Bug](https://github.com/segment-integrations/analytics-android-integration-mixpanel/pull/14): While `setAllTraitsByDefault` is false, this pulls in `peopleProperties` and/or `superProperties` when configured in the integration panel.
  * Refactor tests.


Version 1.0.6 (5th December, 2016)
===================================
*(Supports Mixpanel 4.9.2)*

  * [Improvement](https://github.com/segment-integrations/analytics-android-integration-mixpanel/pull/12): Explicitly send distinct ID as previous ID in alias call.

Version 1.0.5 (5th December, 2016)
===================================
*(Supports Mixpanel 4.9.2)*

  * Bumps Mixpanel version to 4.9.2
  * Mixpanel version 4.9.2 fixes and updates [here](https://github.com/mixpanel/mixpanel-android/releases/tag/v4.9.2)


Version 1.0.4 (6th July, 2016)
===================================
*(Supports Mixpanel 4.9.0)*

  * Mixpanel version 4.9.0 fixes:
  * Activity life cycle callbacks in old Android APIs.
  * OutOfMemoryError for in-app and ab test.
  * NullPointerException when accessing people profiles after resetting a Mixpanel instance.

Version 1.0.3 (12th May, 2016)
===================================
*(Supports analytics-android 4.0.+ and Mixpanel 4.7.+)*

  * Add support for Segment's consolidatedPageCalls setting to send all Screen calls to Mixpanel as `Loaded a Screen`

Version 1.0.2 (12th May, 2016)
===================================
*(Supports analytics-android 4.0.+ and Mixpanel 4.7.+)*

  * Do not call Mixpanel's identify if no userId has been set.

Version 1.0.1 (2nd May, 2016)
===================================
*(Supports analytics-android 4.0.+ and Mixpanel 4.7.+)*

  * Fix mapping of firstName and name.

Version 1.0.0 (27th November, 2015)
===================================
*(Supports analytics-android 4.0.+ and Mixpanel 4.7.+)*

  * Initial Release.
