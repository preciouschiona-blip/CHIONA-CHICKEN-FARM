# CHIONA CHICKEN FARM

Android poultry business app: flocks, daily records, incubator (day 1 to hatch), vaccine plans, money in MWK, forecast, profit split, and a shared farm record for the owner and helpers.

The screens live in `app/src/main/assets/index.html` and run inside a small Android WebView shell.

## Sharing between phones
Create a free Firebase Realtime Database, set its rules to

```json
{"rules":{"farms":{"$farm":{".read":true,".write":true}}}}
```

then paste its address in the app under More, Team and sync, and share the invite code with helpers.

## Build
Pushes to `main` build a debug APK with GitHub Actions (artifact `ChionaChickenFarm-apk`). The Release workflow publishes `ChionaChickenFarm.apk` on the Releases page.
