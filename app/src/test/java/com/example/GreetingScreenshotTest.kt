package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        composeTestRule.setContent { 
            MyApplicationTheme { 
                ConfigurationParserComponent(
                    isFa = false,
                    rawLinkInput = "vless://e1b54f@50.116.50.31:443?sni=google.com#US-Real",
                    onLinkChanged = {},
                    validationState = LinkValidationState.Valid(
                        name = "US-Real",
                        host = "50.116.50.31",
                        port = 443,
                        protocol = "VLESS (Reality)",
                        secret = "e1b54f2a-88b4-92ca",
                        sni = "google.com",
                        displayProtocol = "VLESS"
                    )
                )
            } 
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
