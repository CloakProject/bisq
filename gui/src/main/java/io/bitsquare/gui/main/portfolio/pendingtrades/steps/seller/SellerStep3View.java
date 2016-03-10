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

package io.bitsquare.gui.main.portfolio.pendingtrades.steps.seller;

import io.bitsquare.common.util.Tuple3;
import io.bitsquare.gui.components.TextFieldWithCopyIcon;
import io.bitsquare.gui.components.TitledGroupBg;
import io.bitsquare.gui.main.overlays.popups.Popup;
import io.bitsquare.gui.main.portfolio.pendingtrades.PendingTradesViewModel;
import io.bitsquare.gui.main.portfolio.pendingtrades.steps.TradeStepView;
import io.bitsquare.gui.util.Layout;
import io.bitsquare.locale.CurrencyUtil;
import io.bitsquare.payment.CryptoCurrencyAccountContractData;
import io.bitsquare.payment.PaymentAccountContractData;
import io.bitsquare.trade.Contract;
import io.bitsquare.trade.Trade;
import io.bitsquare.user.Preferences;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.apache.commons.lang3.StringUtils;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

import static io.bitsquare.gui.util.FormBuilder.*;

public class SellerStep3View extends TradeStepView {

    private Button confirmButton;
    private Label statusLabel;
    private ProgressIndicator statusProgressIndicator;
    private Subscription tradeStatePropertySubscription;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, Initialisation
    ///////////////////////////////////////////////////////////////////////////////////////////

    public SellerStep3View(PendingTradesViewModel model) {
        super(model);
    }

    @Override
    public void activate() {
        super.activate();

        tradeStatePropertySubscription = EasyBind.subscribe(trade.stateProperty(), state -> {
            if (state == Trade.State.SELLER_RECEIVED_FIAT_PAYMENT_INITIATED_MSG) {
                PaymentAccountContractData paymentAccountContractData = model.dataModel.getSellersPaymentAccountContractData();
                String key = "confirmPayment" + trade.getId();
                String message;
                String tradeAmountWithCode = model.formatter.formatFiatWithCode(trade.getTradeVolume());
                String currencyName = CurrencyUtil.getNameByCode(trade.getOffer().getCurrencyCode());
                if (paymentAccountContractData instanceof CryptoCurrencyAccountContractData) {
                    String address = ((CryptoCurrencyAccountContractData) paymentAccountContractData).getAddress();
                    message = "Your trading partner has confirmed that he initiated the " + currencyName + " payment.\n\n" +
                            "Please check on your favorite " + currencyName +
                            " blockchain explorer if the transaction to your receiving address\n" +
                            "" + address + "\n" +
                            "has already sufficient blockchain confirmations.\n" +
                            "The payment amount has to be " + tradeAmountWithCode + "\n\n" +
                            "You can copy & paste your " + currencyName + " address from the main screen after " +
                            "closing that popup.";
                } else {
                    message = "Your trading partner has confirmed that he initiated the " + currencyName + " payment.\n\n" +
                            "Please go to your online banking web page and check if you have received " +
                            tradeAmountWithCode + " from the bitcoin buyer.\n\n" +
                            "The reference text of the transaction is: \"" + trade.getShortId() + "\"";
                }
                if (preferences.showAgain(key)) {
                    preferences.dontShowAgain(key, true);
                    new Popup().headLine("Attention required for trade with ID " + trade.getShortId())
                            .attention(message)
                            .show();
                }

            } else if (state == Trade.State.SELLER_CONFIRMED_FIAT_PAYMENT_RECEIPT) {
                showStatusInfo();
                statusLabel.setText("Sending confirmation...");
            } else if (state == Trade.State.SELLER_SENT_FIAT_PAYMENT_RECEIPT_MSG) {
                hideStatusInfo();
            }
        });
    }

    @Override
    public void deactivate() {
        super.deactivate();

        if (tradeStatePropertySubscription != null) {
            tradeStatePropertySubscription.unsubscribe();
            tradeStatePropertySubscription = null;
        }

        hideStatusInfo();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Content
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void addContent() {
        addTradeInfoBlock();

        TitledGroupBg titledGroupBg = addTitledGroupBg(gridPane, ++gridRow, 2, "Confirm payment receipt", Layout.GROUP_DISTANCE);

        TextFieldWithCopyIcon field = addLabelTextFieldWithCopyIcon(gridPane, gridRow, "Amount to receive:",
                model.getFiatAmount(), Layout.FIRST_ROW_AND_GROUP_DISTANCE).second;
        field.setCopyWithoutCurrencyPostFix(true);

        String paymentDetails = "";
        String title = "";
        boolean isBlockChain = false;
        String nameByCode = CurrencyUtil.getNameByCode(trade.getOffer().getCurrencyCode());
        Contract contract = trade.getContract();
        if (contract != null) {
            PaymentAccountContractData paymentAccountContractData = contract.getSellerPaymentAccountContractData();
            if (paymentAccountContractData instanceof CryptoCurrencyAccountContractData) {
                paymentDetails = ((CryptoCurrencyAccountContractData) paymentAccountContractData).getAddress();
                title = "Your " + nameByCode + " address:";
                isBlockChain = true;
            } else {
                paymentDetails = paymentAccountContractData.getPaymentDetails();
                title = "Your payment account:";
            }
        }

        TextFieldWithCopyIcon paymentDetailsTextField = addLabelTextFieldWithCopyIcon(gridPane, ++gridRow,
                title, StringUtils.abbreviate(paymentDetails, 56)).second;
        paymentDetailsTextField.setMouseTransparent(false);
        paymentDetailsTextField.setTooltip(new Tooltip(paymentDetails));

        if (!isBlockChain) {
            addLabelTextFieldWithCopyIcon(gridPane, ++gridRow, "Reference text:", model.dataModel.getReference());
            GridPane.setRowSpan(titledGroupBg, 3);
        }

        Tuple3<Button, ProgressIndicator, Label> tuple = addButtonWithStatusAfterGroup(gridPane, ++gridRow, "Confirm payment receipt");
        confirmButton = tuple.first;
        confirmButton.setOnAction(e -> onPaymentReceived());
        statusProgressIndicator = tuple.second;
        statusLabel = tuple.third;

        hideStatusInfo();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Info
    ///////////////////////////////////////////////////////////////////////////////////////////


    @Override
    protected String getInfoText() {
        if (model.isBlockChainMethod()) {
            return "The bitcoin buyer has started the " + model.dataModel.getCurrencyCode() + " payment.\n" +
                    "Check for blockchain confirmations at your cryptocurrency wallet or block explorer and " +
                    "confirm the payment when you have sufficient blockchain confirmations.";
        } else {
            return "The bitcoin buyer has started the " + model.dataModel.getCurrencyCode() + " payment.\n" +
                    "Check at your payment account (e.g. bank account) and confirm when you have " +
                    "received the payment.";
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Warning
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected String getWarningText() {
        setWarningHeadline();
        String substitute = model.isBlockChainMethod() ?
                "on the " + model.dataModel.getCurrencyCode() + "blockchain" :
                "at your payment provider (e.g. bank)";
        return "You still have not confirmed the receipt of the payment!\n" +
                "Please check " + substitute + " if you have received the payment.\n" +
                "If you do not confirm receipt until " +
                model.getOpenDisputeTimeAsFormattedDate() +
                " the trade will be investigated by the arbitrator.";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Dispute
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected String getOpenForDisputeText() {
        return "You have not confirmed the receipt of the payment!\n" +
                "The max. period for the trade has elapsed.\n" +
                "Please contact the arbitrator for opening a dispute.";
    }

    @Override
    protected void applyOnDisputeOpened() {
        confirmButton.setDisable(true);
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    // UI Handlers
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onPaymentReceived() {
        log.debug("onPaymentReceived");
        if (model.p2PService.isBootstrapped()) {
            Preferences preferences = model.dataModel.preferences;
            String key = "confirmPaymentReceived";
            if (preferences.showAgain(key)) {
                new Popup()
                        .headLine("Confirm that you have received the payment")
                        .confirmation("Have you received the " + model.dataModel.getCurrencyCode() + " payment from your trading partner?\n\n" +
                                "Please note that as soon you have confirmed the receipt, the locked trade amount will be released " +
                                "to the bitcoin buyer and the security deposit will be refunded.")
                        .width(700)
                        .actionButtonText("Yes, I have received the payment")
                        .onAction(this::confirmPaymentReceived)
                        .closeButtonText("Cancel")
                        .dontShowAgainId(key, preferences)
                        .show();
            } else {
                confirmPaymentReceived();
            }
        } else {
            new Popup().warning("You need to wait until your client is bootstrapped in the network.\n" +
                    "That might take up to about 2 minutes at startup.").show();
        }
    }

    private void confirmPaymentReceived() {
        confirmButton.setDisable(true);

        model.dataModel.onFiatPaymentReceived(() -> {
            // In case the first send failed we got the support button displayed. 
            // If it succeeds at a second try we remove the support button again.
            //TODO check for support. in case of a dispute we dont want to hide the button
            //if (notificationGroup != null) 
            //   notificationGroup.setButtonVisible(false);
        }, errorMessage -> {
            confirmButton.setDisable(false);
            hideStatusInfo();
            new Popup().warning("Sending message to your trading partner failed.\n" +
                    "Please try again and if it continue to fail report a bug.").show();
        });
    }

    private void showStatusInfo() {
        statusProgressIndicator.setVisible(true);
        statusProgressIndicator.setManaged(true);
        statusProgressIndicator.setProgress(-1);
    }

    private void hideStatusInfo() {
        statusProgressIndicator.setVisible(false);
        statusProgressIndicator.setManaged(false);
        statusProgressIndicator.setProgress(0);
        statusLabel.setText("");
    }
}

