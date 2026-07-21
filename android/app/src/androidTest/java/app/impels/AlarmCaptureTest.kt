package app.impels

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.impels.schedule.AlarmActivity
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Captures the full-screen ringing AlarmActivity. It is exported=false (only the
 * app's full-screen intent launches it), so adb `am start` is denied — but
 * instrumentation runs as the app's own uid and can launch it via ActivityScenario.
 * Handshake with the external gRPC capturer mirrors CaptureFlowTest.
 */
@RunWith(AndroidJUnit4::class)
class AlarmCaptureTest {

    @Test
    fun captureAlarm() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val signalDir = ctx.getExternalFilesDir("capture")!!.apply { mkdirs() }
        val intent = AlarmActivity.intent(ctx, 999L, "Review Q3 report", "Boss")

        ActivityScenario.launch<AlarmActivity>(intent).use {
            Thread.sleep(2500) // let it draw + start ringing

            val req = File(signalDir, "alarm-screen.req")
            val ack = File(signalDir, "alarm-screen.ack")
            ack.delete(); req.writeText("go")
            val deadline = System.currentTimeMillis() + 30_000
            while (!ack.exists() && System.currentTimeMillis() < deadline) Thread.sleep(150)
            req.delete(); ack.delete()
        }
    }
}
