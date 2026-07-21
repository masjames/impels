package app.impels

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Drives impels through each screen state via testTags and, at every state,
 * hands off to an external gRPC screenshot capturer (run-instrumented-capture.sh).
 *
 * Running in-process makes navigation deterministic: no adb `input` taps, no
 * splash-timing races, no soft keyboard, no uiautomator — the exact problems
 * that defeated the adb-driven script on this emulator (see HANDOFF.md).
 *
 * Handshake: for state <name> the test writes <name>.req into the app's external
 * files dir and blocks until the capturer creates <name>.ack.
 */
@RunWith(AndroidJUnit4::class)
class CaptureFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val signalDir: File by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir("capture")!!.apply { mkdirs() }
    }

    /** Wait for Compose to settle, then let the external capturer take the shot. */
    private fun capture(name: String) {
        compose.waitForIdle()
        val req = File(signalDir, "$name.req")
        val ack = File(signalDir, "$name.ack")
        ack.delete()
        req.writeText("go")
        val deadline = System.currentTimeMillis() + 30_000
        while (!ack.exists() && System.currentTimeMillis() < deadline) {
            Thread.sleep(150)
        }
        req.delete(); ack.delete()
    }

    private fun openForm() = compose.onNodeWithTag("fab_add").performClick().also { compose.waitForIdle() }
    private fun back() = compose.onNodeWithContentDescription("Back").performClick().also { compose.waitForIdle() }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureAllStates() {
        // 1 — empty home (DB was cleared before the run)
        capture("home-empty")

        // 2 — blank add form
        openForm()
        capture("add-form")

        // 3 — filled add form (unsaved), then discard
        compose.onNodeWithTag("field_what").performTextInput("Review Q3 report")
        compose.onNodeWithTag("chip_who_Boss").performClick()
        compose.onNodeWithTag("chip_int_30").performClick()
        compose.waitForIdle()
        capture("add-form-filled")
        back()

        // 4 — focused card (Save & focus)
        openForm()
        compose.onNodeWithTag("field_what").performTextInput("Review Q3 report")
        compose.onNodeWithTag("chip_who_Boss").performClick()
        compose.onNodeWithTag("chip_int_10").performClick()
        compose.onNodeWithTag("btn_save_focus").performClick()
        compose.waitForIdle()
        capture("home-focused")

        // 5 — queue (a second reminder via Catch it → sits in "Up next")
        openForm()
        compose.onNodeWithTag("field_what").performTextInput("Order team lunch")
        compose.onNodeWithTag("chip_who_Coworker").performClick()
        compose.onNodeWithTag("chip_int_30").performClick()
        compose.onNodeWithTag("btn_save").performClick()
        compose.waitForIdle()
        capture("home-queue")

        // 6 — done (mark the focused card done → "Done (1)")
        compose.onNodeWithTag("btn_done").performClick()
        compose.waitForIdle()
        capture("home-done")

        // 7 — settings
        compose.onNodeWithTag("btn_settings").performClick()
        compose.waitForIdle()
        capture("settings")
        back()
    }

    /** Slow narrated walkthrough (~40s) for the demo video; the external
     *  capturer runs a gRPC frame recorder over its duration. No handshake. */
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun demoWalkthrough() {
        fun beat(ms: Long = 1200) { compose.waitForIdle(); Thread.sleep(ms) }

        beat(2500)
        openForm(); beat()
        compose.onNodeWithTag("field_what").performTextInput("Review Q3 report"); beat()
        compose.onNodeWithTag("chip_who_Boss").performClick(); beat()
        compose.onNodeWithTag("chip_int_10").performClick(); beat()
        compose.onNodeWithTag("btn_save_focus").performClick(); beat(2500)

        openForm(); beat()
        compose.onNodeWithTag("field_what").performTextInput("Order team lunch"); beat()
        compose.onNodeWithTag("chip_who_Coworker").performClick(); beat()
        compose.onNodeWithTag("chip_int_30").performClick(); beat()
        compose.onNodeWithTag("btn_save").performClick(); beat(2500)

        compose.onNodeWithTag("btn_done").performClick(); beat(2500)

        compose.onNodeWithTag("btn_settings").performClick(); beat(3000)
        back(); beat(2000)

        openForm(); beat()
        compose.onNodeWithTag("field_what").performTextInput("Email the vendor"); beat()
        compose.onNodeWithTag("chip_who_Me").performClick(); beat()
        compose.onNodeWithTag("chip_int_15").performClick(); beat(2000)
        back(); beat(2000)
    }
}
