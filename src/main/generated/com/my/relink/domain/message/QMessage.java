package com.my.relink.domain.message;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMessage is a Querydsl query type for Message
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMessage extends EntityPathBase<Message> {

    private static final long serialVersionUID = -814256062L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMessage message = new QMessage("message");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> messageTime = createDateTime("messageTime", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final com.my.relink.domain.trade.QTrade trade;

    public final com.my.relink.domain.user.QUser user;

    public QMessage(String variable) {
        this(Message.class, forVariable(variable), INITS);
    }

    public QMessage(Path<? extends Message> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMessage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMessage(PathMetadata metadata, PathInits inits) {
        this(Message.class, metadata, inits);
    }

    public QMessage(Class<? extends Message> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.trade = inits.isInitialized("trade") ? new com.my.relink.domain.trade.QTrade(forProperty("trade"), inits.get("trade")) : null;
        this.user = inits.isInitialized("user") ? new com.my.relink.domain.user.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

