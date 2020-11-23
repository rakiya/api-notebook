package habanero.extensions.database.tables.records

import habanero.extensions.database.tables.SectionTagDetail
import org.jooq.impl.CustomRecord
import org.jooq.types.UInteger
import java.time.LocalDateTime

class SectionTagDetailRecord : CustomRecord<SectionTagDetailRecord>(SectionTagDetail.SECTION_TAG_DETAIL) {

    var sectionId: UInteger
        get() = get(0) as UInteger
        set(value) = set(0, value)

    var tagId: UInteger
        get() = get(1) as UInteger
        set(value) = set(1, value)

    var name: String
        get() = get(2) as String
        set(value) = set(1, value)

    var createdAt: LocalDateTime
        get() = get(3) as LocalDateTime
        set(value) = set(1, value)

}


