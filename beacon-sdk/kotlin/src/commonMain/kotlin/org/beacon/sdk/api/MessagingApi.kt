package org.beacon.sdk.api

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import org.beacon.sdk.model.*

interface MessagingApi {
    suspend fun sendMessage(
        recipientId: PeerId?,
        payload: MessagePayload,
        priority: MessagePriority = MessagePriority.NORMAL,
        ttl: Int = 5
    ): Result<MessageId>

    suspend fun sendSos(location: Location, customMessage: String? = null): Result<MessageId>

    suspend fun sendLocation(recipientId: PeerId?, location: Location): Result<MessageId>

    suspend fun sendResourceReport(report: ResourceReport): Result<MessageId>

    suspend fun sendAlert(alert: Alert): Result<MessageId>

    fun observeIncomingMessages(): Flow<Message>

    fun observeMessageStatus(messageId: MessageId): Flow<MessageStatus>

    suspend fun getMessageHistory(
        peerId: PeerId? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Message>>

    suspend fun acknowledgeMessage(messageId: MessageId): Result<Unit>

    suspend fun retryFailedMessage(messageId: MessageId): Result<Unit>

    suspend fun deleteMessage(messageId: MessageId): Result<Unit>

    suspend fun clearHistory(olderThan: Instant? = null): Result<Int>
}

interface PeerApi {
    fun observePeers(): Flow<List<Peer>>

    fun observePeerEvents(): Flow<PeerDiscoveryEvent>

    suspend fun getPeer(peerId: PeerId): Result<Peer>

    suspend fun getLocalPeer(): Result<Peer>

    suspend fun updateLocalPeer(displayName: String? = null): Result<Unit>

    suspend fun trustPeer(peerId: PeerId, trust: Boolean): Result<Unit>

    suspend fun blockPeer(peerId: PeerId): Result<Unit>

    suspend fun unblockPeer(peerId: PeerId): Result<Unit>

    suspend fun getTrustedPeers(): Result<List<Peer>>

    suspend fun getBlockedPeers(): Result<List<PeerId>>

    fun observePeer(peerId: PeerId): Flow<Peer?>
}