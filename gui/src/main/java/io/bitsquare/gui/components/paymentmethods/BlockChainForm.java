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

package io.bitsquare.gui.components.paymentmethods;

import io.bitsquare.gui.components.InputTextField;
import io.bitsquare.gui.util.Layout;
import io.bitsquare.gui.util.validation.AltCoinAddressValidator;
import io.bitsquare.gui.util.validation.InputValidator;
import io.bitsquare.locale.BSResources;
import io.bitsquare.locale.CurrencyUtil;
import io.bitsquare.locale.TradeCurrency;
import io.bitsquare.payment.BlockChainAccount;
import io.bitsquare.payment.BlockChainAccountContractData;
import io.bitsquare.payment.PaymentAccount;
import io.bitsquare.payment.PaymentAccountContractData;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.bitsquare.gui.util.FormBuilder.*;

public class BlockChainForm extends PaymentMethodForm {
    private static final Logger log = LoggerFactory.getLogger(BlockChainForm.class);

    private final BlockChainAccount blockChainAccount;
    private final AltCoinAddressValidator altCoinAddressValidator;
    private InputTextField addressInputTextField;

    private ComboBox<TradeCurrency> currencyComboBox;

    public static int addFormForBuyer(GridPane gridPane, int gridRow, PaymentAccountContractData paymentAccountContractData) {
        addLabelTextField(gridPane, ++gridRow, "Payment method:", BSResources.get(paymentAccountContractData.getPaymentMethodName()));
        addLabelTextFieldWithCopyIcon(gridPane, ++gridRow, "Address:", ((BlockChainAccountContractData) paymentAccountContractData).getAddress());
        if (paymentAccountContractData instanceof BlockChainAccountContractData &&
                ((BlockChainAccountContractData) paymentAccountContractData).getPaymentId() != null)
            addLabelTextFieldWithCopyIcon(gridPane, ++gridRow, "Payment ID:", ((BlockChainAccountContractData) paymentAccountContractData).getPaymentId());

        addAllowedPeriod(gridPane, ++gridRow, paymentAccountContractData);
        return gridRow;
    }

    public BlockChainForm(PaymentAccount paymentAccount, AltCoinAddressValidator altCoinAddressValidator, InputValidator inputValidator, GridPane gridPane,
                          int gridRow) {
        super(paymentAccount, inputValidator, gridPane, gridRow);
        this.blockChainAccount = (BlockChainAccount) paymentAccount;
        this.altCoinAddressValidator = altCoinAddressValidator;
    }

    @Override
    public void addFormForAddAccount() {
        gridRowFrom = gridRow + 1;

        addTradeCurrencyComboBox();
        currencyComboBox.setPrefWidth(250);
        addressInputTextField = addLabelInputTextField(gridPane, ++gridRow, "Receiving altcoin address:").second;
        addressInputTextField.setValidator(altCoinAddressValidator);

        addressInputTextField.textProperty().addListener((ov, oldValue, newValue) -> {
            blockChainAccount.setAddress(newValue);
            updateFromInputs();
        });

        addAllowedPeriod();
        addAccountNameTextFieldWithAutoFillCheckBox();
    }

    @Override
    protected void autoFillNameTextField() {
        if (autoFillCheckBox != null && autoFillCheckBox.isSelected()) {
            String method = BSResources.get(paymentAccount.getPaymentMethod().getId());
            String address = addressInputTextField.getText();
            address = address.substring(0, Math.min(9, address.length())) + "...";
            String currency = paymentAccount.getSingleTradeCurrency() != null ? paymentAccount.getSingleTradeCurrency().getCode() : "?";
            accountNameTextField.setText(method.concat(", ").concat(currency).concat(", ").concat(address));
        }
    }

    @Override
    public void addFormForDisplayAccount() {
        gridRowFrom = gridRow;
        addLabelTextField(gridPane, gridRow, "Account name:", blockChainAccount.getAccountName(), Layout.FIRST_ROW_AND_GROUP_DISTANCE);
        addLabelTextField(gridPane, ++gridRow, "Payment method:", BSResources.get(blockChainAccount.getPaymentMethod().getId()));
        TextField field = addLabelTextField(gridPane, ++gridRow, "Receiving altcoin address:", blockChainAccount.getAddress()).second;
        field.setMouseTransparent(false);
        addLabelTextField(gridPane, ++gridRow, "Crypto currency:", blockChainAccount.getSingleTradeCurrency().getCodeAndName());
        addAllowedPeriod();
    }

    @Override
    public void updateAllInputsValid() {
        allInputsValid.set(isAccountNameValid()
                && altCoinAddressValidator.validate(blockChainAccount.getAddress()).isValid
                && blockChainAccount.getSingleTradeCurrency() != null);
    }

    @Override
    protected void addTradeCurrencyComboBox() {
        currencyComboBox = addLabelComboBox(gridPane, ++gridRow, "Crypto currency:").second;
        currencyComboBox.setPromptText("Select crypto currency");
        currencyComboBox.setItems(FXCollections.observableArrayList(CurrencyUtil.getSortedCryptoCurrencies()));
        currencyComboBox.setVisibleRowCount(Math.min(currencyComboBox.getItems().size(), 20));
        currencyComboBox.setConverter(new StringConverter<TradeCurrency>() {
            @Override
            public String toString(TradeCurrency tradeCurrency) {
                return tradeCurrency.getCodeAndName();
            }

            @Override
            public TradeCurrency fromString(String s) {
                return null;
            }
        });
        currencyComboBox.setOnAction(e -> {
            paymentAccount.setSingleTradeCurrency(currencyComboBox.getSelectionModel().getSelectedItem());
            updateFromInputs();
        });
    }
}
