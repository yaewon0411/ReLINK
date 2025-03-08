package com.my.relink.domain.trade;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTrade is a Querydsl query type for Trade
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTrade extends EntityPathBase<Trade> {

    private static final long serialVersionUID = -366218116L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTrade trade = new QTrade("trade");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    public final EnumPath<TradeCancelReason> cancelReason = createEnum("cancelReason", TradeCancelReason.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath hasOwnerReceived = createBoolean("hasOwnerReceived");

    public final BooleanPath hasOwnerRequested = createBoolean("hasOwnerRequested");

    public final BooleanPath hasRequesterReceived = createBoolean("hasRequesterReceived");

    public final BooleanPath hasRequesterRequested = createBoolean("hasRequesterRequested");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final com.my.relink.domain.user.QAddress ownerAddress;

    public final com.my.relink.domain.item.exchange.QExchangeItem ownerExchangeItem;

    public final StringPath ownerTrackingNumber = createString("ownerTrackingNumber");

    public final com.my.relink.domain.user.QUser requester;

    public final com.my.relink.domain.user.QAddress requesterAddress;

    public final com.my.relink.domain.item.exchange.QExchangeItem requesterExchangeItem;

    public final StringPath requesterTrackingNumber = createString("requesterTrackingNumber");

    public final StringPath tradeCancelDescription = createString("tradeCancelDescription");

    public final EnumPath<TradeStatus> tradeStatus = createEnum("tradeStatus", TradeStatus.class);

    public QTrade(String variable) {
        this(Trade.class, forVariable(variable), INITS);
    }

    public QTrade(Path<? extends Trade> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTrade(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTrade(PathMetadata metadata, PathInits inits) {
        this(Trade.class, metadata, inits);
    }

    public QTrade(Class<? extends Trade> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.ownerAddress = inits.isInitialized("ownerAddress") ? new com.my.relink.domain.user.QAddress(forProperty("ownerAddress")) : null;
        this.ownerExchangeItem = inits.isInitialized("ownerExchangeItem") ? new com.my.relink.domain.item.exchange.QExchangeItem(forProperty("ownerExchangeItem"), inits.get("ownerExchangeItem")) : null;
        this.requester = inits.isInitialized("requester") ? new com.my.relink.domain.user.QUser(forProperty("requester"), inits.get("requester")) : null;
        this.requesterAddress = inits.isInitialized("requesterAddress") ? new com.my.relink.domain.user.QAddress(forProperty("requesterAddress")) : null;
        this.requesterExchangeItem = inits.isInitialized("requesterExchangeItem") ? new com.my.relink.domain.item.exchange.QExchangeItem(forProperty("requesterExchangeItem"), inits.get("requesterExchangeItem")) : null;
    }

}

