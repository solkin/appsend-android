# AppSend

Extracts the APKs of apps installed on your Android device so they can be kept or passed on.

* Extracts single APK apps as `.apk` and split apps as one `.apks` set, system apps included. No root required.
* Batch export and share, with progress in the background and retry for a failed app.
* Keeps track of exported files: which are installed and which are outdated.
* Shows app details: size with splits, source, SDK levels, signature and permissions with their state.
* Search by name or package, filters for system, split and runnable apps, four sort orders.

Requires Android 7.0 or newer. Built on Material Components 1.14 (Material 3 Expressive).

Modules follow VIPER: `screen/<module>` holds a passive view, a presenter and interactor free of Android types, and a router; `domain` holds entities and ports, `data` their Android implementations.
