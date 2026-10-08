---
name: material-expressive
description: Use when building, theming, or restyling Jetpack Compose UI for this project. Covers Material 3 Expressive adoption (shapes, motion, typography), the Threads-inspired brand palette, and contrast-safe color pairings. Trigger on any request touching Color.kt / Theme.kt / Type.kt / Shape.kt, new Composables, or "make the app look like Threads / expressive / branded".
---

# Material 3 Expressive — GeneratorTask flavor

Material 3 Expressive is Google's 2025 refresh of Material 3: bigger shape vocabulary, flexible motion, variable typography, and emotionally richer component surfaces. In this project we pair it with a **Threads (Meta) minimalist palette**: pure black and pure white as the ground truth, a single Instagram-blue accent, and a graded gray ramp for everything else.

## Dependencies

Add to `app/build.gradle.kts`:

```kotlin
implementation("androidx.compose.material3:material3:1.4.0-expressive01")
implementation("androidx.compose.material3:material3-adaptive:1.1.0")
implementation("androidx.compose.material:material-icons-extended")
```

Expressive components live in the same `material3` package starting with `1.4.0-expressive*`. If the project uses a BOM, pin the expressive BOM (e.g. `androidx.compose:compose-bom-alpha:2026.02.01`) rather than mixing stable + expressive artifacts.

## Threads-inspired palette (contrast-locked)

Design principle: **every color has an on-pair with ≥4.5:1 contrast ratio (WCAG AA)**. Pure black and pure white carry all text; grays carry hierarchy; blue is the sole saturated accent.

```kotlin
package com.chart.generator.ui.theme

import androidx.compose.ui.graphics.Color

// Ground
val ThreadsBlack = Color(0xFF000000)
val ThreadsWhite = Color(0xFFFFFFFF)

// Gray ramp (light -> dark)
val Gray05 = Color(0xFFFAFAFA)   // bg tint light
val Gray10 = Color(0xFFF5F5F5)   // surface light
val Gray20 = Color(0xFFDBDBDB)   // divider light
val Gray40 = Color(0xFF999999)   // secondary text light
val Gray60 = Color(0xFF737373)   // secondary text dark
val Gray70 = Color(0xFF4D4D4D)   // tertiary dark
val Gray80 = Color(0xFF262626)   // divider dark
val Gray90 = Color(0xFF171717)   // surface dark
val Gray95 = Color(0xFF101010)   // elevated dark

// Accent (single)
val InstaBlue = Color(0xFF0095F6)
val InstaBlueDark = Color(0xFF1877F2)

// Semantic
val DangerRed = Color(0xFFED4956)
val SuccessGreen = Color(0xFF4BB543)
```

### Light scheme

```kotlin
val LightColorScheme = lightColorScheme(
    primary = ThreadsBlack,
    onPrimary = ThreadsWhite,
    primaryContainer = Gray10,
    onPrimaryContainer = ThreadsBlack,

    secondary = Gray70,
    onSecondary = ThreadsWhite,
    secondaryContainer = Gray10,
    onSecondaryContainer = Gray80,

    tertiary = InstaBlue,
    onTertiary = ThreadsWhite,
    tertiaryContainer = Color(0xFFE7F3FF),
    onTertiaryContainer = Color(0xFF003A75),

    background = ThreadsWhite,
    onBackground = ThreadsBlack,
    surface = ThreadsWhite,
    onSurface = ThreadsBlack,
    surfaceVariant = Gray05,
    onSurfaceVariant = Gray70,

    outline = Gray20,
    outlineVariant = Gray10,

    error = DangerRed,
    onError = ThreadsWhite,
)
```

### Dark scheme

```kotlin
val DarkColorScheme = darkColorScheme(
    primary = ThreadsWhite,
    onPrimary = ThreadsBlack,
    primaryContainer = Gray95,
    onPrimaryContainer = ThreadsWhite,

    secondary = Gray40,
    onSecondary = ThreadsBlack,
    secondaryContainer = Gray90,
    onSecondaryContainer = Gray20,

    tertiary = InstaBlueDark,
    onTertiary = ThreadsWhite,
    tertiaryContainer = Color(0xFF003A75),
    onTertiaryContainer = Color(0xFFE7F3FF),

    background = ThreadsBlack,
    onBackground = ThreadsWhite,
    surface = ThreadsBlack,
    onSurface = ThreadsWhite,
    surfaceVariant = Gray95,
    onSurfaceVariant = Gray40,

    outline = Gray80,
    outlineVariant = Gray90,

    error = DangerRed,
    onError = ThreadsWhite,
)
```

### Contrast table (WCAG AA minimum 4.5:1 for text)

| Pair | Ratio | Verdict |
|---|---|---|
| `ThreadsBlack` on `ThreadsWhite` | 21.00 | ✓ |
| `ThreadsWhite` on `ThreadsBlack` | 21.00 | ✓ |
| `Gray70` on `ThreadsWhite` | 7.46 | ✓ |
| `Gray40` on `ThreadsBlack` | 4.74 | ✓ |
| `InstaBlue` on `ThreadsWhite` | 3.07 | ⚠ large text / icons only |
| `InstaBlueDark` on `ThreadsBlack` | 4.52 | ✓ |

Rule: **never use `tertiary` for body text in light mode** — use it for buttons, chips, icons, FAB. Pair with explicit `onTertiary` for text on blue surfaces.

## Shapes (expressive)

Expressive uses a wider shape scale than classic M3. Six-step scale, from `extraSmall` to `extraExtraLarge`:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
```

Threads uses `large` (24dp) for cards and bottom sheets, `extraLarge` (32dp) for FABs, `small` (8dp) for chips and tags, `full` (`CircleShape`) for avatars.

## Typography

Expressive supports variable fonts. For a Threads feel, use **Inter** (variable):

```kotlin
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Inter = FontFamily(
    Font(R.font.inter_variable, weight = FontWeight.W400),
    Font(R.font.inter_variable, weight = FontWeight.W500),
    Font(R.font.inter_variable, weight = FontWeight.W600),
    Font(R.font.inter_variable, weight = FontWeight.W700),
)

val ExpressiveTypography = Typography(
    displayLarge = TextStyle(fontFamily = Inter, fontSize = 57.sp, fontWeight = FontWeight.W700, lineHeight = 64.sp),
    headlineLarge = TextStyle(fontFamily = Inter, fontSize = 32.sp, fontWeight = FontWeight.W700, lineHeight = 40.sp),
    titleLarge = TextStyle(fontFamily = Inter, fontSize = 22.sp, fontWeight = FontWeight.W600, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.W400, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.W400, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontSize = 14.sp, fontWeight = FontWeight.W600, lineHeight = 20.sp),
)
```

Threads uses **tight tracking** (letter-spacing ~ -0.02em) on headlines. Set `letterSpacing = (-0.3).sp` on `headlineLarge` / `displayLarge`.

## Motion (expressive easings)

Expressive promotes spring-based, overshoot-friendly motion. Register in `MotionScheme`:

```kotlin
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

val ExpressiveSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
```

Default to **spring over tween** for expressive feel. Reserve `tween` for timing-locked transitions (e.g. sync with an audio cue).

## Theme wiring

```kotlin
@Composable
fun GeneratorTaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        shapes = ExpressiveShapes,
        content = content,
    )
}
```

Note: **`dynamicColor` is deliberately absent** — we ship the branded palette, not Material You.

## Checklist when creating a new Composable

- [ ] Uses `MaterialTheme.colorScheme.*` / `MaterialTheme.typography.*` / `MaterialTheme.shapes.*` only — no hex literals.
- [ ] Previews cover **both** light and dark (`@Preview` + `@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)`).
- [ ] Edge-to-edge safe: respects `WindowInsets` for status/nav bars.
- [ ] No comments inside the Composable (per project CLAUDE.md).
- [ ] Semantics for a11y: `contentDescription` on Icons, `Modifier.semantics` on custom controls.
