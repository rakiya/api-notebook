package habanero.extensions.database.tables

import habanero.extensions.database.tables.records.SectionTagDetailRecord
import org.jooq.TableField
import org.jooq.impl.CustomTable
import org.jooq.impl.DSL.name
import org.jooq.impl.SQLDataType.*
import org.jooq.types.UInteger
import java.time.LocalDateTime


class SectionTagDetail : CustomTable<SectionTagDetailRecord>(name("SectionTagDetail")) {
    companion object {
        val SECTION_TAG_DETAIL = SectionTagDetail()
    }

    val SECTION_ID: TableField<SectionTagDetailRecord, UInteger> =
            createField(name("section_id"), INTEGERUNSIGNED)

    val TAG_ID: TableField<SectionTagDetailRecord, UInteger> =
            createField(name("tag_id"), INTEGERUNSIGNED)

    val NAME: TableField<SectionTagDetailRecord, String> =
            createField(name("name"), VARCHAR)

    val CREATED_AT: TableField<SectionTagDetailRecord, LocalDateTime> =
            createField(name("created_at"), LOCALDATETIME)

    val UPDATED_AT: TableField<SectionTagDetailRecord, LocalDateTime> =
            createField(name("updated_at"), LOCALDATETIME)


    /**
     * Subclasses must implement this method
     * <hr></hr>
     * {@inheritDoc}
     */
    override fun getRecordType(): Class<out SectionTagDetailRecord> {
        return SectionTagDetailRecord::class.java
    }

}