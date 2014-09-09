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

package io.bitsquare.gui.model;

import io.bitsquare.bank.BankAccount;
import io.bitsquare.btc.WalletFacade;
import io.bitsquare.btc.listeners.BalanceListener;
import io.bitsquare.gui.NavigationItem;
import io.bitsquare.gui.util.Profiler;
import io.bitsquare.msg.MessageFacade;
import io.bitsquare.msg.listeners.BootstrapListener;
import io.bitsquare.persistence.Persistence;
import io.bitsquare.trade.TradeManager;
import io.bitsquare.user.User;

import com.google.bitcoin.core.Coin;

import com.google.inject.Inject;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainModel extends UIModel {
    private static final Logger log = LoggerFactory.getLogger(MainModel.class);

    private final User user;
    private final WalletFacade walletFacade;
    private final MessageFacade messageFacade;
    private final TradeManager tradeManager;
    private final Persistence persistence;

    private boolean messageFacadeInited;
    private boolean walletFacadeInited;

    public final BooleanProperty backendInited = new SimpleBooleanProperty();
    public final DoubleProperty networkSyncProgress = new SimpleDoubleProperty();
    public final BooleanProperty networkSyncComplete = new SimpleBooleanProperty();
    public final BooleanProperty takeOfferRequested = new SimpleBooleanProperty();
    public final ObjectProperty<Coin> balance = new SimpleObjectProperty<>();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    private MainModel(User user, WalletFacade walletFacade, MessageFacade messageFacade,
                      TradeManager tradeManager, Persistence persistence) {
        this.user = user;
        this.walletFacade = walletFacade;
        this.messageFacade = messageFacade;
        this.tradeManager = tradeManager;
        this.persistence = persistence;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("EmptyMethod")
    @Override
    public void initialized() {
        super.initialized();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void terminate() {
        super.terminate();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Public
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void initBackend() {
        Profiler.printMsgWithTime("MainModel.initFacades");
        messageFacade.init(new BootstrapListener() {
            @Override
            public void onCompleted() {
                messageFacadeInited = true;
                if (walletFacadeInited) onFacadesInitialised();
            }

            @Override
            public void onFailed(Throwable throwable) {
                log.error(throwable.toString());
            }
        });

        walletFacade.initialize(() -> {
            walletFacadeInited = true;
            if (messageFacadeInited)
                onFacadesInitialised();


            walletFacade.addBalanceListener(new BalanceListener() {
                @Override
                public void onBalanceChanged(Coin balance) {
                    updateBalance(balance);
                }
            });
            updateBalance(walletFacade.getWalletBalance());
        });
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public void setSelectedNavigationItem(NavigationItem navigationItem) {
        persistence.write(this, "selectedNavigationItem", navigationItem);
    }

    public void setCurrentBankAccount(BankAccount bankAccount) {
        user.setCurrentBankAccount(bankAccount);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    public ObservableList<BankAccount> getBankAccounts() {
        return user.getBankAccounts();
    }

    public ObjectProperty<BankAccount> currentBankAccountProperty() {
        return user.currentBankAccountProperty();
    }

    public NavigationItem[] getSelectedNavigationItems() {
        NavigationItem[] selectedNavigationItems = (NavigationItem[]) persistence.read(this, "selectedNavigationItems");
        if (selectedNavigationItems == null || selectedNavigationItems.length == 0)
            selectedNavigationItems = new NavigationItem[]{NavigationItem.BUY};

        return selectedNavigationItems;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onFacadesInitialised() {
        // TODO Check this: never called on regtest
        // Consider to use version from Mike Hearn
        walletFacade.addDownloadListener(new WalletFacade.DownloadListener() {
            @Override
            public void progress(double percent) {
                networkSyncProgress.set(percent);
            }

            @Override
            public void downloadComplete() {
                networkSyncComplete.set(true);
            }
        });

        tradeManager.addTakeOfferRequestListener((offerId, sender) -> takeOfferRequested.set(true));

        backendInited.set(true);
    }


    private void updateBalance(Coin balance) {
        this.balance.set(balance);
    }
}