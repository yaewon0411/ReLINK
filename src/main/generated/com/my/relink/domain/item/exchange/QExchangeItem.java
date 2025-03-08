package com.my.relink.domain.item.exchange;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExchangeItem is a Querydsl query type for ExchangeItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExchangeItem extends EntityPathBase<ExchangeItem> {

    private static final long serialVersionUID = 314416324L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExchangeItem exchangeItem = new QExchangeItem("exchangeItem");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    public final StringPath brand = createString("brand");

    public final com.my.relink.domain.category.QCategory category;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> deposit = createNumber("deposit", Integer.class);

    public final StringPath description = createString("description");

    public final StringPath desiredItem = createString("desiredItem");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final EnumPath<com.my.relink.domain.item.donation.ItemQuality> itemQuality = createEnum("itemQuality", com.my.relink.domain.item.donation.ItemQuality.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath name = createString("name");

    public final StringPath size = createString("size");

    public final EnumPath<com.my.relink.domain.trade.TradeStatus> tradeStatus = createEnum("tradeStatus", com.my.relink.domain.trade.TradeStatus.class);

    public final com.my.relink.domain.user.QUser user;

    public QExchangeItem(String variable) {
        this(ExchangeItem.class, forVariable(variable), INITS);
    }

    public QExchangeItem(Path<? extends ExchangeItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExchangeItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExchangeItem(PathMetadata metadata, PathInits inits) {
        this(ExchangeItem.class, metadata, inits);
    }

    public QExchangeItem(Class<? extends ExchangeItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new com.my.relink.domain.category.QCategory(forProperty("category")) : null;
        this.user = inits.isInitialized("user") ? new com.my.relink.domain.user.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

