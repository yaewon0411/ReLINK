package com.my.relink.domain.review;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReview is a Querydsl query type for Review
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReview extends EntityPathBase<Review> {

    private static final long serialVersionUID = 1490405008L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReview review = new QReview("review");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final com.my.relink.domain.item.exchange.QExchangeItem exchangeItem;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final NumberPath<java.math.BigDecimal> star = createNumber("star", java.math.BigDecimal.class);

    public final ListPath<TradeReview, EnumPath<TradeReview>> tradeReview = this.<TradeReview, EnumPath<TradeReview>>createList("tradeReview", TradeReview.class, EnumPath.class, PathInits.DIRECT2);

    public final com.my.relink.domain.user.QUser writer;

    public QReview(String variable) {
        this(Review.class, forVariable(variable), INITS);
    }

    public QReview(Path<? extends Review> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReview(PathMetadata metadata, PathInits inits) {
        this(Review.class, metadata, inits);
    }

    public QReview(Class<? extends Review> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.exchangeItem = inits.isInitialized("exchangeItem") ? new com.my.relink.domain.item.exchange.QExchangeItem(forProperty("exchangeItem"), inits.get("exchangeItem")) : null;
        this.writer = inits.isInitialized("writer") ? new com.my.relink.domain.user.QUser(forProperty("writer"), inits.get("writer")) : null;
    }

}

