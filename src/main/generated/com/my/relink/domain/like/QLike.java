package com.my.relink.domain.like;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLike is a Querydsl query type for Like
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLike extends EntityPathBase<Like> {

    private static final long serialVersionUID = -1659392912L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLike like = new QLike("like1");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.my.relink.domain.item.exchange.QExchangeItem exchangeItem;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final com.my.relink.domain.user.QUser user;

    public QLike(String variable) {
        this(Like.class, forVariable(variable), INITS);
    }

    public QLike(Path<? extends Like> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLike(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLike(PathMetadata metadata, PathInits inits) {
        this(Like.class, metadata, inits);
    }

    public QLike(Class<? extends Like> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.exchangeItem = inits.isInitialized("exchangeItem") ? new com.my.relink.domain.item.exchange.QExchangeItem(forProperty("exchangeItem"), inits.get("exchangeItem")) : null;
        this.user = inits.isInitialized("user") ? new com.my.relink.domain.user.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

