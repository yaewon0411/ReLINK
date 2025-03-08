package com.my.relink.domain.notification.exchange;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QExchangeNotification is a Querydsl query type for ExchangeNotification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExchangeNotification extends EntityPathBase<ExchangeNotification> {

    private static final long serialVersionUID = -360886412L;

    public static final QExchangeNotification exchangeNotification = new QExchangeNotification("exchangeNotification");

    public final com.my.relink.domain.notification.QNotification _super = new com.my.relink.domain.notification.QNotification(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath exchangeItemName = createString("exchangeItemName");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath requestUserNickname = createString("requestUserNickname");

    public final EnumPath<com.my.relink.domain.trade.TradeStatus> tradeStatus = createEnum("tradeStatus", com.my.relink.domain.trade.TradeStatus.class);

    //inherited
    public final NumberPath<Long> userId = _super.userId;

    public QExchangeNotification(String variable) {
        super(ExchangeNotification.class, forVariable(variable));
    }

    public QExchangeNotification(Path<? extends ExchangeNotification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QExchangeNotification(PathMetadata metadata) {
        super(ExchangeNotification.class, metadata);
    }

}

