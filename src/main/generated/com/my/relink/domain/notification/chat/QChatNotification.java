package com.my.relink.domain.notification.chat;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatNotification is a Querydsl query type for ChatNotification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatNotification extends EntityPathBase<ChatNotification> {

    private static final long serialVersionUID = 1186906388L;

    public static final QChatNotification chatNotification = new QChatNotification("chatNotification");

    public final com.my.relink.domain.notification.QNotification _super = new com.my.relink.domain.notification.QNotification(this);

    public final EnumPath<ChatStatus> chatStatus = createEnum("chatStatus", ChatStatus.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath exchangeItemName = createString("exchangeItemName");

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath requestUserNickname = createString("requestUserNickname");

    //inherited
    public final NumberPath<Long> userId = _super.userId;

    public QChatNotification(String variable) {
        super(ChatNotification.class, forVariable(variable));
    }

    public QChatNotification(Path<? extends ChatNotification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatNotification(PathMetadata metadata) {
        super(ChatNotification.class, metadata);
    }

}

