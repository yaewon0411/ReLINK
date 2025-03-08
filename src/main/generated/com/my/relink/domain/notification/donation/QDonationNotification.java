package com.my.relink.domain.notification.donation;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDonationNotification is a Querydsl query type for DonationNotification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDonationNotification extends EntityPathBase<DonationNotification> {

    private static final long serialVersionUID = 742468692L;

    public static final QDonationNotification donationNotification = new QDonationNotification("donationNotification");

    public final com.my.relink.domain.notification.QNotification _super = new com.my.relink.domain.notification.QNotification(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath donationItemName = createString("donationItemName");

    public final EnumPath<com.my.relink.domain.item.donation.DonationStatus> donationStatus = createEnum("donationStatus", com.my.relink.domain.item.donation.DonationStatus.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    //inherited
    public final NumberPath<Long> userId = _super.userId;

    public QDonationNotification(String variable) {
        super(DonationNotification.class, forVariable(variable));
    }

    public QDonationNotification(Path<? extends DonationNotification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDonationNotification(PathMetadata metadata) {
        super(DonationNotification.class, metadata);
    }

}

