package com.bidding.controller.bidder;

import com.bidding.dao.JdbcAuctionDAO;
import com.bidding.model.BidderAuctionRow;
import com.bidding.model.AuctionDisplayDTO;
import com.bidding.shared.Users;
import com.bidding.shared.UserSession;
import com.bidding.util.DataContext;
import com.bidding.util.SceneManager;
import com.bidding.service.AuctionHistoryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.util.List;

public class AuctionHistoryController extends BaseBidderController {

    @FXML private TableView<BidderAuctionRow> tblAuctionHistory;
    @FXML private TableColumn<BidderAuctionRow, String> colItemName;

    @FXML private TableColumn<BidderAuctionRow, String> colMyLastBid;
    @FXML private TableColumn<BidderAuctionRow, String> colCurrentHighest;
    @FXML private TableColumn<BidderAuctionRow, String> colStatus;
    @FXML private TableColumn<BidderAuctionRow, String> colResult;
    @FXML private TableColumn<BidderAuctionRow, Void> colAction;

    private final ObservableList<BidderAuctionRow> historyList = FXCollections.observableArrayList();
    private final AuctionHistoryService auctionHistoryService = new AuctionHistoryService();
    private final JdbcAuctionDAO auctionDAO = new JdbcAuctionDAO();

    @FXML
    public void initialize() {
        super.setupSidebarBehavior();
        setupTableColumns();
        loadHistoryData();
    }

    private void setupTableColumns() {
        colItemName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colMyLastBid.setCellValueFactory(new PropertyValueFactory<>("myLastBidStr"));
        colCurrentHighest.setCellValueFactory(new PropertyValueFactory<>("currentHighestStr"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colResult.setCellValueFactory(new PropertyValueFactory<>("result"));
        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<BidderAuctionRow, Void> call(final TableColumn<BidderAuctionRow, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Xem");
                    {
                        btn.setOnAction(e -> {
                            BidderAuctionRow row = getTableView().getItems().get(getIndex());
                            if (row != null) {
                                AuctionDisplayDTO auction = auctionDAO.getAuctionById(row.getAuctionId());
                                if (auction != null) {
                                    DataContext.getInstance().setCurrentAuction(auction);
                                    SceneManager.switchToRealtimeBidding();
                                }
                            }
                        });
                        btn.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        });

        tblAuctionHistory.setItems(historyList);
    }

    private void loadHistoryData() {
        historyList.clear();

        Users currentUser = UserSession.getInstance().getLoggedInUser();
        if (currentUser == null) {
            currentUser = DataContext.getInstance().getCurrentUser();
        }
        if (currentUser == null) return;

        List<BidderAuctionRow> data = auctionHistoryService.getAuctionHistoryForUser(currentUser.getId());
        historyList.addAll(data);
    }
}