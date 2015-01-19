2.1 / 2015-01-19
================

  * Audio codec now defaults to AAC.

2.0 / 2014-12-12
================

  * User Interface:
    * Images updated.
    * Exit button added to home screen.
    * Warning now displayed when the user is not logged in.
    * Settings page completely themed.
    * Review screen now loads videos in groups of 10.
    * Extra details about videos displayed on review screen.
    * Progress timers now shown for activities that visibly take time.
    * About page includes license information.
    * Users are warned when GPS is disabled.
    * Video upload progress is displayed.
  * The app is usable without logging in, videos and spots can be
    recorded and will be uploaded once the user is logged in.
  * Video end times and durations are reported to the server.
  * Services no longer poll but are triggered by actions (optimisation).
  * Azure shared access signatures are used so there is no longer a need
    to distribute the access key with the app (security).