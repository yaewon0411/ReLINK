package com.my.relink.domain.item.donation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDonationItem is a Querydsl query type for DonationItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDonationItem extends EntityPathBase<DonationItem> {

    private static final long serialVersionUID = -1793304156L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDonationItem donationItem = new QDonationItem("donationItem");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    public final com.my.relink.domain.category.QCategory category;

    public final StringPath certificateUrl = createString("certificateUrl");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final StringPath desiredDestination = createString("desiredDestination");

    public final StringPath destination = createString("destination");

    public final StringPath detailRejectedReason = createString("detailRejectedReason");

    public final EnumPath<DisposalType> disposalType = createEnum("disposalType", DisposalType.class);

    public final EnumPath<DonationStatus> donationStatus = createEnum("donationStatus", DonationStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<ItemQuality> itemQuality = createEnum("itemQuality", ItemQuality.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final StringPath name = createString("name");

    public final EnumPath<RejectedReason> rejectedReason = createEnum("rejectedReason", RejectedReason.class);

    public final com.my.relink.domain.user.QAddress returnAddress;

    public final StringPath size = createString("size");

    public final com.my.relink.domain.user.QUser user;

    public QDonationItem(String variable) {
        this(DonationItem.class, forVariable(variable), INITS);
    }

    public QDonationItem(Path<? extends DonationItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDonationItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDonationItem(PathMetadata metadata, PathInits inits) {
        this(DonationItem.class, metadata, inits);
    }

    public QDonationItem(Class<? extends DonationItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new com.my.relink.domain.category.QCategory(forProperty("category")) : null;
        this.returnAddress = inits.isInitialized("returnAddress") ? new com.my.relink.domain.user.QAddress(forProperty("returnAddress")) : null;
        this.user = inits.isInitialized("user") ? new com.my.relink.domain.user.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

