# TopadroiSDK for Android

A lightweight event-collection SDK for Android. It buffers events on-device (offline queue with
exponential-backoff retry), batches them, and forwards them over HTTPS to your **topadroi** server
endpoint for downstream processing. The SDK is a thin client — all routing, transformation, and delivery logic lives
server-side.

- **Minimum SDK**: 21 · **compileSdk**: 34 · **JDK**: 17 · **Language**: Kotlin
- Server endpoint (default): `https://capi-worker.topadroi.com/v1/mobile/events`

## Installation

**Maven coordinate** (publication to Maven Central is pending — see the project's `PUBLISHING.md`):

```kotlin
dependencies {
    implementation("io.topadroi:sdk:0.1.0")
}
```

**Interim (JitPack — builds directly from this repo):**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { maven("https://jitpack.io") }
}
// build.gradle.kts
dependencies {
    implementation("com.github.ttkcben:topadroi-Android:0.1.0")
}
```

The library also requires the `INTERNET` permission (already declared in the SDK manifest and
merged automatically).

## Quick start

```kotlin
import com.topadroi.sdk.Topadroi

// 1) Initialize once (e.g. in Application.onCreate)
Topadroi.initialize(context = this, apiKey = "YOUR_SITE_API_KEY", site = "YOUR_SITE_ID")

// 2) Set the consent state you collected from the user.
Topadroi.setConsent(Consent(adTracking = false, analytics = true))

// 3) (Optional) Identify the user — only pass PII you are lawfully allowed to.
Topadroi.identify(Traits(email = "user@example.com"))

// 4) Track events.
Topadroi.track("purchase", mapOf("value" to 49.99, "currency" to "USD"))

// 5) Flush is automatic (batch threshold); you can also force it.
Topadroi.flush()
```

The API key is supplied by **you** at runtime — nothing secret is embedded in this SDK.

## Privacy & Compliance — your responsibilities

> **Read this before shipping.** This SDK is a tool. When you embed it, **you are the data
> controller** and you are solely responsible for lawful, compliant use. topadroi processes the
> data you send strictly as a **processor on your behalf**, under your topadroi service agreement
> / Data Processing Addendum.

You must:

1. **Obtain valid user consent** before collecting or transmitting personal data, as required by
   the laws applicable to your users (e.g. GDPR / UK GDPR, ePrivacy, CCPA/CPRA, LGPD).
2. **Respect advertising-ID choices.** The Google Advertising ID (GAID) is attached **only** when
   `adTracking` consent is set; honor "Limit ad tracking" / deletion of the advertising ID and
   make the disclosures required by Google Play's policies.
3. **Maintain a privacy policy** that discloses what you collect, that data is processed by a
   third-party processor (topadroi) and onward services, and how users exercise their rights.
4. **Only send data you are permitted to send.** Do not pass email, phone, or other identifiers
   without a lawful basis and consent.
5. **Complete your Google Play Data Safety form** to accurately reflect the data your app collects
   and shares via this SDK.
6. **Do not transmit Restricted Data.** Do not send children's data (COPPA / under-16 without
   verifiable parental consent) or special-category/sensitive data (health, biometrics, precise
   geolocation without consent, government IDs, etc.). See the Acceptable Use Policy below.

## Terms of Service & data processing

**By integrating this SDK and transmitting data to the topadroi Service, you agree to the topadroi
Terms of Service.** Use of the Service is governed by:

- **Terms of Service** — https://www.topadroi.com/legal/terms
- **Acceptable Use Policy** (Restricted Data & prohibited uses) — https://www.topadroi.com/legal/acceptable-use
- **Data Processing Addendum** (you = controller, topadroi = processor) — https://www.topadroi.com/legal/dpa
- **Privacy Policy** — https://www.topadroi.com/legal/privacy

Among other things, you represent that you have obtained all required end-user consents, that you
will not transmit Restricted Data, and that you will indemnify topadroi for claims arising from your
data or your breach of these terms. The Apache-2.0 license below covers the SDK **source code**; it
does not replace the Terms that govern use of the **Service**.

## Disclaimer of warranty & limitation of liability

This software is provided **"AS IS", without warranty of any kind**, and the authors and copyright
holders shall not be liable for any claim, damages, or other liability arising from its use, to the
maximum extent permitted by law. See **Sections 7 and 8 of the Apache License, Version 2.0** in
[LICENSE](./LICENSE).

## Trademarks

Android, Google Play, and Google Advertising ID are trademarks of Google LLC. All other product and
platform names are trademarks of their respective owners. References are descriptive only and do not
imply any endorsement of, or affiliation with, this SDK or topadroi.

## License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](./LICENSE) and [NOTICE](./NOTICE).
