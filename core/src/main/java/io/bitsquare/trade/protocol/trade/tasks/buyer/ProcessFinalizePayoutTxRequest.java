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

package io.bitsquare.trade.protocol.trade.tasks.buyer;

import io.bitsquare.common.taskrunner.TaskRunner;
import io.bitsquare.trade.Trade;
import io.bitsquare.trade.protocol.trade.messages.FinalizePayoutTxRequest;
import io.bitsquare.trade.protocol.trade.tasks.TradeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.bitsquare.util.Validator.*;

public class ProcessFinalizePayoutTxRequest extends TradeTask {
    private static final Logger log = LoggerFactory.getLogger(ProcessFinalizePayoutTxRequest.class);

    public ProcessFinalizePayoutTxRequest(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            log.debug("current trade state " + trade.getState());
            FinalizePayoutTxRequest message = (FinalizePayoutTxRequest) processModel.getTradeMessage();
            checkTradeId(processModel.getId(), message);
            checkNotNull(message);

            processModel.tradingPeer.setSignature(checkNotNull(message.sellerSignature));
            processModel.tradingPeer.setPayoutAddressString(nonEmptyStringOf(message.sellerPayoutAddress));
            trade.setLockTimeAsBlockHeight(nonNegativeLongOf(message.lockTime));

            trade.setState(Trade.State.FIAT_PAYMENT_RECEIPT_MSG_RECEIVED);

            // update to the latest peer address of our peer if the message is correct
            trade.setTradingPeerNodeAddress(processModel.getTempTradingPeerNodeAddress());

            removeMailboxMessageAfterProcessing();

            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}