package com.my.relink.service;

import com.my.relink.common.notification.NotificationPublisherService;
import com.my.relink.config.security.AuthUser;
import com.my.relink.controller.trade.dto.request.AddressReqDto;
import com.my.relink.controller.trade.dto.request.TrackingNumberReqDto;
import com.my.relink.controller.trade.dto.request.TradeCancelReqDto;
import com.my.relink.controller.trade.dto.response.*;
import com.my.relink.domain.item.exchange.ExchangeItem;
import com.my.relink.domain.message.repository.MessageRepository;
import com.my.relink.domain.point.pointHistory.PointHistory;
import com.my.relink.domain.point.pointHistory.repository.PointHistoryRepository;
import com.my.relink.domain.trade.Trade;
import com.my.relink.domain.trade.TradeStatus;
import com.my.relink.domain.trade.repository.TradeRepository;
import com.my.relink.domain.trade.repository.dto.TradeWithOwnerItemNameDto;
import com.my.relink.domain.user.Address;
import com.my.relink.domain.user.User;
import com.my.relink.domain.user.repository.UserRepository;
import com.my.relink.ex.BusinessException;
import com.my.relink.ex.ErrorCode;
import com.my.relink.util.DateTimeUtil;
import com.my.relink.util.MetricConstants;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Timed(MetricConstants.SERVICE_TRADE_TIME)
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserTrustScoreService userTrustScoreService;
    private final ImageService imageService;
    private final UserRepository userRepository;
    private final PointTransactionService pointTransactionService;
    private final DateTimeUtil dateTimeUtil;
    private final PointHistoryRepository pointHistoryRepository;
    private final NotificationPublisherService notificationPublisherService;
    private final MessageRepository messageRepository;

    /**
     * [문의하기] -> 해당 채팅방의 거래 정보, 상품 정보, 상대 유저 정보 내리기
     *
     * @param tradeId
     * @param userId
     * @return
     */
    public TradeInquiryDetailRespDto getTradeInquiryDetail(Long tradeId, Long userId) {
        Trade trade = tradeRepository.findByIdWithItemsAndUser(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        trade.validateAccess(userId);

        String requestedItemImageUrl = imageService.getExchangeItemThumbnailUrl(trade.getRequesterExchangeItem());
        User partner = trade.getPartner(userId);
        int trustScoreOfPartner = userTrustScoreService.getTrustScore(partner);

        return new TradeInquiryDetailRespDto(trade, partner, trustScoreOfPartner, requestedItemImageUrl);
    }


    public Trade findByIdOrFail(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));
    }


    @Cacheable(value = "tradeStatus", key = "#tradeId")
    public TradeStatus findByIdOrFailWhenSend(Long tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND))
                .getTradeStatus();
    }

    public Trade findByIdWithUsersOrFail(Long tradeId) {
        return tradeRepository.findByIdWithUsers(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));
    }

    public Trade findByIdFetchItemsAndUsersOrFail(Long tradeId) {
        return tradeRepository.findByIdWithItemsAndUser(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));
    }


    public Trade findByIdWithOwnerItemOrFail(Long tradeId) {
        return tradeRepository.findByIdWithOwnerItem(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));
    }

    @Transactional
    @CacheEvict(value = "tradeStatus", key = "#tradeId")
    @CachePut(value = "tradeStatus", key = "#tradeId")
    public TradeRequestRespDto requestTrade(Long tradeId, AuthUser authUser) {

        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId).
                orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        //거래 상대방이 탈퇴했을 때는 거래 취소 && 보증금 복원
        User partnerUser = trade.getPartner(currentUser.getId());
        boolean isPartnerDeleted = !userRepository.existsByIdAndIsDeletedFalse(partnerUser.getId());

        if (isPartnerDeleted) {
            pointTransactionService.restorePoints(tradeId, currentUser);
            trade.updateTradeStatus(TradeStatus.CANCELED);
            tradeRepository.save(trade);

            throw new BusinessException(ErrorCode.USER_SECESSION);
        }

        //차감 메서드 위임
        pointTransactionService.deductPoints(tradeId, currentUser);

        //요청자/소유자 여부에 따라 적절한 요청 상태 필드 업데이트
        if (trade.isRequester(currentUser.getId())) {
            trade.updateHasRequesterRequested(true);
        } else {
            trade.updateHasOwnerRequested(true);
        }
        // 양쪽 모두 requested가 true라면 거래 상태를 in_exchange로 변경
        if (trade.getHasRequesterRequested() && trade.getHasOwnerRequested()) {
            trade.updateTradeStatus(TradeStatus.IN_EXCHANGE);
            tradeRepository.save(trade);
        }

        notificationPublisherService.createExchangeNotification(
                currentUser.getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.IN_EXCHANGE
        );

        return new TradeRequestRespDto(tradeId);
    }

    @Transactional
    @CacheEvict(value = "tradeStatus", key = "#tradeId")
    @CachePut(value = "tradeStatus", key = "#tradeId")
    public void cancelTradeRequest(Long tradeId, AuthUser authUser) {

        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        //거래 상대방이 탈퇴했을 떄 거래 취소 && 보증금 복원
        User partnerUser = trade.getPartner(currentUser.getId());
        boolean isPartnerDeleted = !userRepository.existsByIdAndIsDeletedFalse(partnerUser.getId());

        if (isPartnerDeleted) {
            pointTransactionService.restorePoints(tradeId, currentUser);
            trade.updateTradeStatus(TradeStatus.CANCELED);
            tradeRepository.save(trade);

            throw new BusinessException(ErrorCode.USER_SECESSION);
        }

        //복원 메서드 위임
        pointTransactionService.restorePoints(tradeId, currentUser);

        // 요청 상태 업데이트
        if (trade.isRequester(currentUser.getId())) {
            trade.updateHasRequesterRequested(false);
        } else {
            trade.updateHasOwnerRequested(false);
        }

        // 거래 상태 확인 및 업데이트 (양쪽 중 하나가 false라면 초기 상태로 되돌리기)
        if (!trade.getHasRequesterRequested() || !trade.getHasOwnerRequested()) {
            trade.updateTradeStatus(TradeStatus.AVAILABLE);
            tradeRepository.save(trade);
        }

        notificationPublisherService.createExchangeNotification(
                currentUser.getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.AVAILABLE
        );
    }

    @Transactional
    public AddressRespDto createAddress(Long tradeId, AddressReqDto reqDto, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        if (trade.getHasOwnerRequested() && trade.getHasRequesterRequested()) {
            // 소유자 주소와 요청자 주소 업데이트
            if (trade.isRequester(currentUser.getId())) {
                Address requesterAddress = reqDto.toRequesterAddressEntity();  // 요청자 주소 생성
                trade.saveRequesterAddress(requesterAddress);
            } else {
                Address ownerAddress = reqDto.toOwnerAddressEntity();  // 소유자 주소 생성
                trade.saveOwnerAddress(ownerAddress);
            }

            tradeRepository.save(trade);
            return new AddressRespDto(tradeId);
        } else {
            throw new BusinessException(ErrorCode.TRADE_ACCESS_DENIED);
        }
    }

    @Transactional
    @CacheEvict(value = "tradeStatus", key = "#tradeId")
    @CachePut(value = "tradeStatus", key = "#tradeId")
    public TradeCompleteRespDto completeTrade(Long tradeId, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        //요청자/소유자 여부에 따라 수령상태 변경
        if (trade.isRequester(currentUser.getId())) {
            trade.updateHasRequesterReceived(true);
        } else {
            trade.updateHasOwnerReceived(true);
        }

        //양쪽 모두 수령 확인 시 거래 상태 변경
        if (trade.getHasOwnerReceived() && trade.getHasRequesterReceived()) {
            trade.updateTradeStatus(TradeStatus.EXCHANGED);
        }
        //보증금 반환
        Integer amount = trade.getOwnerExchangeItem().getDeposit();
        pointTransactionService.restorePointsForAllTraders(trade, amount);
        tradeRepository.save(trade);

        notificationPublisherService.createExchangeNotification(
                currentUser.getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.EXCHANGED
        );

        return new TradeCompleteRespDto(tradeId);
    }

    @Transactional
    @CacheEvict(value = "tradeStatus", key = "#tradeId")
    public void getExchangeItemTrackingNumber(Long tradeId, TrackingNumberReqDto reqDto, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        if (trade.isRequester(currentUser.getId())) {
            trade.updateRequesterTrackingNumber(reqDto.getTrackingNumber());
        } else {
            trade.updateOwnerTrackingNumber(reqDto.getTrackingNumber());
        }

        if (!trade.getOwnerTrackingNumber().isEmpty() && !trade.getRequesterTrackingNumber().isEmpty()) {
            trade.updateTradeStatus(TradeStatus.IN_DELIVERY);
        }
        tradeRepository.save(trade);

        notificationPublisherService.createExchangeNotification(
                currentUser.getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.IN_DELIVERY
        );

    }

    public TradeCompletionRespDto findCompleteTradeInfo(Long tradeId, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findTradeWithDetails(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        ExchangeItem myExchangeItem = trade.getMyExchangeItem(currentUser.getId());
        ExchangeItem partnerExchangeItem = trade.getPartnerExchangeItem(currentUser.getId());

        String myImage = imageService.getExchangeItemThumbnailUrl(myExchangeItem);
        String partnerImage = imageService.getExchangeItemThumbnailUrl(partnerExchangeItem);

        User partnerUser = partnerExchangeItem.getUser();

        return TradeCompletionRespDto.from(myExchangeItem, partnerExchangeItem, myImage, partnerImage, partnerUser, trade, dateTimeUtil);
    }

    public Map<Long, Trade> getTradesByItemIds(List<Long> itemIds) {
        List<Trade> trades = tradeRepository.findByExchangeItemIds(itemIds);
        return trades.stream()
                .flatMap(trade -> List.of(
                        Map.entry(trade.getOwnerExchangeItem().getId(), trade),
                        Map.entry(trade.getRequesterExchangeItem().getId(), trade)
                ).stream())
                .filter(entry -> itemIds.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    }

    public ViewTradeCancelRespDto viewCancelTrade(Long tradeId, AuthUser authUser) {

        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        ExchangeItem partnerExchangeItem = trade.getPartnerExchangeItem(currentUser.getId());

        User partnerUser = partnerExchangeItem.getUser();

        String partnerImage = imageService.getExchangeItemThumbnailUrl(partnerExchangeItem);
        String tradeStartedAt = dateTimeUtil.getTradeStatusFormattedTime(trade.getCreatedAt());

        return ViewTradeCancelRespDto.from(partnerUser, partnerExchangeItem, partnerImage, tradeStartedAt);
    }

    @Transactional
    @CacheEvict(value = "tradeStatus", key = "#tradeId")
    public TradeCancelRespDto cancelTrade(Long tradeId, TradeCancelReqDto reqDto, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        //거래 상태 확인, 요청자 확인
        //보증금 환급 처리
        if (!trade.isTradeInExchange(trade) || !trade.isParticipant(currentUser.getId())) {
            throw new BusinessException(ErrorCode.TRADE_ACCESS_DENIED);
        }

        PointHistory mypointHistory = pointHistoryRepository.findFirstByTradeIdAndUserIdByCreatedAtDesc(tradeId, currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_HISTORY_NOT_FOUND));
        PointHistory partnerPointHistory = pointHistoryRepository.findFirstByTradeIdAndUserIdByCreatedAtDesc(tradeId, trade.getPartner(currentUser.getId()).getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_HISTORY_NOT_FOUND));

        if (mypointHistory.isRefunded() || partnerPointHistory.isRefunded()) {
            throw new BusinessException(ErrorCode.DEPOSIT_ALREADY_REFUNDED);
        }

        Integer amount = trade.getOwnerExchangeItem().getDeposit();
        pointTransactionService.restorePointsForAllTraders(trade, amount);

        trade.updateTradeStatus(TradeStatus.CANCELED);
        trade.getOwnerExchangeItem().updateStatus(TradeStatus.AVAILABLE);

        trade.updateTradeCancelReason(reqDto.getTradeCancelReason(), reqDto.getTradeCancelDescription());
        tradeRepository.save(trade);

        notificationPublisherService.createExchangeNotification(
                currentUser.getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.CANCELED
        );

        notificationPublisherService.createExchangeNotification(
                trade.getPartner(currentUser.getId()).getId(),
                trade.getOwnerExchangeItem().getName(),
                trade.getRequester().getNickname(),
                TradeStatus.CANCELED
        );

        return new TradeCancelRespDto(tradeId);

    }

    public ViewReviewRespDto getReviewInfo(Long tradeId, AuthUser authUser) {
        User currentUser = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));

        ExchangeItem partnerExchangeItem = trade.getPartnerExchangeItem(currentUser.getId());
        User partnerUser = partnerExchangeItem.getUser();
        String partnerImage = imageService.getExchangeItemThumbnailUrl(partnerExchangeItem);
        String completedAt = dateTimeUtil.getTradeStatusFormattedTime(trade.getModifiedAt());

        return ViewReviewRespDto.from(trade, partnerImage, partnerUser, partnerExchangeItem, completedAt);

    }

    @Transactional
    public TradeIdRespDto createTrade(ExchangeItem itemFromOwner, ExchangeItem itemFromRequester, User requester) {
        Trade trade = Trade.builder()
                .requester(requester)
                .ownerExchangeItem(itemFromOwner)
                .requesterExchangeItem(itemFromRequester)
                .tradeStatus(TradeStatus.AVAILABLE)
                .hasOwnerRequested(false)
                .hasRequesterRequested(false)
                .hasOwnerReceived(false)
                .hasRequesterReceived(false)
                .build();
        Trade savedTrade = tradeRepository.save(trade);
        return new TradeIdRespDto(savedTrade.getId());
    }

    public void deleteTrade(Long itemId, Long userId) {
        tradeRepository.findByExchangeItemId(itemId)
                .filter(trade -> {
                    ExchangeItem partnerItem = trade.getPartnerExchangeItem(userId);
                    return partnerItem.isDeleted() || partnerItem.getUser().isDeleted();
                })
                .ifPresent(trade -> {
                    tradeRepository.delete(trade);
                    messageRepository.deleteMessage(trade.getId());
                });
    }

    @Cacheable(value = "tradeInfo", key = "#tradeId")
    public TradeWithOwnerItemNameDto findTradeWithOwnerItemName(Long tradeId) {
        return tradeRepository.findTradeWithOwnerItemNameById(tradeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRADE_NOT_FOUND));
    }
}

