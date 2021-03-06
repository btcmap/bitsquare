package io.bitsquare.p2p.peers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import io.bitsquare.app.Log;
import io.bitsquare.common.UserThread;
import io.bitsquare.p2p.Message;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.network.Connection;
import io.bitsquare.p2p.network.MessageListener;
import io.bitsquare.p2p.network.NetworkNode;
import io.bitsquare.p2p.peers.messages.data.DataRequest;
import io.bitsquare.p2p.peers.messages.data.DataResponse;
import io.bitsquare.p2p.peers.messages.data.PreliminaryDataRequest;
import io.bitsquare.p2p.peers.messages.data.UpdateDataRequest;
import io.bitsquare.p2p.storage.P2PDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

public class RequestDataHandshake implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RequestDataHandshake.class);

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Listener
    ///////////////////////////////////////////////////////////////////////////////////////////

    public interface Listener {
        void onComplete();

        void onFault(String errorMessage);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Class fields
    ///////////////////////////////////////////////////////////////////////////////////////////

    private final NetworkNode networkNode;
    private final P2PDataStorage dataStorage;
    private final PeerManager peerManager;
    private final Listener listener;
    private Timer timeoutTimer;
    private final long nonce = new Random().nextLong();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    public RequestDataHandshake(NetworkNode networkNode, P2PDataStorage dataStorage, PeerManager peerManager,
                                Listener listener) {
        this.networkNode = networkNode;
        this.dataStorage = dataStorage;
        this.peerManager = peerManager;
        this.listener = listener;

        networkNode.addMessageListener(this);
    }

    public void shutDown() {
        Log.traceCall();
        networkNode.removeMessageListener(this);
        stopTimeoutTimer();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void requestData(NodeAddress nodeAddress) {
        Log.traceCall("nodeAddress=" + nodeAddress);
        checkArgument(timeoutTimer == null, "requestData must not be called twice.");

        timeoutTimer = UserThread.runAfter(() -> {
                    log.info("timeoutTimer called");
                    peerManager.shutDownConnection(nodeAddress);
                    shutDown();
                    listener.onFault("A timeout occurred");
                },
                10, TimeUnit.SECONDS);

        Message dataRequest;
        if (networkNode.getNodeAddress() == null)
            dataRequest = new PreliminaryDataRequest(nonce);
        else
            dataRequest = new UpdateDataRequest(networkNode.getNodeAddress(), nonce);

        log.info("We send a {} to peer {}. ", dataRequest.getClass().getSimpleName(), nodeAddress);

        SettableFuture<Connection> future = networkNode.sendMessage(nodeAddress, dataRequest);
        Futures.addCallback(future, new FutureCallback<Connection>() {
            @Override
            public void onSuccess(@Nullable Connection connection) {
                log.trace("Send " + dataRequest + " to " + nodeAddress + " succeeded.");
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                String errorMessage = "Sending dataRequest to " + nodeAddress +
                        " failed. That is expected if the peer is offline. dataRequest=" + dataRequest + "." +
                        "Exception: " + throwable.getMessage();
                log.info(errorMessage);

                peerManager.shutDownConnection(nodeAddress);
                shutDown();
                listener.onFault(errorMessage);
            }
        });
    }

    public void onDataRequest(Message message, final Connection connection) {
        Log.traceCall(message.toString() + " / connection=" + connection);

        checkArgument(timeoutTimer == null, "requestData must not be called twice.");
        timeoutTimer = UserThread.runAfter(() -> {
                    log.info("timeoutTimer called");
                    peerManager.shutDownConnection(connection);
                    shutDown();
                    listener.onFault("A timeout occurred");
                },
                10, TimeUnit.SECONDS);

        DataRequest dataRequest = (DataRequest) message;
        DataResponse dataResponse = new DataResponse(new HashSet<>(dataStorage.getMap().values()), dataRequest.getNonce());
        SettableFuture<Connection> future = networkNode.sendMessage(connection, dataResponse);
        Futures.addCallback(future, new FutureCallback<Connection>() {
            @Override
            public void onSuccess(Connection connection) {
                log.trace("Send DataResponse to {} succeeded. dataResponse={}",
                        connection.getPeersNodeAddressOptional(), dataResponse);
                shutDown();
                listener.onComplete();
            }

            @Override
            public void onFailure(@NotNull Throwable throwable) {
                String errorMessage = "Sending dataRequest to " + connection +
                        " failed. That is expected if the peer is offline. dataRequest=" + dataRequest + "." +
                        "Exception: " + throwable.getMessage();
                log.info(errorMessage);

                peerManager.shutDownConnection(connection);
                shutDown();
                listener.onFault(errorMessage);
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // MessageListener implementation
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onMessage(Message message, Connection connection) {
        if (message instanceof DataResponse) {
            Log.traceCall(message.toString() + " / connection=" + connection);
            DataResponse dataResponse = (DataResponse) message;
            if (dataResponse.requestNonce == nonce) {
                stopTimeoutTimer();

                // connection.getPeersNodeAddressOptional() is not present at the first call
                log.debug("connection.getPeersNodeAddressOptional() " + connection.getPeersNodeAddressOptional());
                connection.getPeersNodeAddressOptional().ifPresent(peersNodeAddress -> {
                    ((DataResponse) message).dataSet.stream()
                            .forEach(e -> dataStorage.add(e, peersNodeAddress));
                });

                shutDown();
                listener.onComplete();
            } else {
                log.debug("Nonce not matching. That happens if we get a response after a canceled handshake " +
                                "(timeout). We drop that message. nonce={} / requestNonce={}",
                        nonce, dataResponse.requestNonce);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void stopTimeoutTimer() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }
}
