/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.trade.protocol.trade.messages;

import io.bitsquare.app.Version;
import io.bitsquare.p2p.NodeAddress;
import io.bitsquare.p2p.messaging.MailboxMessage;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;

@Immutable
public final class DepositTxPublishedMessage extends TradeMessage implements MailboxMessage {
    // That object is sent over the wire, so we need to take care of version compatibility.
    private static final long serialVersionUID = Version.NETWORK_PROTOCOL_VERSION;

    public final byte[] depositTx;
    private final NodeAddress senderNodeAddress;

    public DepositTxPublishedMessage(String tradeId, byte[] depositTx, NodeAddress senderNodeAddress) {
        super(tradeId);
        this.depositTx = depositTx;
        this.senderNodeAddress = senderNodeAddress;
    }

    @Override
    public NodeAddress getSenderNodeAddress() {
        return senderNodeAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DepositTxPublishedMessage)) return false;
        if (!super.equals(o)) return false;

        DepositTxPublishedMessage that = (DepositTxPublishedMessage) o;

        if (!Arrays.equals(depositTx, that.depositTx)) return false;
        return !(senderNodeAddress != null ? !senderNodeAddress.equals(that.senderNodeAddress) : that.senderNodeAddress != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (depositTx != null ? Arrays.hashCode(depositTx) : 0);
        result = 31 * result + (senderNodeAddress != null ? senderNodeAddress.hashCode() : 0);
        return result;
    }
}
