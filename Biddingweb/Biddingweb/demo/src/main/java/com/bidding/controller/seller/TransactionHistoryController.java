package com.bidding.controller.seller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class TransactionHistoryController implements Initializable {

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbStatus;
    @FXML
    private DatePicker dpFrom;
    @FXML
    private DatePicker dpTo;
    @FXML
    private TableView<?> tblTransactions;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (cmbStatus != null) {
            cmbStatus.getItems().addAll("Tất cả", "Đã thanh toán", "Đang chờ", "Hủy");
            cmbStatus.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleFilter() {
        System.out.println("Transaction history filter clicked");
    }
}
