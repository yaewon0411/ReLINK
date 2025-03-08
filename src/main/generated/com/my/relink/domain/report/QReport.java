package com.my.relink.domain.report;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReport is a Querydsl query type for Report
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReport extends EntityPathBase<Report> {

    private static final long serialVersionUID = -452610288L;

    public static final QReport report = new QReport("report");

    public final com.my.relink.domain.QBaseEntity _super = new com.my.relink.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final NumberPath<Long> entityId = createNumber("entityId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedAt = _super.modifiedAt;

    public final EnumPath<ReportReason> reportReason = createEnum("reportReason", ReportReason.class);

    public final EnumPath<ReportType> reportType = createEnum("reportType", ReportType.class);

    public final NumberPath<Long> targetUserId = createNumber("targetUserId", Long.class);

    public QReport(String variable) {
        super(Report.class, forVariable(variable));
    }

    public QReport(Path<? extends Report> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReport(PathMetadata metadata) {
        super(Report.class, metadata);
    }

}

