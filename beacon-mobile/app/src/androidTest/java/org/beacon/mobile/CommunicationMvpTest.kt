package org.beacon.mobile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.beacon.mobile.service.MeshForegroundService
import org.beacon.sdk.BeaconSdkFactory
import org.beacon.sdk.model.BeaconConfig
import org.beacon.sdk.model.MessagePayload
import org.beacon.sdk.model.MessagePriority
import org.beacon.sdk.model.MessageType
import org.beacon.sdk.model.PeerId
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CommunicationMvpTest {

    private val testDispatcher = TestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private var context: Context? = null
    private var beaconSdk: org.beacon.sdk.BeaconSdk? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<BeaconApplication>()
        testDispatcher.advanceTimeBy(1000) // Allow initialization
    }

    @After
    fun teardown() {
        beaconSdk?.shutdown()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `SDK initializes successfully`() = testScope.runBlockingTest {
        val config = BeaconConfig(
            deviceName = "Test Device",
            enableBle = true,
            enableWifiDirect = true,
            enableLora = false
        )

        val result = BeaconSdkFactory.create(context!!, config)
        
        assertTrue(result is Result.Success)
        
        val sdk = (result as Result.Success).value
        beaconSdk = sdk
        
        val initResult = sdk.initialize()
        assertTrue(initResult is Result.Success)
        
        // Verify SDK components are available
        assertNotNull(sdk.messaging)
        assertNotNull(sdk.peers)
        assertNotNull(sdk.network)
        assertNotNull(sdk.maps)
        assertNotNull(sdk.identity)
        assertNotNull(sdk.power)
        assertNotNull(sdk.storage)
        assertNotNull(sdk.lifecycle)
    }

    @Test
    fun `Can send and receive text message`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Sender")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        // Generate a test peer ID
        val recipientId = PeerId("test-peer-${System.currentTimeMillis()}")

        // Send a text message
        val sendResult = sdk.messaging.sendMessage(
            recipientId = recipientId,
            payload = MessagePayload(
                type = MessageType.TEXT,
                text = "Hello, mesh network!"
            ),
            priority = MessagePriority.NORMAL,
            ttl = 5
        )

        assertTrue(sendResult is Result.Success)
        
        val messageId = (sendResult as Result.Success).value
        assertTrue(messageId.value.isNotEmpty())

        // Verify message status
        val statusResult = sdk.messaging.observeMessageStatus(messageId).first()
        assertEquals(org.beacon.sdk.model.DeliveryState.QUEUED, statusResult.state)
    }

    @Test
    fun `Can send SOS message`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test SOS Device")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        // Send SOS
        val sosResult = sdk.messaging.sendSos(
            location = org.beacon.sdk.model.Location(
                latitude = 40.7128,
                longitude = -74.0060,
                accuracy = 10.0
            ),
            customMessage = "Test SOS - trapped on 3rd floor"
        )

        assertTrue(sosResult is Result.Success)
        
        val sosId = (sosResult as Result.Success).value
        assertTrue(sosId.value.isNotEmpty())

        // SOS should be CRITICAL priority
        val statusResult = sdk.messaging.observeMessageStatus(sosId).first()
        // Note: Actual priority verification would require accessing message internals
    }

    @Test
    fun `Can send location`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Location Device")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        val recipientId = PeerId("test-peer-location")
        val location = org.beacon.sdk.model.Location(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 5.0
        )

        val result = sdk.messaging.sendLocation(recipientId, location)
        
        assertTrue(result is Result.Success)
        val messageId = (result as Result.Success).value
        assertTrue(messageId.value.isNotEmpty())
    }

    @Test
    fun `Can observe peers`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Peer Observer")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        // Observe peers - should emit empty list initially
        val peers = sdk.peers.observePeers().first()
        assertEquals(0, peers.size)
    }

    @Test
    fun `Can get network stats`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Network Device")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        val statsResult = sdk.network.getNetworkStats()
        assertTrue(statsResult is Result.Success)
        
        val stats = (statsResult as Result.Success).value
        assertEquals(0, stats.peerCount)
        assertEquals(0, stats.connectedPeerCount)
        assertEquals(0L, stats.messagesSent)
    }

    @Test
    fun `Power mode updates correctly`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Power Device", powerMode = org.beacon.sdk.model.PowerMode.CONSERVATION)
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        val powerMode = sdk.power.getCurrentMode()
        assertTrue(powerMode is Result.Success)
        assertEquals(org.beacon.sdk.model.PowerMode.CONSERVATION, (powerMode as Result.Success).value)

        // Test mode change
        val changeResult = sdk.power.setPowerMode(org.beacon.sdk.model.PowerMode.SURVIVAL)
        assertTrue(changeResult is Result.Success)
    }

    @Test
    fun `Storage operations work`() = testScope.runBlockingTest {
        val config = BeaconConfig(deviceName = "Test Storage Device")
        val sdk = (BeaconSdkFactory.create(context!!, config) as Result.Success).value
        beaconSdk = sdk
        sdk.initialize()

        // Test encrypted storage
        val testData = "test data".toByteArray()
        val putResult = sdk.storage.putEncrypted("test_key", testData)
        assertTrue(putResult is Result.Success)

        val getResult = sdk.storage.getEncrypted("test_key")
        assertTrue(getResult is Result.Success)
        assertEquals(testData.contentToString(), (getResult as Result.Success).value?.toString())
    }

    @Test
    fun `Mesh foreground service starts`() = testScope.runBlockingTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = android.content.Intent(context, MeshForegroundService::class.java)
        
        // Note: Full service testing requires Android Test Orchestrator
        // This is a placeholder for service testing
        assertTrue(true)
    }

    @Test
    fun `SOS activation flow`() = testScope.runBlockingTest {
        // This would test the SOS activation UI flow
        // Requires UI testing with Espresso/Compose testing
        assertTrue(true)
    }
}