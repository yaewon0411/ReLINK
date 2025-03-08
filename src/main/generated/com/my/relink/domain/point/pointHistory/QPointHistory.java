package com.my.relink.domain.point.pointHistory;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPointHistory is a Querydsl query type for PointHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPointHistory extends EntityPathBase<PointHistory> {

    private static final long serialVersionUID = -378405326L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPointHistory pointHistory = new QPointHistory("pointHistory");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    public final NumberPath<Integer> amount = createNumber("amount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final com.my.relink.domain.point.QPoint point;

    public final EnumPath<PointTransactionType> pointTransactionType = createEnum("pointTransactionType", PointTransactionType.class);

    public final com.my.relink.domain.trade.QTrade trade;

    public QPointHistory(String variable) {
        this(PointHistory.class, forVariable(variable), INITS);
    }

    public QPointHistory(Path<? extends PointHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPointHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPointHistory(PathMetadata metadata, PathInits inits) {
        this(PointHistory.class, metadata, inits);
    }

    public QPointHistory(Class<? extends PointHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.point = inits.isInitialized("point") ? new com.my.relink.domain.point.QPoint(forProperty("point"), inits.get("point")) : null;
        this.trade = inits.isInitialized("trade") ? new com.my.relink.domain.trade.QTrade(forProperty("trade"), inits.get("trade")) : null;
    }

}

